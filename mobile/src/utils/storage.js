export function setStorage(key, value) {
  uni.setStorageSync(key, value)
}

export function getStorage(key, defaultValue = null) {
  const value = uni.getStorageSync(key)
  return value === '' || value === undefined || value === null ? defaultValue : value
}

export function removeStorage(key) {
  uni.removeStorageSync(key)
}

export function clearStorage() {
  uni.clearStorageSync()
}
