import { getStorage, removeStorage, setStorage } from './storage'

const TOKEN_KEY = 'authToken'
const TOKEN_TYPE_KEY = 'authTokenType'
const USER_KEY = 'authUser'

export function setAuthSession(session = {}) {
  const token = session.accessToken || session.token || ''
  const tokenType = session.tokenType || 'Bearer'

  if (!token) {
    clearAuthSession()
    return null
  }

  setStorage(TOKEN_KEY, token)
  setStorage(TOKEN_TYPE_KEY, tokenType)
  setStorage(USER_KEY, session.user || null)
  return {
    token,
    tokenType,
    user: session.user || null
  }
}

export function getAuthToken() {
  return getStorage(TOKEN_KEY, '')
}

export function getAuthHeader() {
  const token = getAuthToken()
  if (!token) {
    return ''
  }

  const tokenType = getStorage(TOKEN_TYPE_KEY, 'Bearer')
  return `${tokenType} ${token}`
}

export function getAuthUser() {
  return getStorage(USER_KEY, null)
}

export function isLoggedIn() {
  return Boolean(getAuthToken())
}

export function clearAuthSession() {
  removeStorage(TOKEN_KEY)
  removeStorage(TOKEN_TYPE_KEY)
  removeStorage(USER_KEY)
}

export function redirectToLogin() {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const currentPath = currentPage?.route ? `/${currentPage.route}` : ''

  if (currentPath === '/pages/login/index') {
    return
  }

  uni.navigateTo({
    url: `/pages/login/index?redirect=${encodeURIComponent(currentPath || '/pages/index/index')}`
  })
}

export function requireLogin() {
  if (isLoggedIn()) {
    return true
  }

  redirectToLogin()
  return false
}
