<template>
  <view class="page mine-page tab-page">
    <view class="user-profile-card">
      <view class="profile-avatar">采</view>
      <view class="profile-content">
        <text class="profile-name">{{ userName }}</text>
        <text class="profile-sub">{{ profileStatusText }}</text>
        <text class="profile-account">账号：{{ accountText }}</text>
      </view>
    </view>

    <view class="mine-card quick-action-card">
      <view class="card-heading">
        <text class="section-title">采集工作台</text>
        <text class="section-tip">快捷入口</text>
      </view>
      <view class="action-grid">
        <button class="action-item" @click="goTaskList">
          <text class="action-icon">任</text>
          <text class="action-title">我的任务</text>
          <text class="action-desc">查看已分配采集任务</text>
        </button>
        <button class="action-item" @click="goBatchList">
          <text class="action-icon">批</text>
          <text class="action-title">采集批次</text>
          <text class="action-desc">管理现场采集批次</text>
        </button>
        <button class="action-item" @click="openAssistant">
          <text class="action-icon">AI</text>
          <text class="action-title">AI 小助手</text>
          <text class="action-desc">咨询采集流程与规范</text>
        </button>
        <button class="action-item" @click="showHelp">
          <text class="action-icon">帮</text>
          <text class="action-title">使用帮助</text>
          <text class="action-desc">查看移动采集流程</text>
        </button>
      </view>
    </view>

    <view class="mine-card info-card">
      <text class="section-title">账号信息</text>
      <view class="account-list">
        <view class="account-row">
          <text class="account-label">用户ID</text>
          <text class="account-value">{{ authUser?.userId || '-' }}</text>
        </view>
        <view class="account-row">
          <text class="account-label">账号</text>
          <text class="account-value">{{ accountText }}</text>
        </view>
        <view class="account-row">
          <text class="account-label">身份角色</text>
          <text class="account-value">采集员</text>
        </view>
        <view class="account-row">
          <text class="account-label">登录状态</text>
          <text class="account-value status-value">{{ loggedIn ? '正常' : '未登录' }}</text>
        </view>
      </view>
    </view>

    <view class="mine-card system-card">
      <text class="section-title">移动采集端</text>
      <text class="system-name">本草研究院标本馆移动采集端</text>
      <text class="system-desc">
        支持任务查看、批次创建、现场图片上传、识别汇总与批次提交。
      </text>
    </view>

    <view class="logout-card">
      <button
        v-if="loggedIn"
        class="logout-btn"
        :loading="loggingOut"
        @click="handleLogout"
      >
        退出登录
      </button>
      <button v-else class="login-btn" @click="goLogin">登录账号</button>
    </view>

    <AssistantFloat ref="assistantRef" />
    <AppTabBar />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '../../components/AppTabBar.vue'
import { logout } from '../../api/authApi'
import {
  clearAuthSession,
  getAuthUser,
  isLoggedIn,
  requireLogin
} from '../../utils/auth'

const authUser = ref(getAuthUser())
const loggedIn = ref(isLoggedIn())
const loggingOut = ref(false)
const assistantRef = ref(null)

const userName = computed(() => authUser.value?.realName || authUser.value?.username || '未登录')
const accountText = computed(() => authUser.value?.username || '-')
const profileStatusText = computed(() => loggedIn.value ? '已登录 · 采集员' : '未登录 · 采集员')

onShow(() => {
  uni.hideTabBar({ animation: false })
  refreshState()
})

function refreshState() {
  authUser.value = getAuthUser()
  loggedIn.value = isLoggedIn()
}

async function handleLogout() {
  if (loggingOut.value) {
    return
  }

  loggingOut.value = true
  try {
    await logout()
  } catch (error) {
    console.warn('退出登录接口调用失败，本地会话仍会清理', error)
  } finally {
    clearAuthSession()
    loggingOut.value = false
    refreshState()
    uni.reLaunch({
      url: '/pages/login/index'
    })
  }
}

function goLogin() {
  uni.navigateTo({
    url: '/pages/login/index?redirect=/pages/mine/index'
  })
}

function goTaskList() {
  if (!requireLogin()) {
    return
  }

  uni.switchTab({
    url: '/pages/task/list'
  })
}

function goBatchList() {
  if (!requireLogin()) {
    return
  }

  uni.navigateTo({
    url: '/pages/batch/list'
  })
}

function openAssistant() {
  if (!requireLogin()) {
    return
  }
  assistantRef.value?.open()
}

function showHelp() {
  uni.showToast({
    title: '请通过 AI 小助手咨询使用流程',
    icon: 'none'
  })
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  padding: 24rpx 24rpx calc(150rpx + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, #fbf7ef 0%, #f7f1e6 100%);
}

.user-profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 24rpx;
  padding: 34rpx 30rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #0f5132 0%, #166534 100%);
  box-shadow: 0 14rpx 34rpx rgba(15, 81, 50, 0.22);
  color: #fffaf2;
}

.profile-avatar {
  display: flex;
  width: 104rpx;
  height: 104rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fffaf2;
  color: #0f5132;
  font-size: 44rpx;
  font-weight: 700;
}

.profile-content {
  min-width: 0;
  flex: 1;
}

.profile-name,
.profile-sub,
.profile-account {
  display: block;
}

.profile-name {
  color: #fffaf2;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.profile-sub {
  margin-top: 8rpx;
  color: rgba(255, 250, 242, 0.82);
  font-size: 26rpx;
}

.profile-account {
  margin-top: 8rpx;
  color: rgba(255, 250, 242, 0.66);
  font-size: 24rpx;
  overflow-wrap: anywhere;
}

.mine-card {
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 28rpx;
  background: #fffaf2;
  background: rgba(255, 250, 242, 0.96);
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.section-title {
  display: block;
  color: #0f3d2e;
  font-size: 31rpx;
  font-weight: 700;
}

.section-tip {
  color: #8b7e6b;
  font-size: 23rpx;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.action-item {
  display: flex;
  min-width: 0;
  min-height: 144rpx;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  box-sizing: border-box;
  margin: 0;
  padding: 20rpx;
  border: 1rpx solid #b7d7c2;
  border-radius: 22rpx;
  background: #eaf5ee;
  color: #0f3d2e;
  line-height: 1.2;
  text-align: left;
}

.action-item::after,
.logout-btn::after,
.login-btn::after {
  border: 0;
}

.action-icon {
  display: flex;
  width: 46rpx;
  height: 46rpx;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
  border-radius: 50%;
  background: #0f5132;
  color: #fffaf2;
  font-size: 21rpx;
  font-weight: 700;
  line-height: 1;
}

.action-title,
.action-desc {
  display: block;
  max-width: 100%;
}

.action-title {
  color: #0f3d2e;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.3;
}

.action-desc {
  margin-top: 7rpx;
  color: #7c6f5c;
  font-size: 23rpx;
  line-height: 1.4;
}

.info-card .section-title,
.system-card .section-title {
  margin-bottom: 16rpx;
}

.account-row {
  display: flex;
  min-height: 72rpx;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  border-bottom: 1rpx solid #eadfcd;
}

.account-row:last-child {
  border-bottom: 0;
}

.account-label {
  flex-shrink: 0;
  color: #7c6f5c;
  font-size: 26rpx;
}

.account-value {
  min-width: 0;
  color: #1f2933;
  font-size: 27rpx;
  font-weight: 500;
  text-align: right;
  overflow-wrap: anywhere;
}

.status-value {
  color: #0f5132;
}

.system-card {
  position: relative;
  overflow: hidden;
}

.system-name,
.system-desc {
  display: block;
}

.system-name {
  color: #0f5132;
  font-size: 27rpx;
  font-weight: 600;
}

.system-desc {
  margin-top: 12rpx;
  color: #7c6f5c;
  font-size: 25rpx;
  line-height: 1.65;
}

.logout-card {
  margin-bottom: 40rpx;
  padding: 4rpx 18rpx 0;
}

.logout-btn,
.login-btn {
  display: flex;
  width: 100%;
  height: 84rpx;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  margin: 0;
  border-radius: 18rpx;
  font-size: 28rpx;
  font-weight: 600;
  line-height: 84rpx;
}

.logout-btn {
  border: 1rpx solid #fecdd3;
  background: #fff1f2;
  color: #be123c;
}

.login-btn {
  border: 1rpx solid #b7d7c2;
  background: #eaf5ee;
  color: #0f5132;
}
</style>
