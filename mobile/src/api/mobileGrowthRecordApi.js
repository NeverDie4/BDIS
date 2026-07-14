import { request } from './request'

export function getBatchGrowthRecord(batchId) {
  return request({
    url: `/api/herb/batch/${batchId}/growth-record`,
    method: 'GET',
    showErrorToast: false
  })
}

export function createBatchGrowthRecord(batchId, data) {
  return request({
    url: `/api/herb/batch/${batchId}/growth-record`,
    method: 'POST',
    data
  })
}

export function updateBatchGrowthRecord(batchId, recordId, data) {
  return request({
    url: `/api/herb/batch/${batchId}/growth-record/${recordId}`,
    method: 'PUT',
    data
  })
}

export function submitGrowthRecord(recordId) {
  return request({
    url: `/api/growth-records/${recordId}/submit`,
    method: 'PUT'
  })
}

export function getGrowthRecordDetail(recordId) {
  return request({
    url: `/api/growth-records/${recordId}`,
    method: 'GET'
  })
}
