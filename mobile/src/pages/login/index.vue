<template>
  <view class="login-page">
    <view class="brand">
      <text class="brand-mark">本</text>
      <view>
        <text class="brand-title">本草研究院标本馆</text>
        <text class="brand-subtitle">Herbarium Research Hall</text>
      </view>
    </view>

    <view class="panel">
      <text class="title">采集员登录</text>
      <text class="sub-title">使用采集员账号登录，查看并处理你的采集任务。</text>

      <view class="form-item">
        <text class="form-label">账号</text>
        <input
          v-model="form.username"
          class="form-input"
          placeholder="请输入账号"
          confirm-type="next"
        />
      </view>

      <view class="form-item">
        <text class="form-label">密码</text>
        <input
          v-model="form.password"
          class="form-input"
          password
          placeholder="请输入密码"
          confirm-type="done"
          @confirm="handleLogin"
        />
      </view>

      <button class="primary-btn login-btn" :loading="loading" @click="handleLogin">登录</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCurrentUser, login } from '../../api/authApi'
import { clearAuthSession, getAuthToken, isLoggedIn, setAuthSession } from '../../utils/auth'

const loading = ref(false)
const redirect = ref('/pages/index/index')
const form = reactive({
  username: '',
  password: ''
})

onLoad((options) => {
  if (options.redirect) {
    redirect.value = decodeURIComponent(options.redirect)
  }
  restoreExistingSession()
})

async function restoreExistingSession() {
  if (!isLoggedIn() || loading.value) {
    return
  }

  loading.value = true
  try {
    const user = await getCurrentUser({ skipAuthRedirect: true })
    if (isCollectorSession({ user })) {
      setAuthSession({
        accessToken: getAuthToken(),
        user
      })
      goRedirect()
      return
    }
    clearAuthSession()
  } catch (error) {
    clearAuthSession()
  } finally {
    loading.value = false
  }
}

async function handleLogin() {
  const username = form.username.trim()
  const password = form.password

  if (!username) {
    showToast('请输入账号')
    return
  }

  if (!password) {
    showToast('请输入密码')
    return
  }

  if (loading.value) {
    return
  }

  loading.value = true
  try {
    const session = await login({ username, password })
    if (!isCollectorSession(session)) {
      showToast('请使用采集员账号登录移动端')
      return
    }

    setAuthSession(session)
    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })
    goRedirect()
  } catch (error) {
    console.error('登录失败', error)
    showToast(error?.msg || error?.message || '账号或密码错误')
  } finally {
    loading.value = false
  }
}

function isCollectorSession(session) {
  const roleCodes = session?.user?.roleCodes || []
  return Array.isArray(roleCodes) && roleCodes.includes('COLLECTOR')
}

function goRedirect() {
  const target = redirect.value || '/pages/index/index'
  const tabPages = ['/pages/index/index', '/pages/task/list', '/pages/mine/index']

  if (tabPages.includes(target)) {
    uni.switchTab({ url: target })
    return
  }

  uni.redirectTo({ url: target })
}

function showToast(title) {
  uni.showToast({
    title,
    icon: 'none'
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 56rpx 34rpx;
  background: #f7f3ea;
}

.brand {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 52rpx;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
  border: 4rpx solid #1f6b4a;
  border-radius: 50%;
  color: #1f6b4a;
  font-size: 36rpx;
  font-weight: 700;
}

.brand-title {
  display: block;
  color: #173f30;
  font-size: 34rpx;
  font-weight: 700;
}

.brand-subtitle {
  display: block;
  margin-top: 4rpx;
  color: #6c756c;
  font-size: 22rpx;
}

.panel {
  padding: 38rpx 30rpx;
  border: 1rpx solid rgba(31, 107, 74, 0.12);
  border-radius: 18rpx;
  background: #fffaf1;
  box-shadow: 0 18rpx 50rpx rgba(62, 48, 24, 0.08);
}

.form-item {
  margin-top: 28rpx;
}

.form-label {
  display: block;
  margin-bottom: 10rpx;
  color: #526257;
  font-size: 26rpx;
}

.form-input {
  height: 84rpx;
  box-sizing: border-box;
  padding: 0 22rpx;
  border: 1rpx solid #e5dccb;
  border-radius: 12rpx;
  background: #ffffff;
  color: #14231b;
  font-size: 29rpx;
}

.login-btn {
  margin-top: 36rpx;
  background: #1f6b4a;
}
</style>
