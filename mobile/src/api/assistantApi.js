import { request } from './request'

const prefix = '/api/herb/assistant'

export function chatWithAssistant(data) {
  return request({
    url: `${prefix}/chat`,
    method: 'POST',
    data,
    timeout: 120000
  })
}

export function getAssistantMessages(sessionId) {
  return request({
    url: `${prefix}/sessions/${sessionId}/messages`,
    method: 'GET'
  })
}

export function getAssistantSessions(data = {}) {
  return request({
    url: `${prefix}/sessions`,
    method: 'GET',
    data
  })
}

export function explainBatch(batchId, data = {}) {
  return request({
    url: `${prefix}/batch/${batchId}/explain`,
    method: 'POST',
    data,
    timeout: 120000
  })
}

export function explainImage(imageId, data = {}) {
  return request({
    url: `${prefix}/image/${imageId}/explain`,
    method: 'POST',
    data,
    timeout: 120000
  })
}
