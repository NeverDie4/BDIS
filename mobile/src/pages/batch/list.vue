<template>
  <view class="page">
    <view class="card">
      <view class="section-header">
        <view>
          <text class="title">批次列表</text>
          <text class="sub-title">{{ taskId ? '当前任务下的采集批次' : '批次需要从采集任务进入' }}</text>
        </view>
        <view class="collector-box">
          <text class="collector-label">采集员</text>
          <text class="collector-value">{{ collector.collectorId || '-' }}</text>
        </view>
      </view>
    </view>

    <view v-if="!taskId" class="card empty">
      <text class="empty-title">请先选择采集任务</text>
      <text class="empty-tip">批次属于具体采集任务，请从“我的采集任务”进入任务详情后查看或新建批次。</text>
      <button class="primary-btn" @click="goTaskList">查看我的任务</button>
      <button class="secondary-btn" @click="goHome">返回首页</button>
    </view>

    <template v-else>
      <view v-if="errorText" class="card error">
        <text class="empty-title">{{ errorText }}</text>
        <text class="empty-tip">请检查网络或后端服务是否启动</text>
        <button class="primary-btn" :loading="loading" @click="loadBatches">重新加载</button>
      </view>

      <view v-else-if="list.length > 0" class="batch-list">
        <view v-for="item in list" :key="item.batchId || item.id" class="batch-card" @click="goDetail(item)">
          <view class="batch-header">
            <text class="batch-title">{{ displayText(item.batchName) }}</text>
            <text class="status-tag" :class="`status-${item.batchStatus || 'unknown'}`">
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
            <text>图片 {{ displayNumber(item.imageCount) }}</text>
            <text>已识别 {{ displayNumber(item.identifiedCount) }}</text>
            <text>待复核 {{ displayNumber(item.needReviewCount) }}</text>
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
            <text class="label">创建时间</text>
            <text class="value">{{ formatDateTime(item.createTime) }}</text>
          </view>
        </view>
      </view>

      <view v-else class="card empty">
        <text class="empty-title">暂无批次</text>
        <text class="empty-tip">请返回任务详情页，点击“新建批次”开始本次采集。</text>
        <button class="primary-btn" :loading="loading" @click="loadBatches">重新加载</button>
        <button class="secondary-btn" @click="goBack">返回上一页</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getTaskBatches } from '../../api/mobileTaskApi'
import { BATCH_STATUS_MAP, QUALITY_LEVEL_MAP } from '../../utils/constants'
import { formatDateTime, formatStatus } from '../../utils/format'
import { getCurrentCollector } from '../../utils/user'

const taskId = ref('')
const collector = ref(getCurrentCollector())
const list = ref([])
const loading = ref(false)
const errorText = ref('')

onLoad((options) => {
  taskId.value = options.taskId || options.id || ''
})

onShow(() => {
  collector.value = getCurrentCollector()
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
  collector.value = getCurrentCollector()

  try {
    const data = await getTaskBatches(taskId.value, {
      collectorId: collector.value.collectorId
    })
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

.collector-box {
  flex-shrink: 0;
  min-width: 140rpx;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  background: #eef5ff;
  text-align: center;
}

.collector-label {
  display: block;
  color: #64748b;
  font-size: 22rpx;
}

.collector-value {
  display: block;
  margin-top: 4rpx;
  color: #1677ff;
  font-size: 28rpx;
  font-weight: 700;
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
  background: #eef5ff;
  color: #1677ff;
}

.status-cancelled {
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
  width: 140rpx;
  color: #64748b;
}

.value {
  min-width: 0;
  flex: 1;
  color: #111827;
  text-align: right;
  word-break: break-all;
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
