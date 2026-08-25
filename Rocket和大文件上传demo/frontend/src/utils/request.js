import axios from 'axios'

/**
 * axios 实例封装
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 请求拦截器
service.interceptors.request.use(
  config => config,
  error => Promise.reject(error)
)

// 响应拦截器: 统一处理 Result 结构
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    return Promise.reject(error)
  }
)

export default service
