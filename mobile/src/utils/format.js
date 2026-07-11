import config from '../config'

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const pad = (num) => String(num).padStart(2, '0')
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hour = pad(date.getHours())
  const minute = pad(date.getMinutes())
  const second = pad(date.getSeconds())

  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export function formatStatus(value, map) {
  if (!value) {
    return '-'
  }

  return map && map[value] ? map[value] : value
}

export function formatPercent(value) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) {
    return value
  }

  return `${(numberValue * 100).toFixed(2)}%`
}

export function formatScore(value) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) {
    return value
  }

  return numberValue.toFixed(2)
}

export function getNowDateTime() {
  const date = new Date()
  const pad = (num) => String(num).padStart(2, '0')
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hour = pad(date.getHours())
  const minute = pad(date.getMinutes())
  const second = pad(date.getSeconds())

  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export function resolveFileUrl(url) {
  if (!url) {
    return ''
  }

  if (/^https?:\/\//i.test(url)) {
    return url
  }

  if (config.baseUrl && url.startsWith('/herb/')) {
    return `${config.baseUrl}/api${url}`
  }

  return `${config.baseUrl}${url.startsWith('/') ? '' : '/'}${url}`
}

export function normalizePageData(data) {
  if (Array.isArray(data)) {
    return {
      records: data,
      total: data.length
    }
  }

  const pageData = data || {}
  const records = pageData.records || pageData.list || pageData.rows || pageData.data || []

  return {
    records: Array.isArray(records) ? records : [],
    total: Number(pageData.total || 0)
  }
}
