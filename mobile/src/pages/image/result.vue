<template>
  <view class="page">
    <view v-if="errorText" class="card error">
      <text class="empty-title">{{ errorText }}</text>
      <text class="empty-tip">{{ errorTip }}</text>
      <button class="primary-btn" :loading="loading" @click="loadResult">重新加载</button>
    </view>

    <template v-else>
      <view class="card image-card">
        <text class="section-title">图片信息</text>
        <image v-if="resolvedImageUrl" class="preview-image" mode="aspectFill" :src="resolvedImageUrl" @click="previewImage" />
        <view v-else class="preview-placeholder">暂无图片</view>
        <view class="info-row">
          <text class="label">图片编码</text>
          <text class="value">{{ displayText(result.imageCode) }}</text>
        </view>
        <view class="info-row">
          <text class="label">图片名称</text>
          <text class="value">{{ displayText(result.imageName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">图片角色</text>
          <text class="value">{{ formatStatus(result.imageRole, IMAGE_ROLE_MAP) }}</text>
        </view>
        <view class="info-row">
          <text class="label">采集地点</text>
          <text class="value">{{ displayText(result.collectPlace) }}</text>
        </view>
        <view class="info-row">
          <text class="label">采集时间</text>
          <text class="value">{{ formatDateTime(result.collectTime) }}</text>
        </view>
      </view>

      <view class="card result-card">
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
        <view class="info-row">
          <text class="label">识别时间</text>
          <text class="value">{{ formatDateTime(result.identifyTime) }}</text>
        </view>
        <view class="text-block">
          <text class="label block-label">识别建议</text>
          <text class="block-value">{{ result.suggestion || '暂无识别建议' }}</text>
        </view>
      </view>

      <view class="card">
        <view class="section-header">
          <text class="section-title">本地图谱候选</text>
          <text class="section-count">{{ localCandidates.length }} 项</text>
        </view>

        <view v-if="localCandidates.length > 0" class="candidate-list">
          <view v-for="item in localCandidates" :key="item.rank || item.atlasName || item.speciesName" class="candidate-item">
            <image v-if="resolveImageUrl(item.atlasImageUrl)" class="atlas-image" mode="aspectFill" :src="resolveImageUrl(item.atlasImageUrl)" />
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

      <view class="card doubao-card">
        <text class="section-title">豆包辅助识别</text>
        <template v-if="doubaoResult">
          <view class="info-row">
            <text class="label">豆包预测</text>
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
          <text class="empty-title">暂无豆包辅助识别记录</text>
        </view>
      </view>

      <view class="action-bar">
        <button class="secondary-btn action-btn" :loading="loading" @click="loadResult">重新加载</button>
        <button class="primary-btn action-btn" @click="goBatchDetail">返回批次详情</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { getLatestIdentification } from '../../api/mobileImageApi'
import {
  IMAGE_ROLE_MAP,
  MATCH_RESULT_MAP,
  RESULT_SOURCE_MAP,
  REVIEW_STATUS_MAP
} from '../../utils/constants'
import { formatDateTime, formatPercent, formatStatus, resolveFileUrl } from '../../utils/format'

const imageId = ref('')
const batchId = ref('')
const result = ref({})
const localCandidates = ref([])
const doubaoResult = ref(null)
const loading = ref(false)
const errorText = ref('')
const errorTip = ref('请检查网络或后端服务是否启动')

const resolvedImageUrl = computed(() => resolveImageUrl(result.value.imageUrl))
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

  const rawResult = parseJson(doubao.rawResult)
  const firstResult = Array.isArray(rawResult?.results) ? rawResult.results[0] : null
  return {
    ...doubao,
    speciesName: doubao.speciesName || doubao.predictedName || doubao.predictedSpeciesName || firstResult?.speciesName,
    confidence: doubao.confidence || firstResult?.confidence,
    reason: doubao.reason || firstResult?.reason,
    suggestion: doubao.suggestion || rawResult?.suggestion
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
  if (!resolvedImageUrl.value) {
    return
  }

  uni.previewImage({
    urls: [resolvedImageUrl.value]
  })
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
.section-title {
  display: block;
  margin-bottom: 20rpx;
  color: #111827;
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
  background: #f1f5f9;
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
  color: #111827;
  text-align: right;
  word-break: break-all;
}

.final-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #eef5ff;
}

.final-name {
  min-width: 0;
  color: #111827;
  font-size: 38rpx;
  font-weight: 700;
  word-break: break-all;
}

.confidence {
  flex-shrink: 0;
  color: #1677ff;
  font-size: 34rpx;
  font-weight: 700;
}

.text-block {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #eef2f7;
}

.block-label,
.block-value {
  display: block;
}

.block-value {
  margin-top: 10rpx;
  color: #475569;
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
  background: #f8fafc;
}

.atlas-image,
.atlas-placeholder {
  flex-shrink: 0;
  width: 132rpx;
  height: 132rpx;
  border-radius: 12rpx;
  background: #e2e8f0;
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
  color: #111827;
  font-size: 28rpx;
  font-weight: 700;
  word-break: break-all;
}

.candidate-score {
  flex-shrink: 0;
  color: #1677ff;
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

.action-btn {
  flex: 1;
}

.empty,
.error {
  margin-top: 24rpx;
}

.empty-title {
  display: block;
  color: #111827;
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
</style>
