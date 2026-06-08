import request from './request'

// 获取登录日志列表
export const getLoginLogList = (params) => {
  return request.get('/login-log/list', { params })
}

// 删除登录日志
export const deleteLoginLog = (id) => {
  return request.delete(`/login-log/${id}`)
}

// 清空登录日志
export const clearLoginLog = () => {
  return request.delete('/login-log/clear')
}

// 获取操作日志列表
export const getOperationLogList = (params) => {
  return request.get('/operation-log/list', { params })
}

// 删除操作日志
export const deleteOperationLog = (id) => {
  return request.delete(`/operation-log/${id}`)
}

// 清空操作日志
export const clearOperationLog = () => {
  return request.delete('/operation-log/clear')
}
