<template>
  <view class="custom-tabbar">
    <view
      v-for="item in tabs"
      :key="item.path"
      class="tab-item"
      :class="{ active: activePath === item.path }"
      @tap="switchTab(item.path)"
    >
      <view class="tab-icon-wrap">
        <text class="tab-icon-text">{{ item.icon }}</text>
      </view>
      <text class="tab-label">{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const TABBAR_SYNC_EVENT = 'bdis-tabbar-sync'
const tabs = [
  { path: '/pages/index/index', label: '首页', icon: '馆' },
  { path: '/pages/task/list', label: '任务', icon: '任' },
  { path: '/pages/mine/index', label: '我的', icon: '我' }
]
const activePath = ref('')

onMounted(() => {
  syncActivePath()
  uni.$on(TABBAR_SYNC_EVENT, syncActivePath)
})

onUnmounted(() => {
  uni.$off(TABBAR_SYNC_EVENT, syncActivePath)
})

function syncActivePath() {
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route
  activePath.value = route ? `/${route}` : tabs[0].path
}

function switchTab(path) {
  if (activePath.value === path) {
    return
  }

  activePath.value = path
  uni.switchTab({
    url: path,
    complete: () => uni.$emit(TABBAR_SYNC_EVENT)
  })
}
</script>

<style scoped>
.custom-tabbar {
  position: fixed;
  z-index: 9000;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  height: 112rpx;
  align-items: center;
  justify-content: space-around;
  box-sizing: content-box;
  padding-bottom: env(safe-area-inset-bottom);
  border-top: 1rpx solid #eadfcd;
  background: #fffaf2;
  background: rgba(255, 250, 242, 0.97);
  box-shadow: 0 -8rpx 24rpx rgba(63, 45, 24, 0.08);
  backdrop-filter: blur(14rpx);
}

.tab-item {
  display: flex;
  height: 96rpx;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5rpx;
  color: #7c6f5c;
}

.tab-icon-wrap {
  display: flex;
  width: 58rpx;
  height: 48rpx;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border: 1rpx solid transparent;
  border-radius: 999rpx;
}

.tab-icon-text {
  color: inherit;
  font-size: 27rpx;
  font-weight: 700;
  line-height: 1;
}

.tab-label {
  color: inherit;
  font-size: 23rpx;
  line-height: 1;
}

.tab-item.active {
  color: #0f5132;
  font-weight: 600;
}

.tab-item.active .tab-icon-wrap {
  border-color: #b7d7c2;
  background: #eaf5ee;
  box-shadow: 0 4rpx 12rpx rgba(15, 81, 50, 0.1);
}
</style>
