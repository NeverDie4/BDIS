import config from '../config'
import { request, uploadFile } from './request'

const prefix = config.mobilePrefix

export function getBatchDetail(batchId, params) {
  return request({
    url: `${prefix}/batches/${batchId}`,
    method: 'GET',
    data: params
  })
}

export function uploadBatchImage(batchId, filePath, formData, options = {}) {
  return uploadFile({
    url: `${prefix}/batches/${batchId}/images/upload`,
    filePath,
    formData,
    ...options
  })
}

export function identifyBatchImage(batchId, imageId, data = {}) {
  return request({
    url: `${prefix}/batches/${batchId}/images/${imageId}/identify`,
    method: 'POST',
    data,
    timeout: 180000
  })
}

export function identifyMissingImages(batchId) {
  return request({
    url: `${prefix}/batches/${batchId}/identify-missing-images`,
    method: 'POST',
    timeout: 180000
  })
}

export function refreshBatchSummary(batchId) {
  return request({
    url: `${prefix}/batches/${batchId}/summary/refresh`,
    method: 'POST'
  })
}

export function submitBatch(batchId, data) {
  return request({
    url: `${prefix}/batches/${batchId}/submit`,
    method: 'PUT',
    data
  })
}
