import config from '../config'
import {
  clearAuthSession,
  getAuthHeader,
  getRefreshToken,
  redirectToLogin,
  setAuthSession
} from '../utils/auth'

let refreshPromise = null

function buildUrl(url, data = {}, method = 'GET') {
  const isAbsoluteUrl = /^https?:\/\//i.test(url)
  const targetUrl = isAbsoluteUrl ? url : `${config.baseUrl}${url}`
  const upperMethod = method.toUpperCase()

  if (upperMethod !== 'GET' || !data || Object.keys(data).length === 0) {
    return targetUrl
  }

  const query = Object.keys(data)
    .filter((key) => data[key] !== undefined && data[key] !== null && data[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
    .join('&')

  if (!query) {
    return targetUrl
  }

  return `${targetUrl}${targetUrl.includes('?') ? '&' : '?'}${query}`
}

function showError(message) {
  uni.showToast({
    title: message || '请求失败',
    icon: 'none',
    duration: 2200
  })
}

function isUnauthorized(result, statusCode) {
  return statusCode === 401 || result?.code === 401 || result?.code === 'UNAUTHORIZED'
}

function isSuccessCode(code) {
  return code === 200 || code === '200' || code === 'SUCCESS'
}

function getResultMessage(result, fallback) {
  return result?.msg || result?.message || fallback
}

function parseUploadResponse(data) {
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch (error) {
      return {
        code: 500,
        msg: '上传结果解析失败',
        data: null
      }
    }
  }

  return data
}

function handleBusinessResponse(responseData, showErrorToast = true) {
  const result = responseData || {}

  if (isSuccessCode(result.code)) {
    return {
      ok: true,
      data: result.data
    }
  }

  if (showErrorToast) {
    showError(getResultMessage(result, '操作失败'))
  }
  return {
    ok: false,
    error: result
  }
}

function rawRefreshSession() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject({ code: 'UNAUTHORIZED', msg: '缺少刷新令牌' })
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: buildUrl('/api/auth/sessions/refresh'),
      method: 'POST',
      data: { refreshToken },
      timeout: config.timeout,
      header: {
        'Content-Type': 'application/json; charset=utf-8'
      },
      success: (res) => {
        if (isUnauthorized(res.data || {}, res.statusCode)) {
          reject(res.data || { code: 'UNAUTHORIZED', msg: '登录已过期，请重新登录' })
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject({
            code: res.statusCode,
            msg: `刷新登录状态失败：${res.statusCode}`,
            data: res.data
          })
          return
        }
        const result = handleBusinessResponse(res.data, false)
        if (result.ok) {
          setAuthSession(result.data)
          resolve(result.data)
        } else {
          reject(result.error)
        }
      },
      fail: reject
    })
  })
}

function refreshSessionOnce() {
  if (!refreshPromise) {
    refreshPromise = rawRefreshSession().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

function rejectUnauthorized(error, skipAuthRedirect, reject) {
  clearAuthSession()
  if (!skipAuthRedirect) {
    redirectToLogin()
  }
  reject(error || { code: 'UNAUTHORIZED', msg: '未登录或登录已失效' })
}

function shouldRefreshAccessToken(options) {
  const url = String(options.url || '')
  return !url.includes('/auth/sessions') && !url.includes('/auth/bootstrap-admin')
}

function handleUnauthorized(options, retry, fallbackError, resolve, reject) {
  if (options._authRetry || !getRefreshToken() || !shouldRefreshAccessToken(options)) {
    rejectUnauthorized(fallbackError, options.skipAuthRedirect, reject)
    return
  }

  refreshSessionOnce()
    .then(() => retry({ ...options, _authRetry: true }).then(resolve, reject))
    .catch((refreshError) => {
      if (isUnauthorized(refreshError || {}, refreshError?.code)) {
        rejectUnauthorized(refreshError, options.skipAuthRedirect, reject)
        return
      }
      reject(refreshError)
    })
}

export function request(options = {}) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    loading = false,
    showErrorToast = true,
    timeout = config.timeout
  } = options
  const upperMethod = method.toUpperCase()
  const authHeader = getAuthHeader()

  if (loading) {
    uni.showLoading({
      title: '加载中...'
    })
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: buildUrl(url, data, upperMethod),
      method: upperMethod,
      data: upperMethod === 'GET' ? {} : data,
      timeout,
      header: {
        'Content-Type': 'application/json; charset=utf-8',
        ...(authHeader ? { Authorization: authHeader } : {}),
        ...header
      },
      success: (res) => {
        if (isUnauthorized(res.data || {}, res.statusCode)) {
          handleUnauthorized(
            options,
            request,
            res.data || { code: 'UNAUTHORIZED', msg: '未登录或登录已失效' },
            resolve,
            reject
          )
          return
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          const error = {
            code: res.statusCode,
            msg: `请求失败：${res.statusCode}`,
            data: res.data
          }
          if (showErrorToast) {
            showError(error.msg)
          }
          reject(error)
          return
        }

        const result = handleBusinessResponse(res.data, showErrorToast)
        if (result.ok) {
          resolve(result.data)
        } else {
          reject(result.error)
        }
      },
      fail: (error) => {
        if (showErrorToast) {
          showError('网络异常，请检查后端服务是否启动')
        }
        reject(error)
      },
      complete: () => {
        if (loading) {
          uni.hideLoading()
        }
      }
    })
  })
}

export function uploadFile(options = {}) {
  const {
    url,
    filePath,
    formData = {},
    header = {},
    loading = false,
    timeout = config.timeout
  } = options
  const authHeader = getAuthHeader()

  if (loading) {
    uni.showLoading({
      title: '加载中...'
    })
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: buildUrl(url),
      filePath,
      name: 'file',
      formData,
      timeout,
      header: {
        ...(authHeader ? { Authorization: authHeader } : {}),
        ...header
      },
      success: (res) => {
        const responseData = parseUploadResponse(res.data)
        if (isUnauthorized(responseData || {}, res.statusCode)) {
          handleUnauthorized(
            options,
            uploadFile,
            responseData || { code: 'UNAUTHORIZED', msg: '未登录或登录已失效' },
            resolve,
            reject
          )
          return
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          const error = {
            code: res.statusCode,
            msg: `上传失败：${res.statusCode}`,
            data: res.data
          }
          showError(error.msg)
          reject(error)
          return
        }

        const result = handleBusinessResponse(responseData)
        if (result.ok) {
          resolve(result.data)
        } else {
          reject(result.error)
        }
      },
      fail: (error) => {
        showError('网络异常，请检查后端服务是否启动')
        reject(error)
      },
      complete: () => {
        if (loading) {
          uni.hideLoading()
        }
      }
    })
  })
}
