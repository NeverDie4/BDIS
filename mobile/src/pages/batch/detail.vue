<template>
  <view class="page detail-page">
    <view v-if="errorText" class="card error">
      <text class="empty-title">{{ errorText }}</text>
      <text class="empty-tip">{{ errorTip }}</text>
      <button class="primary-btn" :loading="loading" @click="loadDetail">重新加载</button>
    </view>

    <template v-else>
      <view class="herb-card batch-overview-card">
        <view class="batch-header">
          <text class="batch-title">{{ displayText(detail.batchName) }}</text>
          <text class="status-tag" :class="`status-${detail.batchStatus || 'unknown'}`">
            {{ formatStatus(detail.batchStatus, BATCH_STATUS_MAP) }}
          </text>
        </view>

        <view class="info-block">
          <text class="info-block-label">批次编码</text>
          <text class="info-block-value code-text">{{ displayText(detail.batchCode) }}</text>
        </view>
        <view class="info-row">
          <text class="label">药材名称</text>
          <text class="value">{{ displayText(detail.speciesName) }}</text>
        </view>
      </view>

      <view class="herb-card collection-info-card">
        <text class="section-title">采集信息</text>
        <view class="info-block">
          <text class="info-block-label">产地</text>
          <text class="info-block-value">{{ displayText(detail.originPlace) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">基地名称</text>
          <text class="info-block-value">{{ displayText(detail.baseName) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">采集时间</text>
          <text class="info-block-value">{{ formatCollectTime(detail) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">创建时间</text>
          <text class="info-block-value">{{ formatDateTime(detail.createTime) }}</text>
        </view>
        <view class="text-block">
          <text class="label block-label">备注</text>
          <text class="block-value">{{ displayText(detail.remark) }}</text>
        </view>
      </view>

      <view class="herb-card stat-card">
        <text class="section-title">识别统计</text>
        <view class="stat-grid">
          <view class="stat-item">
            <text class="stat-value">{{ displayNumber(detail.imageCount) }}</text>
            <text class="stat-label">图片</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ displayNumber(detail.identifiedCount) }}</text>
            <text class="stat-label">已识别</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ displayNumber(detail.reviewedCount) }}</text>
            <text class="stat-label">已复核</text>
          </view>
          <view class="stat-item warning">
            <text class="stat-value">{{ displayNumber(detail.needReviewCount) }}</text>
            <text class="stat-label">待复核</text>
          </view>
        </view>
        <text v-if="Number(detail.needReviewCount || 0) > 0" class="notice">存在待复核图片，请等待管理员复核</text>
      </view>

      <view class="herb-card evaluation-card">
        <text class="section-title">批次评价</text>
        <view class="info-row">
          <text class="label">最终药材</text>
          <text class="value">{{ detail.finalSpeciesName || '暂未生成' }}</text>
        </view>
        <view class="info-row">
          <text class="label">平均相似度</text>
          <text class="value">{{ formatPercent(detail.avgSimilarity) }}</text>
        </view>
        <view class="info-row">
          <text class="label">质量等级</text>
          <text class="value">{{ formatStatus(detail.qualityLevel, QUALITY_LEVEL_MAP) }}</text>
        </view>
        <view class="info-row">
          <text class="label">质量分数</text>
          <text class="value">{{ formatScore(detail.qualityScore) }}</text>
        </view>
        <view class="text-block evaluation-summary">
          <text class="label block-label">评价摘要</text>
          <text class="block-value">{{ detail.evaluationSummary || '暂无评价摘要，请先上传图片并刷新汇总' }}</text>
        </view>
      </view>

      <view class="herb-card action-card">
        <text class="section-title">批次操作</text>
        <view class="action-grid">
          <button v-if="canUpload" class="primary-btn action-btn" @click="goUpload">上传图片</button>
          <button v-else-if="showUploadDisabled" class="secondary-btn action-btn" @click="showUploadDisabledTip">上传图片</button>
          <button
            v-if="canIdentifyBatch()"
            class="secondary-btn action-btn"
            :loading="identifyingMissing"
            @click="handleIdentifyMissingImages"
          >
            识别未完成图片
          </button>
          <button v-if="canRefreshSummary" class="secondary-btn action-btn" :loading="refreshing" @click="handleRefreshSummary">刷新汇总</button>
          <button class="secondary-btn action-btn" @click="handleExplainBatch">AI 解释批次</button>
          <button v-if="canSubmit" class="secondary-btn action-btn" :loading="submitting" @click="handleSubmitBatch">提交批次</button>
        </view>
      </view>

      <view class="herb-card image-section-card">
        <view class="section-header">
          <text class="section-title">图片列表</text>
          <text class="section-count">{{ images.length }} 张</text>
        </view>

        <view v-if="images.length > 0" class="image-list">
          <view v-for="item in images" :key="item.batchImageId || item.imageId || item.id" class="image-card">
            <image
              v-if="getDisplayImageUrl(item)"
              class="thumb"
              mode="aspectFill"
              :src="getDisplayImageUrl(item)"
              @error="handleThumbError(item)"
            />
            <view v-else class="thumb placeholder">暂无图片</view>
            <view class="image-info">
              <view class="image-header">
                <text class="image-title">{{ formatStatus(item.imageRole, IMAGE_ROLE_MAP) }}图片</text>
                <text v-if="item.isPrimary" class="primary-tag">主图</text>
              </view>
              <text class="image-line image-code code-text">图片编码：{{ displayText(item.imageCode) }}</text>
              <text class="image-line result-highlight">识别结果：{{ displayText(item.finalSpeciesName) }}</text>
              <text class="image-line confidence-line">置信度：{{ formatPercent(item.finalConfidence) }}</text>
              <text class="image-line">复核状态：{{ formatStatus(item.reviewStatus || 'unknown', REVIEW_STATUS_MAP) }}</text>
              <text class="image-line secondary-meta">来源：{{ formatStatus(item.resultSource || 'unknown', RESULT_SOURCE_MAP) }}</text>
              <text class="image-line secondary-meta">识别时间：{{ formatDateTime(item.identifyTime) }}</text>

              <view class="image-actions">
                <button class="mini-btn secondary-mini" @click="goImageResult(item)">查看结果</button>
                <button
                  v-if="canIdentifyBatch()"
                  class="mini-btn primary-mini"
                  :loading="identifyingImageId === getImageId(item)"
                  :disabled="identifyingImageId === getImageId(item)"
                  @click="handleIdentifyImage(item)"
                >
                  {{ hasIdentification(item) ? '重新识别' : '识别' }}
                </button>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="empty">
          <text class="empty-title">暂无采集图片</text>
          <text class="empty-tip">点击“上传图片”开始采集</text>
        </view>
      </view>
    </template>
    <AssistantFloat ref="assistantRef" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import {
  getBatchDetail,
  identifyBatchImage,
  identifyMissingImages,
  refreshBatchSummary,
  submitBatch
} from '../../api/mobileBatchApi'
import {
  BATCH_STATUS_MAP,
  IMAGE_ROLE_MAP,
  QUALITY_LEVEL_MAP,
  RESULT_SOURCE_MAP,
  REVIEW_STATUS_MAP
} from '../../utils/constants'
import { formatDateTime, formatPercent, formatScore, formatStatus, resolveFileUrl } from '../../utils/format'
import { getAuthHeader } from '../../utils/auth'

const batchId = ref('')
const detail = ref({})
const images = ref([])
const imagePreviewUrls = ref({})
const failedThumbKeys = ref({})
const loading = ref(false)
const refreshing = ref(false)
const submitting = ref(false)
const identifyingImageId = ref(null)
const identifyingMissing = ref(false)
const assistantRef = ref(null)
const errorText = ref('')
const errorTip = ref('请检查网络或后端服务是否启动')

const canUpload = computed(() => ['draft', 'collecting'].includes(detail.value.batchStatus))
const showUploadDisabled = computed(() =>
  ['submitted', 'identifying', 'reviewing', 'confirmed'].includes(detail.value.batchStatus)
)
const canRefreshSummary = computed(() => !['archived', 'cancelled'].includes(detail.value.batchStatus))
const canSubmit = computed(() => ['draft', 'collecting'].includes(detail.value.batchStatus))
const hasMissingImages = computed(() => {
  const imageCount = Number(detail.value.imageCount || images.value.length || 0)
  const identifiedCount = Number(detail.value.identifiedCount || 0)
  return imageCount > identifiedCount
})

onLoad((options) => {
  batchId.value = options.batchId || options.id || ''

  if (!batchId.value) {
    errorText.value = '批次 ID 不存在'
    errorTip.value = '请从任务详情或批次列表重新进入'
  }
})

onShow(() => {
  if (batchId.value) {
    loadDetail()
  }
})

onPullDownRefresh(async () => {
  try {
    await loadDetail()
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function loadDetail() {
  if (!batchId.value) {
    errorText.value = '批次 ID 不存在'
    errorTip.value = '请从任务详情或批次列表重新进入'
    return
  }

  if (loading.value) {
    return
  }

  loading.value = true
  errorText.value = ''
  errorTip.value = '请检查网络或后端服务是否启动'

  try {
    const data = await getBatchDetail(batchId.value)
    detail.value = normalizeBatchDetail(data)
    images.value = normalizeImages(data)
    await prepareImagePreviewUrls(images.value)
  } catch (error) {
    console.error('批次加载失败', error)
    errorText.value = '批次加载失败'
    errorTip.value = '请检查网络或后端服务是否启动'
    uni.showToast({
      title: '批次加载失败，请检查网络或后端服务',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function normalizeBatchDetail(data) {
  const source = data || {}
  return source.batch || source.detail || source
}

function normalizeImages(data) {
  const source = data || {}
  const batch = normalizeBatchDetail(data)
  const records =
    source.images ||
    source.imageList ||
    source.records ||
    batch.images ||
    batch.imageList ||
    batch.records ||
    []

  return Array.isArray(records) ? records : []
}

async function prepareImagePreviewUrls(records) {
  revokeImagePreviewUrls()

  const entries = await Promise.all(
    records.map(async (item) => {
      const key = getImagePreviewKey(item)
      const url = item?.imageUrl

      if (!key || !url) {
        return null
      }

      if (!isPrivateFileUrl(url)) {
        return [key, resolveFileUrl(url)]
      }

      const previewUrl = await loadPrivateImageUrl(url)
      return previewUrl ? [key, previewUrl] : null
    })
  )

  imagePreviewUrls.value = entries
    .filter(Boolean)
    .reduce((result, [key, value]) => {
      result[key] = value
      return result
    }, {})
}

function revokeImagePreviewUrls() {
  Object.values(imagePreviewUrls.value).forEach((url) => {
    if (typeof url === 'string' && url.startsWith('blob:') && typeof URL !== 'undefined') {
      URL.revokeObjectURL(url)
    }
  })
  imagePreviewUrls.value = {}
}

async function loadPrivateImageUrl(url) {
  if (typeof fetch !== 'function' || typeof URL === 'undefined') {
    return ''
  }

  try {
    const response = await fetch(resolveFileUrl(url), {
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

function getImagePreviewKey(item) {
  return String(item?.batchImageId || item?.imageId || item?.id || '')
}

async function handleRefreshSummary() {
  if (!batchId.value || refreshing.value) {
    return
  }

  if (!canRefreshSummary.value) {
    showToast('当前批次状态不允许刷新汇总')
    return
  }

  refreshing.value = true

  try {
    await refreshBatchSummary(batchId.value)
    uni.showToast({
      title: '汇总刷新成功',
      icon: 'success'
    })
    await loadDetail()
  } catch (error) {
    console.error('汇总刷新失败', error)
    showToast('汇总刷新失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

function handleSubmitBatch() {
  if (!batchId.value || submitting.value) {
    return
  }

  if (!canSubmit.value) {
    showToast('当前批次状态不允许提交')
    return
  }

  if (Number(detail.value.imageCount || 0) <= 0) {
    showToast('请至少上传一张图片后再提交批次')
    return
  }

  uni.showModal({
    title: '确认提交',
    content: '提交后将进入后续识别和复核流程，确定提交该批次吗？',
    success: async (res) => {
      if (!res.confirm) {
        return
      }

      await submitCurrentBatch()
    }
  })
}

async function submitCurrentBatch() {
  submitting.value = true

  try {
    await submitBatch(batchId.value, {
      remark: '手机端采集完成，提交批次'
    })
    uni.showToast({
      title: '批次提交成功',
      icon: 'success'
    })
    await loadDetail()
  } catch (error) {
    console.error('批次提交失败', error)
    showToast('批次提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleIdentifyImage(item) {
  if (!batchId.value) {
    showToast('批次ID为空，无法触发识别')
    return
  }

  const imageId = getImageId(item)
  if (!imageId) {
    showToast('图片ID为空，无法触发识别')
    return
  }

  if (!canIdentifyBatch()) {
    showToast('当前批次状态不允许触发识别')
    return
  }

  if (identifyingImageId.value) {
    return
  }

  if (hasIdentification(item)) {
    uni.showModal({
      title: '重新识别',
      content: '该图片已有识别结果，是否重新识别？',
      success: async (res) => {
        if (res.confirm) {
          await identifyCurrentImage(imageId)
        }
      }
    })
    return
  }

  identifyCurrentImage(imageId)
}

async function identifyCurrentImage(imageId) {
  identifyingImageId.value = imageId

  try {
    await identifyBatchImage(batchId.value, imageId, {
      forceRefresh: true
    })
    uni.showToast({
      title: '识别完成',
      icon: 'success'
    })
    await refreshSummaryAfterIdentify()
    await loadDetail()
  } catch (error) {
    console.error('identify image failed:', error)
    showToast('识别失败，请稍后重试')
  } finally {
    identifyingImageId.value = null
  }
}

function handleIdentifyMissingImages() {
  if (!batchId.value) {
    showToast('批次ID为空，无法触发识别')
    return
  }

  if (!canIdentifyBatch()) {
    showToast('当前批次状态不允许触发识别')
    return
  }

  if (identifyingMissing.value) {
    return
  }

  const imageCount = Number(detail.value.imageCount || images.value.length || 0)
  if (imageCount <= 0) {
    showToast('请先上传图片')
    return
  }

  if (!hasMissingImages.value) {
    showToast('当前批次暂无未识别图片')
    return
  }

  uni.showModal({
    title: '批量识别',
    content: '将对当前批次下未识别图片执行识别，识别可能需要一定时间，是否继续？',
    success: async (res) => {
      if (res.confirm) {
        await identifyMissingImagesInBatch()
      }
    }
  })
}

async function identifyMissingImagesInBatch() {
  identifyingMissing.value = true

  try {
    const result = await identifyMissingImages(batchId.value)
    showIdentifyBatchResult(result)
    await refreshSummaryAfterIdentify()
    await loadDetail()
  } catch (error) {
    console.error('identify missing images failed:', error)
    showToast('批量识别失败，请稍后重试')
  } finally {
    identifyingMissing.value = false
  }
}

async function refreshSummaryAfterIdentify() {
  try {
    await refreshBatchSummary(batchId.value)
  } catch (error) {
    console.error('refresh summary after identify failed:', error)
    showToast('识别已完成，但批次汇总刷新失败，请手动点击刷新汇总')
  }
}

function showIdentifyBatchResult(result = {}) {
  const totalCount = result.totalCount ?? result.total ?? result.count ?? 0
  const successCount = result.successCount ?? result.success ?? 0
  const failCount = result.failCount ?? result.failedCount ?? result.fail ?? 0
  const failTip = Number(failCount || 0) > 0 ? '\n部分图片识别失败，请稍后重试或联系管理员' : ''

  uni.showModal({
    title: '批量识别完成',
    content: `共处理 ${totalCount} 张，成功 ${successCount} 张，失败 ${failCount} 张${failTip}`,
    showCancel: false
  })
}

function canIdentifyBatch() {
  const status = detail.value?.batchStatus
  return ['draft', 'collecting', 'submitted', 'identifying', 'reviewing'].includes(status)
}

function hasIdentification(item) {
  return !!(item.identificationResultId || item.finalSpeciesName || item.finalConfidence)
}

function getImageId(item) {
  return item.imageId || item.id || ''
}

function goUpload() {
  if (!batchId.value) {
    showToast('批次ID为空，无法上传图片')
    return
  }

  if (!canUpload.value) {
    showUploadDisabledTip()
    return
  }

  uni.navigateTo({
    url: `/pages/image/upload?batchId=${batchId.value}`
  })
}

function goImageResult(item) {
  const imageId = getImageId(item)

  if (!imageId) {
    showToast('图片ID为空，无法查看识别结果')
    return
  }

  uni.navigateTo({
    url: `/pages/image/result?imageId=${imageId}&batchId=${batchId.value}`
  })
}

function showUploadDisabledTip() {
  showToast('当前批次状态不允许继续上传图片')
}

function handleExplainBatch() {
  if (!batchId.value) {
    return
  }
  assistantRef.value?.explainBatch(batchId.value)
}

function resolveImageUrl(url) {
  return resolveFileUrl(url)
}

function getDisplayImageUrl(item) {
  if (
    typeof failedThumbKeys !== 'undefined' &&
    failedThumbKeys.value[getImagePreviewKey(item)]
  ) {
    return ''
  }

  const previewUrl = imagePreviewUrls.value[getImagePreviewKey(item)]
  if (previewUrl) {
    return previewUrl
  }

  return isPrivateFileUrl(item?.imageUrl) ? '' : resolveImageUrl(item?.imageUrl)
}

function handleThumbError(item) {
  failedThumbKeys.value = {
    ...failedThumbKeys.value,
    [getImagePreviewKey(item)]: true
  }
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function displayNumber(value) {
  return value === undefined || value === null || value === '' ? 0 : value
}

function formatCollectTime(item) {
  const startTime = formatDateTime(item.collectStartTime)
  const endTime = formatDateTime(item.collectEndTime)

  if (startTime === '-' && endTime === '-') {
    return '-'
  }

  return `${startTime} ~ ${endTime}`
}

function showToast(title) {
  uni.showToast({
    title,
    icon: 'none'
  })
}
</script>

<style scoped>
.detail-page {
  padding-top: 32rpx;
}

.batch-header,
.section-header,
.image-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.batch-header {
  margin-bottom: 20rpx;
}

.section-header {
  margin-bottom: 20rpx;
}

.batch-title {
  min-width: 0;
  color: #1f2933;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-all;
}

.section-title {
  display: block;
  margin-bottom: 20rpx;
  color: #1f2933;
  font-size: 32rpx;
  font-weight: 700;
}

.section-count {
  color: #64748b;
  font-size: 26rpx;
}

.status-tag,
.primary-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #eaf5ee;
  color: #166534;
  font-size: 23rpx;
  line-height: 1;
}

.primary-tag {
  background: #fff7e6;
  color: #c46a00;
}

.status-collecting,
.status-draft {
  background: #edf7ed;
  color: #1f8a3b;
}

.status-submitted,
.status-identifying,
.status-reviewing {
  background: #fff7e6;
  color: #c46a00;
}

.status-confirmed,
.status-archived {
  background: #eaf5ee;
  color: #166534;
}

.status-cancelled {
  background: #f4eadf;
  color: #64748b;
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
  color: #5f5548;
  line-height: 1.55;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
}

.stat-item {
  padding: 18rpx 10rpx;
  border-radius: 14rpx;
  background: #fffaf2;
  text-align: center;
}

.stat-item.warning {
  background: #fff7e6;
}

.stat-value {
  display: block;
  color: #1f2933;
  font-size: 34rpx;
  font-weight: 700;
}

.stat-label {
  display: block;
  margin-top: 6rpx;
  color: #64748b;
  font-size: 23rpx;
}

.notice {
  display: block;
  margin-top: 18rpx;
  color: #c46a00;
  font-size: 25rpx;
  line-height: 1.5;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.action-btn {
  height: 80rpx;
  margin-top: 0;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 500;
  line-height: 80rpx;
}

.action-btn[disabled] {
  border-color: #ddd5ca !important;
  background: #eee9e1 !important;
  color: #8b7e6b !important;
  opacity: 0.78;
}

.image-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.image-card {
  display: flex;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: #fffaf2;
}

.thumb {
  flex-shrink: 0;
  width: 150rpx;
  height: 150rpx;
  border-radius: 16rpx;
  background: #f0e8dc;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 24rpx;
  text-align: center;
}

.image-info {
  min-width: 0;
  flex: 1;
}

.image-title {
  min-width: 0;
  color: #1f2933;
  font-size: 29rpx;
  font-weight: 700;
  word-break: break-all;
}

.image-line {
  display: block;
  margin-top: 8rpx;
  color: #5f5548;
  font-size: 24rpx;
  line-height: 1.4;
  word-break: break-all;
}

.image-code {
  color: #3f3a32;
  font-size: 23rpx;
}

.result-highlight {
  color: #0f3d2e;
  font-weight: 600;
}

.confidence-line {
  color: #0f5132;
  font-weight: 600;
}

.secondary-meta {
  color: #8b7e6b;
  font-size: 22rpx;
}

.image-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 18rpx;
}

.mini-btn {
  flex: 1;
  height: 58rpx;
  margin: 0;
  padding: 0 16rpx;
  border-radius: 10rpx;
  font-size: 24rpx;
  line-height: 58rpx;
}

.primary-mini {
  background: #166534;
  color: #ffffff;
}

.secondary-mini {
  background: #eaf5ee;
  color: #166534;
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

.batch-title,
.section-title,
.image-title,
.empty-title {
  color: #0f3d2e;
}

.status-tag,
.status-draft,
.status-collecting,
.status-confirmed {
  background: #e8f7ed;
  color: #15803d;
}

.status-submitted,
.status-identifying,
.status-reviewing,
.primary-tag {
  background: #fff4df;
  color: #d97706;
}

.status-archived {
  background: #f4ead8;
  color: #8a5a2b;
}

.label {
  color: #7c6f5c;
}

.value {
  color: #1f2933;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.text-block {
  border-top-color: #eadfcd;
}

.block-value,
.image-line {
  color: #5f5548;
}

.stat-item {
  border: 1rpx solid #d9e8dc;
  background: #eaf5ee;
}

.stat-item.warning {
  border-color: #f3d39a;
  background: #fff4df;
}

.stat-value {
  color: #0f5132;
}

.stat-label,
.section-count,
.empty-tip {
  color: #7c6f5c;
}

.notice {
  color: #d97706;
}

.image-card {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
}

.thumb {
  background: #f0e8dc;
}

.image-line.image-code {
  color: #3f3a32;
}

.image-line.result-highlight {
  color: #0f3d2e;
}

.image-line.confidence-line {
  color: #0f5132;
}

.image-line.secondary-meta {
  color: #8b7e6b;
}

.primary-mini {
  background: #166534;
  color: #ffffff;
}

.secondary-mini {
  border: 1rpx solid #b7d7c2;
  background: #eaf5ee;
  color: #0f5132;
}

.detail-page {
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

.batch-overview-card .info-row,
.evaluation-card .info-row {
  align-items: center;
  gap: 20rpx;
  margin-bottom: 16rpx;
  padding: 0;
}

.batch-overview-card .label,
.evaluation-card .label {
  width: auto;
  color: #7c6f5c;
  font-size: 26rpx;
}

.batch-overview-card .value,
.evaluation-card .value {
  color: #1f2933;
  font-size: 28rpx;
  font-weight: 500;
}

.collection-info-card .info-block:last-child {
  margin-bottom: 0;
}

.collection-info-card .text-block {
  margin-top: 6rpx;
}

.stat-grid {
  gap: 10rpx;
}

.stat-item {
  padding: 14rpx 8rpx;
  border-radius: 14rpx;
}

.stat-value {
  font-size: 31rpx;
}

.evaluation-summary {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 14rpx;
  background: #f7f1e6;
}

.evaluation-summary .block-value {
  color: #3f3a32;
  line-height: 1.65;
}

.action-card .action-grid {
  gap: 14rpx;
}

.action-card .action-btn {
  border-radius: 18rpx;
  font-weight: 600;
}

.image-section-card .image-card {
  padding: 18rpx;
  border-radius: 18rpx;
}

.image-section-card .image-actions {
  padding-top: 14rpx;
  border-top: 1rpx solid #eadfcd;
}

.mini-btn::after {
  border: 0;
}
</style>
