import request from './request'

/**
 * 获取插件列表(按分类分组)
 */
export function getPluginList() {
  return request.get('/plugin/list')
}

/**
 * 获取服务器已安装插件列表
 */
export function getInstalledPlugins(serverId) {
  return request.get(`/plugin/installed/${serverId}`)
}

/**
 * 检查插件是否已安装
 */
export function checkInstalled(serverId, pluginId) {
  return request.get(`/plugin/check/${serverId}/${pluginId}`)
}

/**
 * 安装插件
 */
export function installPlugin(data) {
  return request.post('/plugin/install', data)
}

/**
 * 卸载插件
 */
export function uninstallPlugin(installId) {
  return request.delete(`/plugin/uninstall/${installId}`)
}

/**
 * 获取安装日志
 */
export function getInstallLog(installId) {
  return request.get(`/plugin/install/log/${installId}`)
}

/**
 * 管理员 - 获取插件管理列表
 */
export function getManagePluginList() {
  return request.get('/plugin/manage/list')
}

/**
 * 管理员 - 保存插件
 */
export function savePlugin(data) {
  return request.post('/plugin/manage', data)
}

/**
 * 管理员 - 删除插件
 */
export function deletePlugin(id) {
  return request.delete(`/plugin/manage/${id}`)
}
