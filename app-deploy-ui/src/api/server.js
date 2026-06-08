import request from './request'

// 服务器列表
export const getServerList = (params) => {
  return request.get('/server/list', { params })
}

// 服务器详情
export const getServer = (id) => {
  return request.get(`/server/${id}`)
}

// 新增服务器
export const addServer = (data) => {
  return request.post('/server', data)
}

// 更新服务器
export const updateServer = (data) => {
  return request.put('/server', data)
}

// 删除服务器
export const deleteServer = (id) => {
  return request.delete(`/server/${id}`)
}

// 测试连接
export const testConnection = (id) => {
  return request.post(`/server/test/${id}`)
}

// 获取服务器监控信息
export const getServerMonitor = (id) => {
  return request.get(`/server/monitor/${id}`)
}
