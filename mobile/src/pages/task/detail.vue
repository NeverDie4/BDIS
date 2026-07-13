<template>
  <view class="page detail-page">
    <view v-if="errorText" class="card error">
      <text class="empty-title">{{ errorText }}</text>
      <text class="empty-tip">{{ errorTip }}</text>
      <button class="primary-btn" :loading="loading" @click="loadDetail">重新加载</button>
    </view>

    <template v-else>
      <view class="herb-card task-card task-overview-card">
        <view class="task-header">
          <text class="task-title">{{ displayText(detail.taskName) }}</text>
          <text class="status-tag" :class="`status-${detail.taskStatus || 'unknown'}`">
            {{ formatStatus(detail.taskStatus, TASK_STATUS_MAP) }}
          </text>
        </view>

        <view class="info-block">
          <text class="info-block-label">任务编码</text>
          <text class="info-block-value code-text">{{ displayText(detail.taskCode) }}</text>
        </view>
        <view class="info-row">
          <text class="label">药材名称</text>
          <text class="value">{{ displayText(detail.speciesName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">采集员</text>
          <text class="value">{{ formatCollector(detail) }}</text>
        </view>
      </view>

      <view class="herb-card collection-info-card">
        <text class="section-title">采集信息</text>
        <view class="info-block">
          <text class="info-block-label">基地名称</text>
          <text class="info-block-value">{{ displayText(detail.baseName) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">采集地点</text>
          <text class="info-block-value">{{ displayText(detail.collectPlace) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">计划时间</text>
          <text class="info-block-value">{{ formatTaskTime(detail) }}</text>
        </view>
      </view>

      <view class="herb-card description-card">
        <text class="section-title">任务说明</text>
        <view class="text-block description-block">
          <text class="block-value">{{ displayText(detail.description) }}</text>
        </view>
        <view class="text-block">
          <text class="label block-label">备注</text>
          <text class="block-value">{{ displayText(detail.remark) }}</text>
        </view>
      </view>

      <view class="herb-card task-batch-card">
        <view class="section-header">
          <text class="section-title">任务下批次</text>
          <text class="section-count">{{ batches.length }} 个</text>
        </view>
        <view class="action-bar">
          <button class="primary-btn action-btn" @click="goCreateBatch">新建批次</button>
          <button class="secondary-btn action-btn" :loading="loading" @click="loadDetail">刷新</button>
        </view>
      </view>

      <view v-if="batches.length > 0" class="batch-list">
        <view v-for="item in batches" :key="item.batchId || item.id" class="herb-card batch-card" @click="goBatchDetail(item)">
          <view class="task-header">
            <text class="batch-title">{{ displayText(item.batchName) }}</text>
            <text class="status-tag" :class="`batch-status-${item.batchStatus || 'unknown'}`">
              {{ formatStatus(item.batchStatus, BATCH_STATUS_MAP) }}
            </text>
          </view>

          <view class="info-block compact-block">
            <text class="info-block-label">批次编码</text>
            <text class="info-block-value code-text">{{ displayText(item.batchCode) }}</text>
          </view>
          <view class="info-row">
            <text class="label">药材</text>
            <text class="value">{{ displayText(item.speciesName) }}</text>
          </view>
          <view class="info-row">
            <text class="label">产地</text>
            <text class="value">{{ displayText(item.originPlace) }}</text>
          </view>
          <view class="stats-row">
            <text>图片：{{ displayNumber(item.imageCount) }} 张</text>
            <text>已识别：{{ displayNumber(item.identifiedCount) }} 张</text>
            <text>待复核：{{ displayNumber(item.needReviewCount) }} 张</text>
          </view>
          <view class="info-row">
            <text class="label">已复核</text>
            <text class="value">{{ displayNumber(item.reviewedCount) }} 张</text>
          </view>
          <view class="info-row">
            <text class="label">最终药材</text>
            <text class="value">{{ displayText(item.finalSpeciesName) }}</text>
          </view>
          <view class="info-row">
            <text class="label">质量等级</text>
            <text class="value">{{ formatStatus(item.qualityLevel, QUALITY_LEVEL_MAP) }}</text>
          </view>
          <view class="info-row">
            <text class="label">质量分</text>
            <text class="value">{{ formatScore(item.qualityScore) }}</text>
          </view>
          <view class="info-row">
            <text class="label">创建时间</text>
            <text class="value">{{ formatDateTime(item.createTime) }}</text>
          </view>
          <view class="detail-link">查看详情 <text>›</text></view>
        </view>
      </view>

      <view v-else class="herb-card empty">
        <text class="empty-title">暂无批次</text>
        <text class="empty-tip">点击“新建批次”开始本次采集</text>
      </view>
    </template>
    <AssistantFloat />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getTaskBatches, getTaskDetail } from '../../api/mobileTaskApi'
import { BATCH_STATUS_MAP, QUALITY_LEVEL_MAP, TASK_STATUS_MAP } from '../../utils/constants'
import { formatDateTime, formatScore, formatStatus } from '../../utils/format'

const taskId = ref('')
const detail = ref({})
const batches = ref([])
const loading = ref(false)
const errorText = ref('')
const errorTip = ref('请检查网络或后端服务是否启动')

onLoad((options) => {
  taskId.value = options.taskId || options.id || ''

  if (!taskId.value) {
    errorText.value = '任务 ID 不存在'
    errorTip.value = '请从任务列表重新进入任务详情'
  }
})

onShow(() => {
  if (taskId.value) {
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
  if (!taskId.value) {
    errorText.value = '任务 ID 不存在'
    errorTip.value = '请从任务列表重新进入任务详情'
    return
  }

  if (loading.value) {
    return
  }

  loading.value = true
  errorText.value = ''
  errorTip.value = '请检查网络或后端服务是否启动'

  try {
    const data = await getTaskDetail(taskId.value)
    detail.value = normalizeTaskDetail(data)
    batches.value = await resolveBatchList(data)
  } catch (error) {
    console.error('任务详情加载失败', error)
    errorText.value = '任务加载失败'
    errorTip.value = '请检查网络或后端服务是否启动'
    uni.showToast({
      title: '任务加载失败，请检查网络或后端服务',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function normalizeTaskDetail(data) {
  const source = data || {}
  return source.task || source.detail || source
}

async function resolveBatchList(data) {
  const source = data || {}
  const detailSource = normalizeTaskDetail(data)
  const hasBatchField =
    Array.isArray(source.batches) ||
    Array.isArray(source.batchList) ||
    Array.isArray(source.records) ||
    Array.isArray(detailSource.batches) ||
    Array.isArray(detailSource.batchList) ||
    Array.isArray(detailSource.records)

  if (hasBatchField) {
    return normalizeBatchList(source.batches || source.batchList || source.records || detailSource.batches || detailSource.batchList || detailSource.records)
  }

  try {
    const data = await getTaskBatches(taskId.value)
    return normalizeBatchList(data)
  } catch (error) {
    console.error('任务批次加载失败', error)
    return []
  }
}

function normalizeBatchList(data) {
  if (Array.isArray(data)) {
    return data
  }

  const source = data || {}
  const records = source.batches || source.batchList || source.records || source.list || source.rows || source.data || []
  return Array.isArray(records) ? records : []
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function displayNumber(value) {
  return value === undefined || value === null || value === '' ? 0 : value
}

function formatTaskTime(item) {
  const startTime = formatDateTime(item.plannedStartTime)
  const endTime = formatDateTime(item.plannedEndTime)

  if (startTime === '-' && endTime === '-') {
    return '-'
  }

  return `${startTime} ~ ${endTime}`
}

function formatCollector(item) {
  const name = item.collectorName
  const id = item.collectorId

  if (!name && !id) {
    return '-'
  }

  return `${displayText(name)}（${displayText(id)}）`
}

function goCreateBatch() {
  if (!taskId.value) {
    uni.showToast({
      title: '任务ID为空，无法新建批次',
      icon: 'none'
    })
    return
  }

  uni.navigateTo({
    url: `/pages/batch/create?taskId=${taskId.value}`
  })
}

function goBatchDetail(item) {
  const batchId = item.batchId || item.id

  if (!batchId) {
    uni.showToast({
      title: '批次ID为空，无法打开详情',
      icon: 'none'
    })
    return
  }

  uni.navigateTo({
    url: `/pages/batch/detail?batchId=${batchId}`
  })
}
</script>

<style scoped>
.detail-page {
  padding-top: 32rpx;
}

.task-card,
.batch-card {
  margin-bottom: 24rpx;
}

.task-header,
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 20rpx;
}

.task-title,
.batch-title {
  min-width: 0;
  color: #1f2933;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-all;
}

.status-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #eaf5ee;
  color: #166534;
  font-size: 23rpx;
  line-height: 1;
}

.status-published,
.batch-status-collecting {
  background: #edf7ed;
  color: #1f8a3b;
}

.status-in_progress,
.batch-status-identifying,
.batch-status-reviewing {
  background: #fff7e6;
  color: #c46a00;
}

.status-completed,
.batch-status-submitted,
.batch-status-confirmed,
.batch-status-archived {
  background: #eaf5ee;
  color: #166534;
}

.status-cancelled,
.batch-status-cancelled {
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

.section-title {
  color: #1f2933;
  font-size: 32rpx;
  font-weight: 700;
}

.section-count {
  color: #64748b;
  font-size: 26rpx;
}

.action-bar {
  display: flex;
  gap: 18rpx;
}

.action-btn {
  flex: 1;
  margin-top: 0;
}

.batch-list {
  padding-bottom: 24rpx;
}

.batch-card {
  padding: 28rpx;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin: 14rpx 0;
  color: #5f5548;
  font-size: 25rpx;
}

.stats-row text {
  padding: 8rpx 12rpx;
  border-radius: 10rpx;
  background: #fffaf2;
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

.task-card,
.batch-card {
  border: 1rpx solid #eadfcd;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.task-title,
.batch-title,
.section-title,
.empty-title {
  color: #0f3d2e;
}

.status-tag {
  background: #e8f7ed;
  color: #15803d;
}

.batch-status-identifying,
.batch-status-reviewing,
.batch-status-submitted {
  background: #fff4df;
  color: #d97706;
}

.batch-status-confirmed {
  background: #e8f7ed;
  color: #15803d;
}

.batch-status-archived {
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
  color: #1f2933;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 25rpx;
  line-height: 1.45;
  overflow-wrap: normal;
  word-break: break-all;
}

.compact-block {
  margin-bottom: 12rpx;
}

.text-block {
  border-top-color: #eadfcd;
}

.block-value,
.stats-row {
  color: #5f5548;
}

.stats-row text {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
}

.section-count,
.empty-tip {
  color: #8b7e6b;
}

.description-block {
  margin-top: 18rpx;
  padding-top: 0;
  border-top: 0;
}

.task-card .info-row {
  justify-content: flex-start;
  gap: 18rpx;
  padding: 12rpx 0;
}

.task-card .label {
  width: 124rpx;
}

.task-card .value {
  text-align: left;
  line-height: 1.55;
}

.description-card .block-value {
  color: #3f3a32;
  line-height: 1.7;
}

.action-bar button.primary-btn {
  background: linear-gradient(135deg, #166534 0%, #0f5132 100%) !important;
  color: #ffffff !important;
}

.action-bar button.secondary-btn {
  border-color: #b7d7c2 !important;
  background: #eaf5ee !important;
  color: #0f5132 !important;
}

.detail-link {
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #eadfcd;
  color: #166534;
  font-size: 26rpx;
  font-weight: 600;
  text-align: right;
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
  margin-bottom: 22rpx;
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

.task-overview-card .info-row,
.collection-info-card .info-row {
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 16rpx;
  padding: 0;
}

.task-overview-card .label {
  width: auto;
  color: #7c6f5c;
  font-size: 26rpx;
}

.task-overview-card .value {
  color: #1f2933;
  font-size: 28rpx;
  font-weight: 500;
  text-align: right;
}

.collection-info-card .info-block:last-child {
  margin-bottom: 0;
}

.description-card .description-block {
  margin-top: 0;
  padding: 18rpx 20rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 14rpx;
  background: #f7f1e6;
}

.description-card .text-block:not(.description-block) {
  margin-top: 18rpx;
}

.action-btn {
  height: 78rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  line-height: 78rpx;
}

.task-batch-card .section-header {
  margin-bottom: 18rpx;
}

.batch-card {
  padding: 24rpx;
  border-radius: 20rpx;
  box-shadow: none;
}
</style>
