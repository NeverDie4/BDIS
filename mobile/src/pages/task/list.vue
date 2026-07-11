<template>
  <view class="page">
    <view class="user-card">
      <view>
        <text class="title">我的采集任务</text>
        <text class="sub-title">当前采集员：{{ collector.collectorName || '-' }}</text>
      </view>
      <view class="collector-id">
        <text class="collector-id-label">采集员ID</text>
        <text class="collector-id-value">{{ collector.collectorId || '-' }}</text>
      </view>
    </view>

    <scroll-view class="filter-bar" scroll-x>
      <view class="filter-content">
        <view
          v-for="item in statusOptions"
          :key="item.value || 'all'"
          class="filter-item"
          :class="{ active: taskStatus === item.value }"
          @click="changeStatus(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </scroll-view>

    <view v-if="list.length > 0" class="task-list">
      <view v-for="item in list" :key="item.taskId || item.id" class="task-card" @click="goDetail(item)">
        <view class="task-header">
          <text class="task-title">{{ displayText(item.taskName) }}</text>
          <text class="status-tag" :class="`status-${item.taskStatus || 'unknown'}`">
            {{ formatStatus(item.taskStatus, TASK_STATUS_MAP) }}
          </text>
        </view>

        <view class="info-row">
          <text class="label">任务编码</text>
          <text class="value">{{ displayText(item.taskCode) }}</text>
        </view>
        <view class="info-row">
          <text class="label">药材</text>
          <text class="value">{{ displayText(item.speciesName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">基地</text>
          <text class="value">{{ displayText(item.baseName) }}</text>
        </view>
        <view class="info-row">
          <text class="label">地点</text>
          <text class="value">{{ displayText(item.collectPlace) }}</text>
        </view>
        <view class="info-row">
          <text class="label">计划时间</text>
          <text class="value">{{ formatTaskTime(item) }}</text>
        </view>
        <view class="info-row">
          <text class="label">批次</text>
          <text class="value">{{ formatBatchText(item) }}</text>
        </view>

        <text class="desc">说明：{{ displayText(item.description) }}</text>
      </view>

      <view class="load-more">
        <text v-if="loading">加载中...</text>
        <text v-else-if="finished">没有更多了</text>
        <text v-else>上拉加载更多</text>
      </view>
    </view>

    <view v-else class="empty card">
      <text class="empty-title">{{ errorText || '暂无采集任务' }}</text>
      <text class="empty-tip">请确认 PC 端已发布采集任务，且 collectorId 与当前采集员一致。</text>
      <button class="primary-btn" :loading="loading" @click="reload">重新加载</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { getMyTasks } from '../../api/mobileTaskApi'
import { TASK_STATUS_MAP } from '../../utils/constants'
import { formatDateTime, formatStatus, normalizePageData } from '../../utils/format'
import { getCurrentCollector } from '../../utils/user'

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已发布', value: 'published' },
  { label: '进行中', value: 'in_progress' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const collector = ref(getCurrentCollector())
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const taskStatus = ref('')
const errorText = ref('')
const lastCollectorId = ref(collector.value.collectorId)

onLoad(() => {
  loadTasks(true)
})

onShow(() => {
  const currentCollector = getCurrentCollector()
  if (currentCollector.collectorId !== lastCollectorId.value) {
    collector.value = currentCollector
    lastCollectorId.value = currentCollector.collectorId
    list.value = []
    loadTasks(true)
  }
})

onPullDownRefresh(async () => {
  refreshing.value = true
  await loadTasks(true)
  refreshing.value = false
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (loading.value || finished.value) {
    return
  }

  pageNum.value += 1
  loadTasks(false)
})

async function loadTasks(reset = false) {
  if (loading.value) {
    return
  }

  if (reset) {
    pageNum.value = 1
    finished.value = false
    errorText.value = ''
  }

  loading.value = true
  collector.value = getCurrentCollector()
  lastCollectorId.value = collector.value.collectorId

  try {
    const data = await getMyTasks({
      collectorId: collector.value.collectorId,
      taskStatus: taskStatus.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    const page = normalizePageData(data)
    const records = page.records

    total.value = page.total
    list.value = reset ? records : list.value.concat(records)
    finished.value = (total.value > 0 && list.value.length >= total.value) || records.length < pageSize.value
  } catch (error) {
    console.error('任务加载失败', error)
    uni.showToast({
      title: '任务加载失败，请检查网络或后端服务',
      icon: 'none'
    })

    if (reset && list.value.length === 0) {
      errorText.value = '任务加载失败'
    }
  } finally {
    loading.value = false
  }
}

function changeStatus(value) {
  if (taskStatus.value === value) {
    return
  }

  taskStatus.value = value
  list.value = []
  total.value = 0
  pageNum.value = 1
  finished.value = false
  loadTasks(true)
}

function reload() {
  loadTasks(true)
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function formatTaskTime(item) {
  const startTime = formatDateTime(item.plannedStartTime)
  const endTime = formatDateTime(item.plannedEndTime)

  if (startTime === '-' && endTime === '-') {
    return '-'
  }

  return `${startTime} ~ ${endTime}`
}

function formatBatchText(item) {
  const batchCount = displayText(item.batchCount)
  const unfinishedBatchCount = displayText(item.unfinishedBatchCount)
  return `${batchCount} 个，未完成：${unfinishedBatchCount} 个`
}

function goDetail(item) {
  const taskId = item.taskId || item.id

  if (!taskId) {
    uni.showToast({
      title: '任务ID为空，无法打开详情',
      icon: 'none'
    })
    return
  }

  uni.navigateTo({
    url: `/pages/task/detail?taskId=${taskId}`
  })
}
</script>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 24rpx;
  padding: 30rpx;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.collector-id {
  flex-shrink: 0;
  min-width: 150rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: #eef5ff;
  text-align: center;
}

.collector-id-label {
  display: block;
  color: #64748b;
  font-size: 22rpx;
}

.collector-id-value {
  display: block;
  margin-top: 4rpx;
  color: #1677ff;
  font-size: 30rpx;
  font-weight: 700;
}

.filter-bar {
  width: 100%;
  margin-bottom: 24rpx;
  white-space: nowrap;
}

.filter-content {
  display: flex;
  gap: 16rpx;
}

.filter-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #475569;
  font-size: 26rpx;
  box-shadow: 0 6rpx 18rpx rgba(15, 23, 42, 0.05);
}

.filter-item.active {
  background: #1677ff;
  color: #ffffff;
  font-weight: 600;
}

.task-list {
  padding-bottom: 24rpx;
}

.task-card {
  margin-bottom: 22rpx;
  padding: 28rpx;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.task-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.task-title {
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

.status-published {
  background: #edf7ed;
  color: #1f8a3b;
}

.status-in_progress {
  background: #fff7e6;
  color: #c46a00;
}

.status-completed {
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

.desc {
  display: block;
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #eef2f7;
  color: #475569;
  font-size: 26rpx;
  line-height: 1.5;
}

.empty {
  margin-top: 36rpx;
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

.load-more {
  padding: 28rpx 0 12rpx;
  color: #94a3b8;
  text-align: center;
  font-size: 26rpx;
}
</style>
