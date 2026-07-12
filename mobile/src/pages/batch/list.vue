<template>
  <view class="page batch-list-page">
    <view class="herb-card list-summary-card">
      <text class="section-title">采集批次</text>
      <text class="summary-desc">{{ taskId ? '查看当前任务下的现场采集批次' : '批次需要从采集任务进入' }}</text>
    </view>

    <view v-if="!taskId" class="herb-card empty">
      <text class="empty-title">请先选择采集任务</text>
      <text class="empty-tip">批次属于具体采集任务，请从“我的采集任务”进入任务详情后查看或新建批次。</text>
      <button class="primary-btn" @click="goTaskList">查看我的任务</button>
      <button class="secondary-btn" @click="goHome">返回首页</button>
    </view>

    <template v-else>
      <view v-if="errorText" class="herb-card error">
        <text class="empty-title">{{ errorText }}</text>
        <text class="empty-tip">请检查网络或后端服务是否启动</text>
        <button class="primary-btn" :loading="loading" @click="loadBatches">重新加载</button>
      </view>

      <view v-else-if="list.length > 0" class="batch-list">
        <view v-for="item in list" :key="item.batchId || item.id" class="herb-card batch-card" @click="goDetail(item)">
          <view class="batch-header">
            <text class="batch-title">{{ displayText(item.batchName) }}</text>
            <text class="status-tag" :class="`status-${item.batchStatus || 'unknown'}`">
              {{ formatStatus(item.batchStatus, BATCH_STATUS_MAP) }}
            </text>
          </view>

          <view class="info-block code-block">
            <text class="info-block-label">批次编码</text>
            <text class="info-block-value code-text">{{ displayText(item.batchCode) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">药材名称</text>
            <text class="info-value">{{ displayText(item.speciesName) }}</text>
          </view>
          <view class="info-block">
            <text class="info-block-label">产地 / 基地</text>
            <text class="info-block-value">{{ displayText(item.originPlace) }} / {{ displayText(item.baseName) }}</text>
          </view>
          <view class="stats-row">
            <text>图片 {{ displayNumber(item.imageCount) }}</text>
            <text>已识别 {{ displayNumber(item.identifiedCount) }}</text>
            <text>待复核 {{ displayNumber(item.needReviewCount) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">最终药材</text>
            <text class="info-value">{{ displayText(item.finalSpeciesName) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">质量等级</text>
            <text class="info-value">{{ formatStatus(item.qualityLevel, QUALITY_LEVEL_MAP) }}</text>
          </view>
          <view class="info-block create-time-block">
            <text class="info-block-label">创建时间</text>
            <text class="info-block-value">{{ formatDateTime(item.createTime) }}</text>
          </view>
          <view class="detail-link">查看详情 <text>›</text></view>
        </view>
      </view>

      <view v-else class="herb-card empty">
        <text class="empty-title">暂无采集批次</text>
        <text class="empty-tip">请返回任务详情页，点击“新建批次”开始本次采集。</text>
        <button class="primary-btn" :loading="loading" @click="loadBatches">重新加载</button>
        <button class="secondary-btn" @click="goBack">返回上一页</button>
      </view>
    </template>
    <AssistantFloat />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getTaskBatches } from '../../api/mobileTaskApi'
import { BATCH_STATUS_MAP, QUALITY_LEVEL_MAP } from '../../utils/constants'
import { formatDateTime, formatStatus } from '../../utils/format'

const taskId = ref('')
const list = ref([])
const loading = ref(false)
const errorText = ref('')

onLoad((options) => {
  taskId.value = options.taskId || options.id || ''
})

onShow(() => {
  if (taskId.value) {
    loadBatches()
  }
})

onPullDownRefresh(async () => {
  try {
    if (taskId.value) {
      await loadBatches()
    }
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function loadBatches() {
  if (!taskId.value || loading.value) {
    return
  }

  loading.value = true
  errorText.value = ''

  try {
    const data = await getTaskBatches(taskId.value)
    list.value = normalizeBatchList(data)
  } catch (error) {
    console.error('批次列表加载失败', error)
    errorText.value = '批次列表加载失败'
    uni.showToast({
      title: '批次列表加载失败，请检查网络或后端服务',
      icon: 'none'
    })
  } finally {
    loading.value = false
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

function goDetail(item) {
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

function goTaskList() {
  uni.switchTab({
    url: '/pages/task/list'
  })
}

function goHome() {
  uni.switchTab({
    url: '/pages/index/index'
  })
}

function goBack() {
  uni.navigateBack()
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function displayNumber(value) {
  return value === undefined || value === null || value === '' ? 0 : value
}
</script>

<style scoped>
.section-header,
.batch-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.batch-list {
  padding-bottom: 24rpx;
}

.batch-card {
  margin-bottom: 22rpx;
  padding: 28rpx;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.batch-header {
  margin-bottom: 18rpx;
}

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

.status-draft,
.status-collecting {
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
  width: 140rpx;
  color: #64748b;
}

.value {
  min-width: 0;
  flex: 1;
  color: #1f2933;
  text-align: right;
  word-break: break-all;
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

.batch-card {
  border: 1rpx solid #eadfcd;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.batch-title,
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
.status-reviewing {
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

.stats-row {
  color: #5f5548;
}

.stats-row text {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
}

.empty-tip {
  color: #8b7e6b;
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

.batch-list-page {
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

.list-summary-card {
  padding: 30rpx;
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 14rpx;
  color: #0f3d2e;
  font-size: 32rpx;
  font-weight: 700;
}

.section-title::before {
  width: 8rpx;
  height: 32rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #166534;
  content: '';
}

.summary-desc {
  display: block;
  margin-left: 22rpx;
  color: #7c6f5c;
  font-size: 25rpx;
  line-height: 1.5;
}

.batch-card {
  padding: 26rpx 28rpx;
  border-radius: 24rpx;
}

.batch-title {
  font-size: 31rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 16rpx;
  padding: 0;
}

.info-label {
  flex-shrink: 0;
  color: #7c6f5c;
  font-size: 26rpx;
}

.info-value {
  min-width: 0;
  flex: 1;
  color: #1f2933;
  font-size: 27rpx;
  font-weight: 500;
  line-height: 1.45;
  text-align: right;
  overflow-wrap: anywhere;
}

.info-block {
  margin-bottom: 20rpx;
}

.info-block-label,
.info-block-value {
  display: block;
  text-align: left;
}

.info-block-label {
  margin-bottom: 7rpx;
  color: #7c6f5c;
  font-size: 25rpx;
}

.info-block-value {
  color: #1f2933;
  font-size: 27rpx;
  line-height: 1.55;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.code-text {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 24rpx;
  line-height: 1.5;
  overflow-wrap: normal;
  word-break: break-all;
}

.stats-row {
  margin: 8rpx 0 18rpx;
}

.stats-row text {
  border-color: #b7d7c2;
  border-radius: 999rpx;
  background: #eaf5ee;
  color: #0f5132;
  font-size: 23rpx;
}

.create-time-block {
  margin-top: 4rpx;
}

.detail-link {
  margin-top: 8rpx;
  padding-top: 14rpx;
}
</style>
