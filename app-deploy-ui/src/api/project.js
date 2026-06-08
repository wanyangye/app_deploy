import request from './request'

// 项目列表
export const getProjectList = (params) => {
  return request.get('/project/list', { params })
}

// 项目详情
export const getProject = (id) => {
  return request.get(`/project/${id}`)
}

// 新增项目
export const addProject = (data) => {
  return request.post('/project', data)
}

// 更新项目
export const updateProject = (data) => {
  return request.put('/project', data)
}

// 删除项目
export const deleteProject = (id) => {
  return request.delete(`/project/${id}`)
}

// 获取项目成员列表
export const getProjectMembers = (projectId) => {
  return request.get(`/project/${projectId}/members`)
}

// 分配项目成员
export const assignProjectMembers = (projectId, members) => {
  return request.post(`/project/${projectId}/members`, { members })
}

// 移除项目成员
export const removeProjectMember = (projectId, memberId) => {
  return request.delete(`/project/${projectId}/members/${memberId}`)
}
