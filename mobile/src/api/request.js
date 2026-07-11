import config from '../config'

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

function handleBusinessResponse(responseData) {
  const result = responseData || {}

  if (result.code === 200) {
    return {
      ok: true,
      data: result.data
    }
  }

  showError(result.msg || '操作失败')
  return {
    ok: false,
    error: result
  }
}

export function request(options = {}) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    loading = false
  } = options
  const upperMethod = method.toUpperCase()

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
      timeout: config.timeout,
      header: {
        'Content-Type': 'application/json; charset=utf-8',
        ...header
      },
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          const error = {
            code: res.statusCode,
            msg: `请求失败：${res.statusCode}`,
            data: res.data
          }
          showError(error.msg)
          reject(error)
          return
        }

        const result = handleBusinessResponse(res.data)
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

export function uploadFile(options = {}) {
  const {
    url,
    filePath,
    formData = {},
    header = {},
    loading = false,
    timeout = config.timeout
  } = options

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
      header,
      success: (res) => {
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

        const result = handleBusinessResponse(parseUploadResponse(res.data))
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
