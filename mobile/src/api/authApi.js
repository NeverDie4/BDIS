import { request } from './request'

export function login(data) {
  return request({
    url: '/api/auth/sessions',
    method: 'POST',
    data,
    skipAuthRedirect: true
  })
}

export function getCurrentUser(options = {}) {
  return request({
    url: '/api/auth/me',
    method: 'GET',
    skipAuthRedirect: Boolean(options.skipAuthRedirect)
  })
}

export function logout() {
  return request({
    url: '/api/auth/sessions/current',
    method: 'DELETE',
    skipAuthRedirect: true
  })
}
