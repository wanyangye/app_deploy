import axios from 'axios'
import router from '../router'
import { message } from '@/utils/message'

// 防止重复跳转登录页面
let isRedirectingToLogin = false

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    const tokenName = localStorage.getItem('tokenName') || 'Authorization'
    if (token) {
      config.headers[tokenName] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 如果是 blob 类型的响应（文件下载），直接返回
    if (response.config.responseType === 'blob') {
      // 检查是否是错误响应（如果后端返回的是 JSON 错误信息）
      if (response.data.type === 'application/json') {
        return new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => {
            try {
              const errorData = JSON.parse(reader.result)
              message.error(errorData.message || '下载失败')
              reject(new Error(errorData.message || '下载失败'))
            } catch (e) {
              reject(new Error('下载失败'))
            }
          }
          reader.readAsText(response.data)
        })
      }
      return response.data
    }
    
    const res = response.data
    
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401 || res.code === 403) {
      // token 无效或过期
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        message.error(res.message || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('tokenName')
        localStorage.removeItem('user')
        router.push('/login').finally(() => {
          // 重置标志，允许下次跳转
          setTimeout(() => {
            isRedirectingToLogin = false
          }, 1000)
        })
      }
      return Promise.reject(new Error(res.message || 'Token无效'))
    } else if (res.code === 500 && res.message && (res.message.includes('token') || res.message.includes('无效'))) {
      // token 无效（后端返回 500 但消息包含 token 关键字）
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        message.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('tokenName')
        localStorage.removeItem('user')
        router.push('/login').finally(() => {
          setTimeout(() => {
            isRedirectingToLogin = false
          }, 1000)
        })
      }
      return Promise.reject(new Error('Token无效'))
    } else {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      
      // 处理 blob 类型错误响应
      if (error.config.responseType === 'blob' && data instanceof Blob) {
        // 从响应头获取错误信息
        const errorMessage = error.response.headers['x-error-message']
        if (errorMessage) {
          message.error(decodeURIComponent(errorMessage))
        } else {
          message.error('下载文件失败')
        }
        return Promise.reject(new Error(errorMessage || '下载文件失败'))
      }
      
      if (status === 401 || status === 403) {
        // token 无效或过期
        if (!isRedirectingToLogin) {
          isRedirectingToLogin = true
          message.error(data?.message || '登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('tokenName')
          localStorage.removeItem('user')
          router.push('/login').finally(() => {
            setTimeout(() => {
              isRedirectingToLogin = false
            }, 1000)
          })
        }
      } else if (status === 500) {
        // 检查是否是 token 相关错误
        if (data?.message && (data.message.includes('token') || data.message.includes('无效'))) {
          if (!isRedirectingToLogin) {
            isRedirectingToLogin = true
            message.error('登录已过期，请重新登录')
            localStorage.removeItem('token')
            localStorage.removeItem('tokenName')
            localStorage.removeItem('user')
            router.push('/login').finally(() => {
              setTimeout(() => {
                isRedirectingToLogin = false
              }, 1000)
            })
          }
        } else {
          message.error(data?.message || '服务器错误')
        }
      } else {
        message.error(data?.message || error.message || '网络错误')
      }
    } else {
      message.error(error.message || '网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
