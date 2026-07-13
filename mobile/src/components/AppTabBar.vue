<template>
  <view class="app-tabbar-wrap">
    <view class="app-tabbar">
      <view
        v-for="item in tabs"
        :key="item.route"
        class="tab-item"
        :class="{ active: currentRoute === item.route }"
        @tap="handleSwitch(item)"
      >
        <view class="tab-icon">{{ item.iconText }}</view>
        <text class="tab-text">{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const ROUTE_SYNC_EVENT = 'app-tabbar-route-change'
const tabs = [
  { route: '/pages/index/index', label: '首页', iconText: '馆' },
  { route: '/pages/task/list', label: '任务', iconText: '任' },
  { route: '/pages/mine/index', label: '我的', iconText: '我' }
]
const currentRoute = ref('')

onMounted(() => {
  updateCurrentRoute()
  uni.$on(ROUTE_SYNC_EVENT, updateCurrentRoute)
})

onUnmounted(() => {
  uni.$off(ROUTE_SYNC_EVENT, updateCurrentRoute)
})

function updateCurrentRoute() {
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route
  currentRoute.value = route ? `/${route}` : tabs[0].route
}

function handleSwitch(item) {
  updateCurrentRoute()
  if (currentRoute.value === item.route) {
    return
  }

  uni.switchTab({
    url: item.route,
    success: () => uni.$emit(ROUTE_SYNC_EVENT)
  })
}
</script>

<style scoped>
.app-tabbar-wrap {
  position: fixed;
  z-index: 8000;
  right: 0;
  bottom: 0;
  left: 0;
  box-sizing: border-box;
  padding: 10rpx 28rpx calc(14rpx + env(safe-area-inset-bottom));
  background: linear-gradient(
    180deg,
    rgba(247, 241, 230, 0) 0%,
    rgba(247, 241, 230, 0.92) 42%,
    #f7f1e6 100%
  );
  pointer-events: none;
}

.app-tabbar {
  display: flex;
  height: 94rpx;
  align-items: center;
  justify-content: space-around;
  box-sizing: border-box;
  border: 1rpx solid #eadfcd;
  border-radius: 999rpx;
  background: #fffaf2;
  background: rgba(255, 250, 242, 0.96);
  box-shadow: 0 -2rpx 16rpx rgba(63, 45, 24, 0.05), 0 8rpx 22rpx rgba(63, 45, 24, 0.08);
  backdrop-filter: blur(10px);
  pointer-events: auto;
}

.tab-item {
  display: flex;
  height: 70rpx;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3rpx;
  box-sizing: border-box;
  margin: 0 10rpx;
  border: 1rpx solid transparent;
  border-radius: 999rpx;
  color: #7c6f5c;
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.tab-item.active {
  border-color: #b7d7c2;
  background: #eaf5ee;
  color: #0f5132;
  font-weight: 700;
}

.tab-icon {
  display: flex;
  width: 38rpx;
  height: 38rpx;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f4ead8;
  color: #7c6f5c;
  font-size: 20rpx;
  font-weight: 700;
  line-height: 1;
}

.tab-item.active .tab-icon {
  background: #0f5132;
  color: #fffaf2;
}

.tab-text {
  color: inherit;
  font-size: 22rpx;
  line-height: 1;
}
</style>
