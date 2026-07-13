<template>
  <view class="page detail-page image-result-page">
    <view v-if="errorText" class="herb-card error">
      <text class="empty-title">{{ errorText }}</text>
      <text class="empty-tip">{{ errorTip }}</text>
      <button class="primary-btn" :loading="loading" @click="loadResult">重新加载</button>
    </view>

    <template v-else>
      <view class="herb-card image-card">
        <text class="section-title">图片信息</text>
        <image v-if="displayImageUrl" class="preview-image" mode="aspectFill" :src="displayImageUrl" @error="displayImageUrl = ''" @click="previewImage" />
        <view v-else class="preview-placeholder">暂无图片</view>
        <view class="info-block">
          <text class="info-block-label">图片编码</text>
          <text class="info-block-value code-text">{{ displayText(result.imageCode) }}</text>
        </view>
        <view class="info-row">
          <text class="label">图片名称</text>
          <text class="value">{{ displayText(result.imageName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">图片角色</text>
          <text class="value">{{ formatStatus(result.imageRole, IMAGE_ROLE_MAP) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">采集地点</text>
          <text class="info-block-value">{{ displayText(result.collectPlace) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">采集时间</text>
          <text class="info-block-value">{{ formatDateTime(result.collectTime) }}</text>
        </view>
      </view>

      <view class="herb-card result-card">
        <text class="section-title">本地图谱匹配结果</text>
        <view class="final-result">
          <text class="final-name">{{ finalSpeciesName }}</text>
          <text class="confidence">{{ formatPercent(finalConfidence) }}</text>
        </view>
        <view class="info-row">
          <text class="label">结果来源</text>
          <text class="value">{{ formatStatus(result.resultSource || 'unknown', RESULT_SOURCE_MAP) }}</text>
        </view>
        <view class="info-row">
          <text class="label">匹配结果</text>
          <text class="value">{{ formatStatus(result.matchResult || 'unknown', MATCH_RESULT_MAP) }}</text>
        </view>
        <view class="info-row">
          <text class="label">是否复核</text>
          <text class="value">{{ formatNeedReview(result.needReview) }}</text>
        </view>
        <view class="info-row">
          <text class="label">复核状态</text>
          <text class="value">{{ formatStatus(result.reviewStatus || 'unknown', REVIEW_STATUS_MAP) }}</text>
        </view>
        <view class="info-block compact-block">
          <text class="info-block-label">识别时间</text>
          <text class="info-block-value">{{ formatDateTime(result.identifyTime) }}</text>
        </view>
        <view class="text-block">
          <text class="label block-label">识别建议</text>
          <text class="block-value">{{ result.suggestion || '暂无识别建议' }}</text>
        </view>
      </view>

      <view class="herb-card candidate-card">
        <view class="section-header">
          <text class="section-title">本地图谱候选</text>
          <text class="section-count">{{ localCandidates.length }} 项</text>
        </view>

        <view v-if="localCandidates.length > 0" class="candidate-list">
          <view v-for="item in localCandidates" :key="item.rank || item.atlasName || item.speciesName" class="candidate-item">
            <image v-if="getCandidateImageUrl(item)" class="atlas-image" mode="aspectFill" :src="getCandidateImageUrl(item)" />
            <view v-else class="atlas-placeholder">图谱</view>
            <view class="candidate-info">
              <view class="candidate-header">
                <text class="candidate-title">Top {{ item.rank || '-' }} {{ displayText(item.speciesName) }}</text>
                <text class="candidate-score">{{ formatPercent(item.similarity) }}</text>
              </view>
              <text class="candidate-line">标准图谱：{{ displayText(item.atlasName) }}</text>
            </view>
          </view>
        </view>

        <view v-else class="empty">
          <text class="empty-title">暂无本地图谱候选</text>
        </view>
      </view>

      <view class="herb-card doubao-card">
        <text class="section-title">大模型辅助识别</text>
        <view v-if="doubaoAssistMessage" class="assist-status">
          <text>{{ doubaoAssistMessage }}</text>
        </view>
        <template v-if="doubaoResult">
          <view class="info-row">
            <text class="label">大模型预测</text>
            <text class="value">{{ displayText(doubaoResult.speciesName) }}</text>
          </view>
          <view class="info-row">
            <text class="label">置信度</text>
            <text class="value">{{ formatPercent(doubaoResult.confidence) }}</text>
          </view>
          <view class="text-block">
            <text class="label block-label">理由</text>
            <text class="block-value">{{ displayText(doubaoResult.reason) }}</text>
          </view>
          <view class="text-block">
            <text class="label block-label">建议</text>
            <text class="block-value">{{ displayText(doubaoResult.suggestion) }}</text>
          </view>
        </template>
        <view v-else class="empty">
          <text class="empty-title">暂无大模型辅助识别记录</text>
        </view>
      </view>

      <button class="secondary-btn assistant-explain-btn" @click="handleExplainImage">AI 解释识别结果</button>
      <view class="action-bar">
        <button class="secondary-btn action-btn" :loading="loading" @click="loadResult">重新加载</button>
        <button class="primary-btn action-btn" @click="goBatchDetail">返回批次详情</button>
      </view>
    </template>
    <AssistantFloat ref="assistantRef" />
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { getLatestIdentification } from '../../api/mobileImageApi'
import {
  IMAGE_ROLE_MAP,
  MATCH_RESULT_MAP,
  RESULT_SOURCE_MAP,
  REVIEW_STATUS_MAP
} from '../../utils/constants'
import { formatDateTime, formatPercent, formatStatus, resolveFileUrl } from '../../utils/format'
import { getAuthHeader } from '../../utils/auth'

const imageId = ref('')
const batchId = ref('')
const result = ref({})
const localCandidates = ref([])
const doubaoResult = ref(null)
const displayImageUrl = ref('')
const candidateImageUrls = ref({})
const assistantRef = ref(null)
const loading = ref(false)
const errorText = ref('')
const errorTip = ref('请检查网络或后端服务是否启动')

const topLocalCandidate = computed(() => localCandidates.value[0] || null)
const finalSpeciesName = computed(() =>
  topLocalCandidate.value?.speciesName ||
  result.value.finalSpeciesName ||
  result.value.speciesName ||
  '暂未生成识别结果'
)
const finalConfidence = computed(() =>
  topLocalCandidate.value?.similarity ||
  result.value.localSimilarity ||
  result.value.finalConfidence ||
  result.value.confidence
)
const doubaoAssistMessage = computed(() => {
  if (result.value.matchResult !== 'low_confidence') {
    return doubaoResult.value ? '本次识别已使用大模型辅助判断。' : ''
  }

  return doubaoResult.value
    ? '本地图谱置信度较低，已自动调用大模型辅助识别。'
    : '本地图谱置信度较低，但暂未获得大模型辅助识别结果。'
})

onLoad((options) => {
  imageId.value = options.imageId || options.id || ''
  batchId.value = options.batchId || ''

  if (!imageId.value) {
    errorText.value = '图片 ID 不存在'
    errorTip.value = '请从批次详情或上传结果重新进入'
    return
  }

  loadResult()
})

onPullDownRefresh(async () => {
  try {
    await loadResult()
  } finally {
    uni.stopPullDownRefresh()
  }
})

onUnmounted(() => {
  revokePreviewUrls()
})

async function loadResult() {
  if (!imageId.value) {
    errorText.value = '图片 ID 不存在'
    errorTip.value = '请从批次详情或上传结果重新进入'
    return
  }

  if (loading.value) {
    return
  }

  loading.value = true
  errorText.value = ''
  errorTip.value = '请检查网络或后端服务是否启动'

  try {
    const data = await getLatestIdentification(imageId.value)
    result.value = normalizeResult(data)
    localCandidates.value = normalizeCandidates(data)
    doubaoResult.value = normalizeDoubao(data)
    await preparePreviewImages()
  } catch (error) {
    console.error('识别结果加载失败', error)
    errorText.value = '识别结果加载失败'
    errorTip.value = '请检查网络或后端服务是否启动'
    uni.showToast({
      title: '识别结果加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

async function preparePreviewImages() {
  revokePreviewUrls()
  displayImageUrl.value = await loadDisplayImageUrl(result.value.imageUrl)

  const entries = await Promise.all(
    localCandidates.value.map(async (item) => {
      const key = getCandidateKey(item)
      const url = item?.atlasImageUrl

      if (!key || !url) {
        return null
      }

      const previewUrl = await loadDisplayImageUrl(url)
      return previewUrl ? [key, previewUrl] : null
    })
  )

  candidateImageUrls.value = entries
    .filter(Boolean)
    .reduce((resultMap, [key, value]) => {
      resultMap[key] = value
      return resultMap
    }, {})
}

function revokePreviewUrls() {
  const urls = [displayImageUrl.value, ...Object.values(candidateImageUrls.value)]
  urls.forEach((url) => {
    if (typeof url === 'string' && url.startsWith('blob:') && typeof URL !== 'undefined') {
      URL.revokeObjectURL(url)
    }
  })
  displayImageUrl.value = ''
  candidateImageUrls.value = {}
}

async function loadDisplayImageUrl(url) {
  if (!url) {
    return ''
  }

  if (!isPrivateFileUrl(url)) {
    return resolveImageUrl(url)
  }

  if (typeof fetch !== 'function' || typeof URL === 'undefined') {
    return ''
  }

  try {
    const response = await fetch(resolveImageUrl(url), {
      headers: {
        Authorization: getAuthHeader()
      }
    })

    if (!response.ok) {
      return ''
    }

    const blob = await response.blob()
    return URL.createObjectURL(blob)
  } catch (error) {
    console.error('load private image failed:', error)
    return ''
  }
}

function isPrivateFileUrl(url) {
  return typeof url === 'string' && /\/api\/files\//.test(url)
}

function getCandidateKey(item) {
  return String(item?.rank || item?.atlasId || item?.atlasName || item?.speciesName || item?.atlasImageUrl || '')
}

function getCandidateImageUrl(item) {
  return candidateImageUrls.value[getCandidateKey(item)] || ''
}

function normalizeResult(data) {
  const source = data || {}
  return source.result || source.detail || source
}

function normalizeCandidates(data) {
  const source = data || {}
  const detail = normalizeResult(data)
  const candidates =
    source.localCandidates ||
    source.matches ||
    source.matchCandidates ||
    detail.localCandidates ||
    detail.matches ||
    detail.matchCandidates ||
    []

  return Array.isArray(candidates) ? candidates : []
}

function normalizeDoubao(data) {
  const source = data || {}
  const detail = normalizeResult(data)
  const doubao =
    source.doubaoRecognition ||
    source.recognition ||
    source.doubaoResult ||
    detail.doubaoRecognition ||
    detail.recognition ||
    detail.doubaoResult ||
    null
  if (!doubao) {
    return null
  }
  if (doubao.recognitionSource && doubao.recognitionSource !== 'doubao_auxiliary') {
    return null
  }

  const rawResult = parseJson(doubao.rawResult)
  const rawPayload = rawResult?.data && typeof rawResult.data === 'object' ? rawResult.data : rawResult
  const firstResult = Array.isArray(rawPayload?.results) ? rawPayload.results[0] : null
  return {
    ...doubao,
    speciesName: doubao.speciesName || doubao.predictedName || doubao.predictedSpeciesName || firstResult?.speciesName,
    confidence: doubao.confidence || firstResult?.confidence,
    reason: doubao.reason || firstResult?.reason || rawPayload?.reason,
    suggestion: doubao.suggestion || firstResult?.suggestion || rawPayload?.suggestion
  }
}

function parseJson(value) {
  if (!value || typeof value !== 'string') {
    return null
  }

  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
}

function resolveImageUrl(url) {
  return resolveFileUrl(url)
}

function previewImage() {
  if (!displayImageUrl.value) {
    return
  }

  uni.previewImage({
    urls: [displayImageUrl.value]
  })
}

function handleExplainImage() {
  if (!imageId.value) {
    return
  }
  assistantRef.value?.explainImage(imageId.value)
}

function formatNeedReview(value) {
  if (value === true || value === 1 || value === '1' || value === 'true') {
    return '需要复核'
  }

  if (value === false || value === 0 || value === '0' || value === 'false') {
    return '无需复核'
  }

  return '-'
}

function goBatchDetail() {
  const pages = getCurrentPages()
  const previousPage = pages[pages.length - 2]
  const previousRoute = normalizeRoute(previousPage?.route)

  if (previousRoute === 'pages/batch/detail') {
    uni.navigateBack()
    return
  }

  const batchDetailIndex = findPageIndex('pages/batch/detail')
  if (batchDetailIndex >= 0) {
    uni.navigateBack({
      delta: pages.length - 1 - batchDetailIndex
    })
    return
  }

  if (batchId.value) {
    uni.redirectTo({
      url: `/pages/batch/detail?batchId=${batchId.value}`
    })
    return
  }

  if (pages.length > 1) {
    uni.navigateBack()
  }
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function findPageIndex(targetRoute) {
  const pages = getCurrentPages()
  for (let index = pages.length - 2; index >= 0; index--) {
    if (normalizeRoute(pages[index]?.route) === targetRoute) {
      return index
    }
  }
  return -1
}

function normalizeRoute(route) {
  return route ? route.replace(/^\//, '') : ''
}
</script>

<style scoped>
.detail-page {
  padding-top: 32rpx;
}

.section-title {
  display: block;
  margin-bottom: 20rpx;
  color: #1f2933;
  font-size: 32rpx;
  font-weight: 700;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.section-count {
  color: #64748b;
  font-size: 26rpx;
}

.preview-image,
.preview-placeholder {
  width: 100%;
  height: 420rpx;
  margin-bottom: 20rpx;
  border-radius: 16rpx;
  background: #f4eadf;
}

.preview-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 28rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22rpx;
  padding: 10rpx 0;
}

.label {
  flex-shrink: 0;
  width: 150rpx;
  color: #64748b;
}

.value {
  min-width: 0;
  flex: 1;
  color: #1f2933;
  text-align: right;
  word-break: break-all;
}

.info-block {
  margin-bottom: 22rpx;
}

.info-block-label,
.info-block-value {
  display: block;
  text-align: left;
}

.info-block-label {
  margin-bottom: 8rpx;
  color: #7c6f5c;
  font-size: 26rpx;
}

.info-block-value {
  color: #1f2933;
  font-size: 28rpx;
  line-height: 1.6;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.code-text {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 25rpx;
  line-height: 1.45;
  overflow-wrap: normal;
  word-break: break-all;
}

.compact-block {
  margin-bottom: 12rpx;
}

.final-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #eaf5ee;
}

.final-name {
  min-width: 0;
  color: #1f2933;
  font-size: 38rpx;
  font-weight: 700;
  word-break: break-all;
}

.confidence {
  flex-shrink: 0;
  color: #166534;
  font-size: 34rpx;
  font-weight: 700;
}

.text-block {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #eef2f7;
}

.assist-status {
  margin-bottom: 18rpx;
  padding: 18rpx 20rpx;
  border-left: 6rpx solid #166534;
  border-radius: 8rpx;
  background: #eaf5ee;
  color: #0f5132;
  font-size: 26rpx;
  line-height: 1.5;
}

.block-label,
.block-value {
  display: block;
}

.block-value {
  margin-top: 10rpx;
  color: #5f5548;
  line-height: 1.55;
}

.candidate-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.candidate-item {
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #fffaf2;
}

.atlas-image,
.atlas-placeholder {
  flex-shrink: 0;
  width: 150rpx;
  height: 150rpx;
  border-radius: 16rpx;
  background: #f0e8dc;
}

.atlas-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 24rpx;
}

.candidate-info {
  min-width: 0;
  flex: 1;
}

.candidate-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.candidate-title {
  min-width: 0;
  color: #1f2933;
  font-size: 28rpx;
  font-weight: 700;
  word-break: break-all;
}

.candidate-score {
  flex-shrink: 0;
  color: #166534;
  font-size: 27rpx;
  font-weight: 700;
}

.candidate-line {
  display: block;
  margin-top: 12rpx;
  color: #64748b;
  font-size: 25rpx;
  line-height: 1.4;
}

.action-bar {
  display: flex;
  gap: 18rpx;
  padding-bottom: 28rpx;
}

.assistant-explain-btn {
  width: 100%;
  margin-bottom: 18rpx;
}

.action-btn {
  flex: 1;
}

.empty,
.error {
  margin-top: 24rpx;
}

.empty-title {
  display: block;
  color: #1f2933;
  font-size: 30rpx;
  font-weight: 700;
}

.empty-tip {
  display: block;
  margin-top: 14rpx;
  color: #64748b;
  font-size: 26rpx;
  line-height: 1.5;
}

.section-title,
.final-name,
.candidate-title,
.empty-title {
  color: #0f3d2e;
}

.section-count,
.empty-tip,
.candidate-line {
  color: #7c6f5c;
}

.preview-image,
.preview-placeholder {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
}

.preview-placeholder {
  color: #8b7e6b;
}

.label {
  color: #7c6f5c;
}

.value {
  color: #1f2933;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.final-result {
  border: 1rpx solid #b7d7c2;
  background: #eaf5ee;
}

.confidence,
.candidate-score {
  color: #166534;
}

.text-block {
  border-top-color: #eadfcd;
}

.assist-status {
  border-left-color: #166534;
  background: #fffaf2;
  color: #0f5132;
}

.block-value {
  color: #5f5548;
}

.candidate-item {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
}

.atlas-image,
.atlas-placeholder {
  background: #f0e8dc;
}

.image-result-page {
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
}

.herb-card {
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 24rpx;
  background: #fffaf2;
  background: rgba(255, 250, 242, 0.96);
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.section-title {
  display: flex;
  align-items: center;
  color: #0f3d2e;
}

.section-title::before {
  width: 8rpx;
  height: 32rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #166534;
  content: '';
}

.section-header .section-title {
  margin-bottom: 0;
}

.image-card .info-row,
.result-card .info-row,
.doubao-card .info-row {
  align-items: center;
  margin-bottom: 16rpx;
  padding: 0;
}

.image-card .label,
.result-card .label,
.doubao-card .label {
  width: auto;
  color: #7c6f5c;
}

.image-card .value,
.result-card .value,
.doubao-card .value {
  font-weight: 500;
}

.candidate-item {
  border-radius: 18rpx;
}

.assistant-explain-btn,
.action-btn {
  height: 78rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  line-height: 78rpx;
}
</style>
