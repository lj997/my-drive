import request from '@/utils/request'

export function createShare(data) {
  return request({
    url: '/share/create',
    method: 'post',
    data
  })
}

export function getShareInfo(data) {
  return request({
    url: '/share/info',
    method: 'post',
    data
  })
}

export function listShares() {
  return request({
    url: '/share/list',
    method: 'get'
  })
}

export function cancelShare(shareId) {
  return request({
    url: `/share/cancel/${shareId}`,
    method: 'post'
  })
}

export function getShareDownloadUrl(shareCode, password) {
  let url = `/api/share/download?shareCode=${encodeURIComponent(shareCode)}`
  if (password) {
    url += `&password=${encodeURIComponent(password)}`
  }
  return url
}

export function getSharePreviewUrl(shareCode, password) {
  let url = `/api/share/preview?shareCode=${encodeURIComponent(shareCode)}`
  if (password) {
    url += `&password=${encodeURIComponent(password)}`
  }
  return url
}
