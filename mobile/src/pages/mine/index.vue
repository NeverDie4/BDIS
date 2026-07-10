<template>
  <view class="page">
    <view class="card profile-card">
      <text class="section-title">采集员信息</text>
      <view class="profile-main">
        <view class="avatar">{{ collectorInitial }}</view>
        <view class="profile-info">
          <text class="collector-name">{{ currentCollector.collectorName }}</text>
          <text class="collector-mode">本地模拟采集员</text>
        </view>
      </view>
      <view class="info-row">
        <text class="label">当前采集员</text>
        <text class="value">{{ currentCollector.collectorName }}</text>
      </view>
      <view class="info-row">
        <text class="label">采集员 ID</text>
        <text class="value">{{ currentCollector.collectorId }}</text>
      </view>
      <view class="info-row">
        <text class="label">当前模式</text>
        <text class="value">本地模拟采集员</text>
      </view>
      <text class="tip">当前项目暂未接入正式登录系统，手机端使用本地缓存中的 collectorId 模拟当前采集人员。</text>
    </view>

    <view class="card">
      <text class="section-title">编辑采集员</text>
      <view class="form-item">
        <text class="form-label">collectorId</text>
        <input v-model="form.collectorId" class="form-input" type="number" placeholder="请输入采集员 ID" />
      </view>
      <view class="form-item">
        <text class="form-label">collectorName</text>
        <input v-model="form.collectorName" class="form-input" placeholder="请输入采集员姓名" />
      </view>
      <view class="button-row">
        <button class="primary-btn row-btn" @click="handleSave">保存</button>
        <button class="secondary-btn row-btn" @click="handleResetDefault">恢复默认</button>
      </view>
    </view>

    <view class="card">
      <text class="section-title">系统配置</text>
      <view class="info-row">
        <text class="label">后端地址</text>
        <text class="value code-value">{{ config.baseUrl }}</text>
      </view>
      <view class="info-row">
        <text class="label">接口前缀</text>
        <text class="value code-value">{{ config.mobilePrefix }}</text>
      </view>
      <view class="warning">
        <text>如果在真机或模拟器中访问后端，请不要使用 localhost，需要将 baseUrl 改成电脑的局域网 IP，例如 http://192.168.x.x:8080。</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">快捷入口</text>
      <view class="action-list">
        <button class="primary-btn" @click="goTaskList">查看我的任务</button>
        <button class="secondary-btn" @click="goHome">返回首页</button>
        <button class="danger-btn" @click="handleClearCache">清除缓存</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import config from '../../config'
import {
  clearCurrentCollector,
  getCurrentCollector,
  getDefaultCollector,
  setCurrentCollector
} from '../../utils/user'

const currentCollector = ref(getCurrentCollector())
const form = reactive({
  collectorId: '',
  collectorName: ''
})

const collectorInitial = computed(() => {
  const name = currentCollector.value.collectorName || '采'
  return name.slice(0, 1)
})

onShow(() => {
  loadCollector()
})

function loadCollector() {
  currentCollector.value = getCurrentCollector()
  form.collectorId = String(currentCollector.value.collectorId)
  form.collectorName = currentCollector.value.collectorName
}

function handleSave() {
  const collectorId = String(form.collectorId || '').trim()
  const collectorName = String(form.collectorName || '').trim()

  if (!collectorId) {
    showToast('请输入采集员 ID')
    return
  }

  if (!/^\d+$/.test(collectorId)) {
    showToast('采集员 ID 必须是数字')
    return
  }

  if (!collectorName) {
    showToast('请输入采集员姓名')
    return
  }

  currentCollector.value = setCurrentCollector({
    collectorId: Number(collectorId),
    collectorName
  })
  loadCollector()
  uni.showToast({
    title: '采集员信息已保存',
    icon: 'success'
  })
}

function handleResetDefault() {
  uni.showModal({
    title: '恢复默认',
    content: '确定恢复默认采集员信息吗？',
    success: (res) => {
      if (!res.confirm) {
        return
      }

      currentCollector.value = setCurrentCollector(getDefaultCollector())
      loadCollector()
      uni.showToast({
        title: '已恢复默认',
        icon: 'success'
      })
    }
  })
}

function handleClearCache() {
  uni.showModal({
    title: '清除缓存',
    content: '确定清除本地缓存吗？',
    success: (res) => {
      if (!res.confirm) {
        return
      }

      clearCurrentCollector()
      loadCollector()
      uni.showToast({
        title: '缓存已清除',
        icon: 'success'
      })
    }
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

function showToast(title) {
  uni.showToast({
    title,
    icon: 'none'
  })
}
</script>

<style scoped>
.profile-card {
  overflow: hidden;
}

.section-title {
  display: block;
  margin-bottom: 20rpx;
  color: #111827;
  font-size: 32rpx;
  font-weight: 700;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 22rpx;
  margin-bottom: 24rpx;
  padding: 22rpx;
  border-radius: 16rpx;
  background: #f8fafc;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  background: #1677ff;
  color: #ffffff;
  font-size: 38rpx;
  font-weight: 700;
}

.profile-info {
  min-width: 0;
  flex: 1;
}

.collector-name {
  display: block;
  color: #111827;
  font-size: 34rpx;
  font-weight: 700;
  word-break: break-all;
}

.collector-mode {
  display: block;
  margin-top: 8rpx;
  color: #64748b;
  font-size: 25rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22rpx;
  padding: 12rpx 0;
}

.label {
  flex-shrink: 0;
  width: 170rpx;
  color: #64748b;
}

.value {
  min-width: 0;
  flex: 1;
  color: #111827;
  text-align: right;
  word-break: break-all;
}

.code-value {
  font-family: Consolas, Monaco, monospace;
  font-size: 25rpx;
}

.tip {
  display: block;
  margin-top: 20rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #eef5ff;
  color: #315f9d;
  font-size: 25rpx;
  line-height: 1.55;
}

.form-item {
  margin-bottom: 22rpx;
}

.form-label {
  display: block;
  margin-bottom: 10rpx;
  color: #475569;
  font-size: 26rpx;
}

.form-input {
  height: 78rpx;
  padding: 0 20rpx;
  border: 1rpx solid #dbe3ef;
  border-radius: 12rpx;
  background: #ffffff;
  color: #111827;
  font-size: 28rpx;
}

.button-row {
  display: flex;
  gap: 18rpx;
}

.row-btn {
  flex: 1;
  margin-top: 0;
}

.warning {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #fff7e6;
  color: #9a5b00;
  font-size: 25rpx;
  line-height: 1.55;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.action-list button {
  margin-top: 0;
}

.danger-btn {
  width: 100%;
  height: 84rpx;
  border-radius: 14rpx;
  background: #fff1f2;
  color: #be123c;
  font-size: 28rpx;
  line-height: 84rpx;
}
</style>
