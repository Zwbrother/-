import axios from 'axios'
import { message } from 'antd'
import { useAuthStore } from './store.js'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resp) => {
    const data = resp.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) return data.data
      if (data.code === 401) {
        useAuthStore.getState().logout()
        message.error('登录已过期，请重新登录')
        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(new Error(data.message))
      }
      message.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络异常'
    message.error(msg)
    return Promise.reject(err)
  }
)

export default api
