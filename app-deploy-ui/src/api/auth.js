import request from './request'

// 登录
export const login = (data) => {
  return request.post('/auth/login', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get('/auth/user-info')
}

// 退出登录
export const logout = () => {
  return request.post('/auth/logout')
}
