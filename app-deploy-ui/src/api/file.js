import request from './request'

/**
 * 列出目录内容
 */
export function listFiles(params) {
  return request.get('/file/list', { params })
}

/**
 * 上传文件
 */
export function uploadFile(serverId, remotePath, file) {
  const formData = new FormData()
  formData.append('serverId', serverId)
  formData.append('remotePath', remotePath)
  formData.append('file', file)

  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 批量上传文件（支持文件夹）
 */
export function uploadBatch(serverId, remotePath, files, paths, onProgress) {
  const formData = new FormData()
  formData.append('serverId', serverId)
  formData.append('remotePath', remotePath)
  
  // 添加所有文件
  files.forEach(file => {
    formData.append('files', file)
  })
  
  // 添加所有路径
  paths.forEach(path => {
    formData.append('paths', path)
  })

  return request.post('/file/upload-batch', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000, // 10分钟超时，适用于大文件夹
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percentCompleted, progressEvent.loaded, progressEvent.total)
      }
    }
  })
}

/**
 * 下载文件
 */
export function downloadFile(serverId, filePath, onProgress) {
  return request.get('/file/download', {
    params: { serverId, filePath },
    responseType: 'blob',
    timeout: 300000, // 5分钟超时，适用于大文件
    onDownloadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percentCompleted, progressEvent.loaded, progressEvent.total)
      }
    }
  })
}

/**
 * 删除文件或目录
 */
export function deleteFile(serverId, filePath) {
  return request.delete('/file/delete', {
    params: { serverId, filePath }
  })
}

/**
 * 创建目录
 */
export function createDirectory(params) {
  return request.post('/file/mkdir', null, { params })
}
