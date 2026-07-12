<template>
  <view class="page tab-page task-list-page">
    <view class="herb-card list-summary-card">
      <text class="section-title">我的采集任务</text>
      <text class="summary-account">当前账号：{{ authUser?.realName || authUser?.username || '-' }}</text>
      <text class="summary-desc">查看已分配的中药材采集任务</text>
    </view>

    <scroll-view class="filter-bar" scroll-x :show-scrollbar="false">
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
      <view v-for="item in list" :key="item.taskId || item.id" class="herb-card task-card" @click="goDetail(item)">
        <view class="task-header">
          <text class="task-title">{{ displayText(item.taskName) }}</text>
          <text class="status-tag" :class="`status-${item.taskStatus || 'unknown'}`">
            {{ formatStatus(item.taskStatus, TASK_STATUS_MAP) }}
          </text>
        </view>

        <view class="info-block code-block">
          <text class="info-block-label">任务编码</text>
          <text class="info-block-value code-text">{{ displayText(item.taskCode) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">药材名称</text>
          <text class="info-value">{{ displayText(item.speciesName) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">基地名称</text>
          <text class="info-value">{{ displayText(item.baseName) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">采集地点</text>
          <text class="info-block-value">{{ displayText(item.collectPlace) }}</text>
        </view>
        <view class="info-block">
          <text class="info-block-label">计划时间</text>
          <text class="info-block-value">{{ formatTaskTime(item) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">采集批次</text>
          <text class="info-value">{{ formatBatchText(item) }}</text>
        </view>

        <view class="task-desc">
          <text class="task-desc-label">任务说明</text>
          <text class="task-desc-value">{{ displayText(item.description) }}</text>
        </view>
        <view class="detail-link">查看详情 <text>›</text></view>
      </view>

      <view class="load-more">
        <text v-if="loading">加载中...</text>
        <text v-else-if="finished">没有更多了</text>
        <text v-else>上拉加载更多</text>
      </view>
    </view>

    <view v-else class="empty herb-card">
      <text class="empty-title">{{ errorText || '暂无采集任务' }}</text>
      <text class="empty-tip">请确认 PC 端已发布采集任务，且任务已分配给当前采集员账号。</text>
      <button class="primary-btn" :loading="loading" @click="reload">重新加载</button>
    </view>
    <AssistantFloat />
    <AppTabBar />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import AppTabBar from '../../components/AppTabBar.vue'
import { getMyTasks } from '../../api/mobileTaskApi'
import { TASK_STATUS_MAP } from '../../utils/constants'
import { formatDateTime, formatStatus, normalizePageData } from '../../utils/format'
import { getAuthUser, requireLogin } from '../../utils/auth'

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已发布', value: 'published' },
  { label: '进行中', value: 'in_progress' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const authUser = ref(getAuthUser())
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const taskStatus = ref('')
const errorText = ref('')

onLoad(() => {
  if (requireLogin()) {
    loadTasks(true)
  }
})

onShow(() => {
  uni.hideTabBar({ animation: false })
  if (!requireLogin()) {
    return
  }

  authUser.value = getAuthUser()
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
  authUser.value = getAuthUser()

  try {
    const data = await getMyTasks({
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
.tab-page {
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}

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
  color: #5f5548;
  font-size: 26rpx;
  box-shadow: 0 6rpx 18rpx rgba(15, 23, 42, 0.05);
}

.filter-item.active {
  background: #166534;
  color: #ffffff;
  font-weight: 600;
}

.task-list {
  padding-bottom: 12rpx;
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

.status-published {
  background: #edf7ed;
  color: #1f8a3b;
}

.status-in_progress {
  background: #fff7e6;
  color: #c46a00;
}

.status-completed {
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

.desc {
  display: block;
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #eef2f7;
  color: #5f5548;
  font-size: 26rpx;
  line-height: 1.5;
}

.empty {
  margin-top: 36rpx;
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

.load-more {
  padding: 28rpx 0 12rpx;
  color: #94a3b8;
  text-align: center;
  font-size: 26rpx;
}

.user-card,
.task-card {
  border: 1rpx solid #eadfcd;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.user-card {
  background: #fffaf2;
}

.filter-item {
  border: 1rpx solid #eadfcd;
  background: #ffffff;
  box-shadow: none;
  color: #7c6f5c;
}

.filter-item.active {
  border-color: #166534;
  background: #166534;
  color: #ffffff;
}

.task-title,
.empty-title {
  color: #0f3d2e;
}

.status-tag {
  background: #e8f7ed;
  color: #15803d;
}

.status-in_progress,
.status-completed {
  background: #e8f7ed;
  color: #15803d;
}

.label {
  color: #7c6f5c;
}

.value {
  color: #1f2933;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.desc {
  border-top-color: #eadfcd;
  color: #5f5548;
}

.empty-tip,
.load-more {
  color: #8b7e6b;
}

.detail-link {
  margin-top: 16rpx;
  color: #166534;
  font-size: 26rpx;
  font-weight: 600;
  text-align: right;
}

.task-list-page {
  padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
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
  position: relative;
  overflow: hidden;
  padding: 30rpx;
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
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

.summary-account,
.summary-desc {
  display: block;
  margin-left: 22rpx;
}

.summary-account {
  color: #0f5132;
  font-size: 27rpx;
  font-weight: 600;
}

.summary-desc {
  margin-top: 8rpx;
  color: #7c6f5c;
  font-size: 25rpx;
}

.filter-bar {
  box-sizing: border-box;
  padding: 0 2rpx 4rpx;
}

.filter-content {
  box-sizing: border-box;
  padding-right: 24rpx;
}

.filter-item {
  height: 60rpx;
  padding: 0 24rpx;
  background: #fffaf2;
  font-size: 25rpx;
}

.task-card {
  padding: 26rpx 28rpx;
  border-radius: 24rpx;
}

.task-header {
  margin-bottom: 14rpx;
}

.task-title {
  font-size: 31rpx;
  line-height: 1.4;
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

.code-block {
  margin-top: -2rpx;
}

.code-text {
  color: #1f2933;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 24rpx;
  line-height: 1.5;
  overflow-wrap: normal;
  word-break: break-all;
}

.task-desc {
  margin-top: 6rpx;
  padding: 16rpx 18rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 14rpx;
  background: #f7f1e6;
}

.task-desc-label,
.task-desc-value {
  display: block;
}

.task-desc-label {
  color: #7c6f5c;
  font-size: 24rpx;
}

.task-desc-value {
  margin-top: 7rpx;
  color: #3f3a32;
  font-size: 26rpx;
  line-height: 1.55;
}

.detail-link {
  padding-top: 14rpx;
  border-top: 1rpx solid #eadfcd;
}
</style>
