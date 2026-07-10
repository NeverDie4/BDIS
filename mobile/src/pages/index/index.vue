<template>
  <view class="page home-page">
    <view class="card hero-card">
      <text class="title">生物医药数字信息系统</text>
      <text class="sub-title">中药材采集与图谱识别</text>
    </view>

    <view class="card">
      <button class="primary-btn" @click="goTaskList">我的采集任务</button>
      <button class="secondary-btn" @click="goBatchList">批次采集</button>
      <button class="secondary-btn" @click="goMine">我的</button>
    </view>
  </view>
</template>

<script setup>
function goTaskList() {
  uni.switchTab({
    url: '/pages/task/list'
  })
}

function goBatchList() {
  uni.navigateTo({
    url: '/pages/batch/list'
  })
}

function goMine() {
  uni.switchTab({
    url: '/pages/mine/index'
  })
}
</script>

<style scoped>
.hero-card {
  padding: 44rpx 32rpx;
}
</style>
