import { getStorage, removeStorage, setStorage } from './storage'
import { getAuthUser } from './auth'

const COLLECTOR_ID_KEY = 'collectorId'
const COLLECTOR_NAME_KEY = 'collectorName'
const LEGACY_COLLECTOR_KEY = 'currentCollector'

const DEFAULT_COLLECTOR = {
  collectorId: 1001,
  collectorName: '采集员A'
}

export function getCurrentCollector() {
  const authUser = getAuthUser()
  if (authUser?.userId) {
    return {
      collectorId: Number(authUser.userId),
      collectorName: authUser.realName || authUser.username || DEFAULT_COLLECTOR.collectorName
    }
  }

  const legacyCollector = getStorage(LEGACY_COLLECTOR_KEY)
  const collectorId = getStorage(COLLECTOR_ID_KEY, legacyCollector?.collectorId || DEFAULT_COLLECTOR.collectorId)
  const collectorName = getStorage(COLLECTOR_NAME_KEY, legacyCollector?.collectorName || DEFAULT_COLLECTOR.collectorName)

  return {
    collectorId: Number(collectorId) || DEFAULT_COLLECTOR.collectorId,
    collectorName: collectorName || DEFAULT_COLLECTOR.collectorName
  }
}

export function setCurrentCollector(collector) {
  const nextCollector = {
    collectorId: Number(collector.collectorId) || DEFAULT_COLLECTOR.collectorId,
    collectorName: collector.collectorName || DEFAULT_COLLECTOR.collectorName
  }

  setStorage(COLLECTOR_ID_KEY, nextCollector.collectorId)
  setStorage(COLLECTOR_NAME_KEY, nextCollector.collectorName)
  removeStorage(LEGACY_COLLECTOR_KEY)

  return nextCollector
}

export function clearCurrentCollector() {
  removeStorage(COLLECTOR_ID_KEY)
  removeStorage(COLLECTOR_NAME_KEY)
  removeStorage(LEGACY_COLLECTOR_KEY)
}

export function getDefaultCollector() {
  return { ...DEFAULT_COLLECTOR }
}
