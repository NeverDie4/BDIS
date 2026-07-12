import { request } from './request'

export function login(data) {
  return request({
    url: '/api/auth/sessions',
    method: 'POST',
    data,
    skipAuthRedirect: true
  })
}

export function getCurrentUser() {
  return request({
    url: '/api/auth/me',
    method: 'GET'
  })
}

export function logout() {
  return request({
    url: '/api/auth/sessions/current',
    method: 'DELETE',
    skipAuthRedirect: true
  })
}
