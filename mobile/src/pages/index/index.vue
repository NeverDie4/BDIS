<template>
  <view class="page home-page tab-page">
    <view class="card hero-card">
      <text class="hero-kicker">本草研究院标本馆移动采集端</text>
      <text class="title">采集员工作台</text>
      <text class="sub-title">查看采集任务，创建批次并上传现场采集图片。</text>
    </view>

    <view class="card action-card">
      <text class="section-title">常用功能</text>
      <view class="action-grid">
        <button class="action-item action-primary" @click="goTaskList">
          <text class="action-mark">任</text>
          <text class="action-name">我的采集任务</text>
          <text class="action-tip">查看已分配任务</text>
        </button>
        <button class="action-item" @click="goBatchList">
          <text class="action-mark">批</text>
          <text class="action-name">采集批次</text>
          <text class="action-tip">管理现场批次</text>
        </button>
      </view>
    </view>

    <view class="card process-card">
      <text class="section-title">移动采集流程</text>
      <view class="process-line">
        <text v-for="(item, index) in processSteps" :key="item" class="process-step">
          <text class="process-index">{{ index + 1 }}</text>
          <text class="process-text">{{ item }}</text>
        </text>
      </view>
    </view>
    <view class="bottom-breathing-space" />
    <AssistantFloat />
    <AppTabBar />
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '../../components/AppTabBar.vue'
import { requireLogin } from '../../utils/auth'

const processSteps = ['查看任务', '新建批次', '上传图片', '识别汇总', '提交批次']

onShow(() => {
  uni.hideTabBar({ animation: false })
})

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

</script>

<style scoped>
.tab-page {
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 48rpx 34rpx;
  border-color: #c9ddce;
  background: #fffaf2;
}

.hero-card::after {
  position: absolute;
  right: -34rpx;
  bottom: -54rpx;
  width: 190rpx;
  height: 190rpx;
  border: 30rpx solid rgba(22, 101, 52, 0.06);
  border-radius: 50%;
  content: '';
}

.hero-card > text {
  position: relative;
  z-index: 1;
  display: block;
}

.hero-kicker {
  display: block;
  margin-bottom: 16rpx;
  color: #8a5a2b;
  font-size: 24rpx;
  font-weight: 600;
}

.action-card {
  padding: 30rpx 28rpx;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 26rpx;
}

.action-item {
  display: flex;
  width: 100%;
  height: 176rpx;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  box-sizing: border-box;
  margin: 0;
  padding: 22rpx;
  border: 1rpx solid #b7d7c2;
  border-radius: 16rpx;
  background: #eaf5ee;
  color: #0f5132;
  text-align: left;
}

.action-item::after {
  border: 0;
}

.action-primary {
  background: #166534;
  color: #ffffff;
}

.action-mark {
  display: flex;
  width: 52rpx;
  height: 52rpx;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  color: #0f5132;
  font-size: 24rpx;
  font-weight: 700;
}

.action-name {
  margin-top: 14rpx;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.35;
}

.action-tip {
  margin-top: 6rpx;
  color: inherit;
  font-size: 23rpx;
  opacity: 0.72;
}

.process-card {
  margin-bottom: 12rpx;
  background: #fffaf2;
}

.bottom-breathing-space {
  height: 12rpx;
}

.process-line {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx 12rpx;
  margin-top: 26rpx;
}

.process-step {
  display: inline-flex;
  align-items: center;
  color: #5f5548;
  font-size: 25rpx;
}

.process-index {
  display: inline-flex;
  width: 38rpx;
  height: 38rpx;
  align-items: center;
  justify-content: center;
  margin-right: 8rpx;
  border-radius: 50%;
  background: #0f5132;
  color: #ffffff;
  font-size: 21rpx;
  font-weight: 700;
}
</style>
