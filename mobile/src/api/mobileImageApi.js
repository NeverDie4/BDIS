import config from '../config'
import { request } from './request'

const prefix = config.mobilePrefix

export function getLatestIdentification(imageId) {
  return request({
    url: `${prefix}/images/${imageId}/identification/latest`,
    method: 'GET'
  })
}
