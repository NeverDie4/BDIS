import config from '../config'
import { request } from './request'

const prefix = config.mobilePrefix

export function getMyTasks(params) {
  return request({
    url: `${prefix}/tasks`,
    method: 'GET',
    data: params
  })
}

export function getTaskDetail(taskId, params) {
  return request({
    url: `${prefix}/tasks/${taskId}`,
    method: 'GET',
    data: params
  })
}

export function getTaskBatches(taskId, params) {
  return request({
    url: `${prefix}/tasks/${taskId}/batches`,
    method: 'GET',
    data: params
  })
}

export function getTaskAgentRequirements(taskId) {
  return request({
    url: `${prefix}/tasks/${taskId}/agent-requirements`,
    method: 'GET'
  })
}

export function createBatchUnderTask(taskId, data) {
  return request({
    url: `${prefix}/tasks/${taskId}/batches`,
    method: 'POST',
    data
  })
}
