<template>
  <view class="page">
    <view v-if="errorText" class="card error">
      <text class="empty-title">{{ errorText }}</text>
      <text class="empty-tip">{{ errorTip }}</text>
      <button class="primary-btn" :loading="loading" @click="loadDetail">重新加载</button>
    </view>

    <template v-else>
      <view class="card task-card">
        <view class="task-header">
          <text class="task-title">{{ displayText(detail.taskName) }}</text>
          <text class="status-tag" :class="`status-${detail.taskStatus || 'unknown'}`">
            {{ formatStatus(detail.taskStatus, TASK_STATUS_MAP) }}
          </text>
        </view>

        <view class="info-row">
          <text class="label">任务编码</text>
          <text class="value">{{ displayText(detail.taskCode) }}</text>
        </view>
        <view class="info-row">
          <text class="label">药材名称</text>
          <text class="value">{{ displayText(detail.speciesName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">基地名称</text>
          <text class="value">{{ displayText(detail.baseName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">采集地点</text>
          <text class="value">{{ displayText(detail.collectPlace) }}</text>
        </view>
        <view class="info-row">
          <text class="label">计划时间</text>
          <text class="value">{{ formatTaskTime(detail) }}</text>
        </view>
        <view class="info-row">
          <text class="label">采集员</text>
          <text class="value">{{ formatCollector(detail) }}</text>
        </view>

        <view class="text-block">
          <text class="label block-label">任务说明</text>
          <text class="block-value">{{ displayText(detail.description) }}</text>
        </view>
        <view class="text-block">
          <text class="label block-label">备注</text>
          <text class="block-value">{{ displayText(detail.remark) }}</text>
        </view>
      </view>

      <view class="card">
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
        <view v-for="item in batches" :key="item.batchId || item.id" class="batch-card" @click="goBatchDetail(item)">
          <view class="task-header">
            <text class="batch-title">{{ displayText(item.batchName) }}</text>
            <text class="status-tag" :class="`batch-status-${item.batchStatus || 'unknown'}`">
              {{ formatStatus(item.batchStatus, BATCH_STATUS_MAP) }}
            </text>
          </view>

          <view class="info-row">
            <text class="label">批次编码</text>
            <text class="value">{{ displayText(item.batchCode) }}</text>
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
        </view>
      </view>

      <view v-else class="card empty">
        <text class="empty-title">暂无批次</text>
        <text class="empty-tip">点击“新建批次”开始本次采集</text>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getTaskBatches, getTaskDetail } from '../../api/mobileTaskApi'
import { BATCH_STATUS_MAP, QUALITY_LEVEL_MAP, TASK_STATUS_MAP } from '../../utils/constants'
import { formatDateTime, formatScore, formatStatus } from '../../utils/format'
import { getCurrentCollector } from '../../utils/user'

const taskId = ref('')
const collector = ref(getCurrentCollector())
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
  collector.value = getCurrentCollector()

  try {
    const data = await getTaskDetail(taskId.value, {
      collectorId: collector.value.collectorId
    })
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
    const data = await getTaskBatches(taskId.value, {
      collectorId: collector.value.collectorId
    })
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
  const name = item.collectorName || collector.value.collectorName
  const id = item.collectorId || collector.value.collectorId

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
  color: #111827;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-all;
}

.status-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #eef5ff;
  color: #1677ff;
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
  background: #eef5ff;
  color: #1677ff;
}

.status-cancelled,
.batch-status-cancelled {
  background: #f1f5f9;
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
  color: #111827;
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
  color: #475569;
  line-height: 1.55;
}

.section-title {
  color: #111827;
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
  color: #475569;
  font-size: 25rpx;
}

.stats-row text {
  padding: 8rpx 12rpx;
  border-radius: 10rpx;
  background: #f8fafc;
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
