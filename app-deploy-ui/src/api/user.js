import request from './request'

// 用户列表
export const getUserList = (params) => {
  return request.get('/user/list', { params })
}

// 用户详情
export const getUser = (id) => {
  return request.get(`/user/${id}`)
}

// 添加用户
export const addUser = (data) => {
  return request.post('/user', data)
}

// 更新用户
export const updateUser = (data) => {
  return request.put('/user', data)
}

// 删除用户
export const deleteUser = (id) => {
  return request.delete(`/user/${id}`)
}

// 获取当前用户信息和权限
export const getCurrentUser = () => {
  return request.get('/user/current')
}

// 更新个人信息
export const updateProfile = (data) => {
  return request.put('/user/profile', data)
}

// 修改密码
export const updatePassword = (data) => {
  return request.put('/user/password', data)
}
