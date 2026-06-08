import request from './request'

// 项目组列表
export const getProjectGroupList = (params) => {
  return request.get('/project-group/list', { params })
}

// 项目组详情
export const getProjectGroup = (id) => {
  return request.get(`/project-group/${id}`)
}

// 新增项目组
export const addProjectGroup = (data) => {
  return request.post('/project-group', data)
}

// 更新项目组
export const updateProjectGroup = (data) => {
  return request.put('/project-group', data)
}

// 删除项目组
export const deleteProjectGroup = (id) => {
  return request.delete(`/project-group/${id}`)
}

// 获取所有项目组（不分页）
export const getAllProjectGroups = () => {
  return request.get('/project-group/all')
}
