import request from './request'

// 角色列表
export const getRoleList = (params) => {
  return request.get('/role/list', { params })
}

// 所有角色
export const getAllRoles = () => {
  return request.get('/role/all')
}

// 角色详情
export const getRole = (id) => {
  return request.get(`/role/${id}`)
}

// 添加角色
export const addRole = (data) => {
  return request.post('/role', data)
}

// 更新角色
export const updateRole = (data) => {
  return request.put('/role', data)
}

// 删除角色
export const deleteRole = (id) => {
  return request.delete(`/role/${id}`)
}
