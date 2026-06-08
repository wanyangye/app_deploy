import request from './request'

// 获取当前用户菜单
export const getUserMenus = () => {
  return request.get('/auth/permission')
}

// 获取所有菜单树
export const getMenuTree = () => {
  return request.get('/auth/permission-tree')
}

// 菜单列表
export const getMenuList = (params) => {
  return request.get('/menu/list', { params })
}

// 菜单详情
export const getMenu = (id) => {
  return request.get(`/menu/${id}`)
}

// 添加菜单
export const addMenu = (data) => {
  return request.post('/menu', data)
}

// 更新菜单
export const updateMenu = (data) => {
  return request.put('/menu', data)
}

// 删除菜单
export const deleteMenu = (id) => {
  return request.delete(`/menu/${id}`)
}

// 获取角色的菜单ID列表
export const getRoleMenuIds = (roleId) => {
  return request.get(`/menu/role/${roleId}`)
}

// 为角色分配菜单
export const assignRoleMenus = (roleId, menuIds) => {
  return request.post(`/menu/role/${roleId}`, { menuIds })
}
