import request from './request'

/**
 * 大文件分片上传接口
 */
export const uploadApi = {
  // 上传单个分片
  uploadChunk (formData, onUploadProgress) {
    return request.post('/upload/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    })
  },
  // 查询上传进度(断点续传)
  check (params) {
    return request.get('/upload/check', { params })
  },
  // 合并分片
  merge (params) {
    return request.post('/upload/merge', null, { params })
  }
}
