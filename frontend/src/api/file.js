import request from '@/utils/request'
import axios from 'axios'

export function listFiles(parentId) {
  return request({
    url: '/file/list',
    method: 'get',
    params: { parentId }
  })
}

export function getBreadcrumb(fileId) {
  return request({
    url: '/file/breadcrumb',
    method: 'get',
    params: { fileId }
  })
}

export function createFolder(data) {
  return request({
    url: '/file/folder',
    method: 'post',
    data
  })
}

export function uploadFile(file, parentId, onUploadProgress) {
  const formData = new FormData()
  formData.append('file', file)
  if (parentId) {
    formData.append('parentId', parentId)
  }

  const token = localStorage.getItem('token')
  return axios.post('/api/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    onUploadProgress
  }).then(response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      return Promise.reject(new Error(res.message || '上传失败'))
    }
  })
}

export function renameFile(data) {
  return request({
    url: '/file/rename',
    method: 'post',
    data
  })
}

export function moveFiles(data) {
  return request({
    url: '/file/move',
    method: 'post',
    data
  })
}

export function moveToRecycleBin(fileIds) {
  return request({
    url: '/file/delete',
    method: 'post',
    data: fileIds
  })
}

export function listRecycleBin() {
  return request({
    url: '/file/recycle',
    method: 'get'
  })
}

export function restoreFromRecycleBin(fileIds) {
  return request({
    url: '/file/recycle/restore',
    method: 'post',
    data: fileIds
  })
}

export function permanentDelete(fileIds) {
  return request({
    url: '/file/recycle/delete',
    method: 'post',
    data: fileIds
  })
}

export function getDownloadUrl(fileId) {
  const token = localStorage.getItem('token')
  return `/api/file/download?fileId=${fileId}&token=${encodeURIComponent(token)}`
}

export function getPreviewUrl(fileId) {
  const token = localStorage.getItem('token')
  return `/api/file/preview?fileId=${fileId}&token=${encodeURIComponent(token)}`
}
