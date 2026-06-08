import { ref } from 'vue'
import { getCurrentUser } from '@/api/user'

const userInfo = ref(null)
const permissions = ref([])

export const usePermission = () => {
  const loadUserPermissions = async () => {
    try {
      const res = await getCurrentUser()
      userInfo.value = res.data.user
      permissions.value = res.data.permissions || []
    } catch (error) {
      console.error('加载用户权限失败', error)
    }
  }

  const hasPermission = (permission) => {
    if (!permissions.value || permissions.value.length === 0) return false
    // 管理员和项目管理员拥有所有权限
    if (permissions.value.includes('ADMIN') || permissions.value.includes('PROJECT_ADMIN')) {
      return true
    }
    return permissions.value.includes(permission)
  }

  const isAdmin = () => {
    return permissions.value.includes('ADMIN')
  }

  const isProjectAdmin = () => {
    return permissions.value.includes('ADMIN') || permissions.value.includes('PROJECT_ADMIN')
  }

  const canBuild = () => {
    return hasPermission('DEVELOPER') || isProjectAdmin()
  }

  return {
    userInfo,
    permissions,
    loadUserPermissions,
    hasPermission,
    isAdmin,
    isProjectAdmin,
    canBuild
  }
}
