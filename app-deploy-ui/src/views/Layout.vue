<template>
  <n-layout has-sider class="layout" v-if="menuLayout === 'side'">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="logo">
        <n-icon :size="collapsed ? 32 : 36" :color="'#18a058'">
          <RocketSharp />
        </n-icon>
        <span v-if="!collapsed" class="logo-text">AppDeploy</span>
      </div>

      <n-menu
        v-model:value="activeKey"
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        @update:value="handleMenuSelect"
      />

      <div class="version-info" v-if="!collapsed">
        <n-text depth="4" style="font-size: 12px; color: #999;">当前版本v1.0作者：Steven</n-text>
      </div>
    </n-layout-sider>

    <n-layout>
      <n-layout-header bordered class="header">
        <div class="header-content">
          <div class="header-left">
            <n-breadcrumb>
              <n-breadcrumb-item>{{ currentMenuName }}</n-breadcrumb-item>
            </n-breadcrumb>
          </div>

          <div class="header-right">
            <n-space :size="16" align="center">
              <n-tooltip placement="bottom">
                <template #trigger>
                  <n-button text class="icon-button" @click="toggleMenuLayout">
                    <n-icon size="20">
                      <ReorderThreeSharp />
                    </n-icon>
                  </n-button>
                </template>
                切换为顶部菜单
              </n-tooltip>
              
              <n-tooltip placement="bottom">
                <template #trigger>
                  <n-button text class="icon-button" @click="toggleTheme">
                    <n-icon size="20">
                      <MoonSharp v-if="!isDark" />
                      <SunnySharp v-else />
                    </n-icon>
                  </n-button>
                </template>
                {{ isDark ? '切换为亮色模式' : '切换为暗黑模式' }}
              </n-tooltip>
              
              <n-tooltip placement="bottom">
                <template #trigger>
                  <n-button text class="icon-button" @click="toggleFullscreen">
                    <n-icon size="20">
                      <ExpandSharp v-if="!isFullscreen" />
                      <ContractSharp v-else />
                    </n-icon>
                  </n-button>
                </template>
                {{ isFullscreen ? '退出全屏' : '全屏' }}
              </n-tooltip>

              <n-dropdown :options="userOptions" @select="handleUserAction">
                <div class="user-info">
                  <n-avatar round size="small" :style="{ background: '#18a058' }">
                    {{ userInitial }}
                  </n-avatar>
                  <span class="username">{{ user?.nickname || user?.username }}</span>
                </div>
              </n-dropdown>
            </n-space>
          </div>
        </div>
      </n-layout-header>

      <n-layout-content content-style="padding: 24px;" :native-scrollbar="false">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
  
  <!-- 顶部菜单布局 -->
  <n-layout v-else class="layout">
    <n-layout-header bordered class="header header-with-menu">
      <div class="header-top">
        <div class="header-logo">
          <n-icon size="32" :color="'#18a058'">
            <RocketSharp />
          </n-icon>
        </div>
        
        <n-space style="flex: 1; justify-content: center;">
          <n-menu
            v-model:value="activeKey"
            mode="horizontal"
            :options="menuOptions"
            @update:value="handleMenuSelect"
          />
        </n-space>
        
        <div class="header-right">
          <n-space :size="16" align="center">
            <n-tooltip placement="bottom">
              <template #trigger>
                <n-button text class="icon-button" @click="toggleMenuLayout">
                  <n-icon size="20">
                    <MenuSharp />
                  </n-icon>
                </n-button>
              </template>
              切换为侧边栏菜单
            </n-tooltip>
            
            <n-tooltip placement="bottom">
              <template #trigger>
                <n-button text class="icon-button" @click="toggleTheme">
                  <n-icon size="20">
                    <MoonSharp v-if="!isDark" />
                    <SunnySharp v-else />
                  </n-icon>
                </n-button>
              </template>
              {{ isDark ? '切换为亮色模式' : '切换为暗黑模式' }}
            </n-tooltip>
            
            <n-tooltip placement="bottom">
              <template #trigger>
                <n-button text class="icon-button" @click="toggleFullscreen">
                  <n-icon size="20">
                    <ExpandSharp v-if="!isFullscreen" />
                    <ContractSharp v-else />
                  </n-icon>
                </n-button>
              </template>
              {{ isFullscreen ? '退出全屏' : '全屏' }}
            </n-tooltip>

            <n-tooltip placement="bottom">
              <template #trigger>
                <n-button text class="icon-button" @click="openGithub">
                  <n-icon size="20">
                    <LogoGithub />
                  </n-icon>
                </n-button>
              </template>
              查看源码
            </n-tooltip>

            <n-dropdown :options="userOptions" @select="handleUserAction">
              <div class="user-info">
                <n-avatar round size="small" :style="{ background: '#18a058' }">
                  {{ userInitial }}
                </n-avatar>
                <span class="username">{{ user?.nickname || user?.username }}</span>
              </div>
            </n-dropdown>
          </n-space>
        </div>
      </div>
    </n-layout-header>

    <n-layout-content content-style="padding: 24px;" :native-scrollbar="false" class="layout-content-top">
      <router-view />
    </n-layout-content>
  </n-layout>
</template>

<script setup>
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon, useMessage } from 'naive-ui'
import {
  SpeedometerSharp,
  FolderOpenSharp,
  ServerSharp,
  RocketSharp,
  LogOutSharp,
  PersonSharp,
  PeopleSharp,
  Albums,
  ShieldCheckmarkSharp,
  MenuSharp,
  ExpandSharp,
  ContractSharp,
  LogoGithub,
  ExtensionPuzzleSharp,
  MoonSharp,
  SunnySharp,
  DocumentTextSharp,
  LogInSharp,
  ListSharp,
  ReorderThreeSharp
} from '@vicons/ionicons5'
import { logout } from '@/api/auth'
import { getUserMenus } from '@/api/menu'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const collapsed = ref(false)
const activeKey = ref('/dashboard')
const menuOptions = ref([])
const isFullscreen = ref(false)
const isDark = ref(false)
const menuLayout = ref('side') // 'side' 或 'top'

const user = computed(() => {
  const userStr = localStorage.getItem('user')
  return userStr ? JSON.parse(userStr) : null
})

const userInitial = computed(() => {
  const name = user.value?.nickname || user.value?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const renderIcon = (iconName) => {
  const iconMap = {
    SpeedometerSharp,
    FolderOpenSharp,
    ServerSharp,
    RocketSharp,
    PeopleSharp,
    Albums,
    ShieldCheckmarkSharp,
    MenuSharp,
    ExtensionPuzzleSharp,
    DocumentTextSharp,
    LogInSharp,
    ListSharp
  }
  const IconComponent = iconMap[iconName]
  return () => h(NIcon, null, { default: () => h(IconComponent || SpeedometerSharp) })
}

const loadUserMenus = async () => {
  try {
    const menus = await getUserMenus()
    menuOptions.value = convertToMenuOptions(menus)
  } catch (error) {
    console.error('加载菜单失败', error)
  }
}

const convertToMenuOptions = (menus) => {
  return menus.map(menu => ({
    label: menu.name,
    key: menu.path,
    icon: renderIcon(menu.icon),
    children: menu.children ? convertToMenuOptions(menu.children) : undefined
  }))
}

const currentMenuName = computed(() => {
  // 先尝试一级菜单
  let menu = menuOptions.value.find(item => item.key === activeKey.value)
  if (menu) return menu.label

  // 尝试二级菜单
  for (const item of menuOptions.value) {
    if (item.children) {
      const child = item.children.find(c => c.key === activeKey.value)
      if (child) return child.label
    }
  }

  return '仪表盘'
})

const userOptions = [
  {
    label: '个人信息',
    key: 'profile',
    icon: renderIcon(PersonSharp)
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: renderIcon(LogOutSharp)
  }
]

onMounted(() => {
  activeKey.value = route.path
  loadUserMenus()
})

const handleMenuSelect = (key) => {
  activeKey.value = key
  router.push(key)
}

const handleUserAction = async (key) => {
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'logout') {
    try {
      await logout()
    } catch (error) {
      console.error(error)
    } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('tokenName')
      localStorage.removeItem('user')
      message.success('已退出登录')
      router.push('/login')
    }
  }
}

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
      isFullscreen.value = false
    }
  }
}

// 监听全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}


// 主题切换
const toggleTheme = () => {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  // 触发自定义事件，通知 App.vue 更新主题
  window.dispatchEvent(new CustomEvent('theme-change', { detail: isDark.value ? 'dark' : 'light' }))
}

// 菜单布局切换
const toggleMenuLayout = () => {
  menuLayout.value = menuLayout.value === 'side' ? 'top' : 'side'
  localStorage.setItem('menuLayout', menuLayout.value)
  message.success(`已切换为${menuLayout.value === 'side' ? '侧边栏' : '顶部'}菜单`)
}

onMounted(() => {
  activeKey.value = route.path
  loadUserMenus()
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  
  // 加载保存的主题
  const savedTheme = localStorage.getItem('theme')
  isDark.value = savedTheme === 'dark'
  
  // 加载保存的菜单布局
  const savedLayout = localStorage.getItem('menuLayout')
  if (savedLayout === 'top' || savedLayout === 'side') {
    menuLayout.value = savedLayout
  }
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<style scoped>
.layout {
  height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
}



.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #18a058;
}

.header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
}

.username {
  color: #333;
  font-size: 14px;
}

.icon-button {
  padding: 4px;
  height: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-button:hover {
  background: #f5f5f5;
  border-radius: 4px;
}

.version-info {
  position: absolute;
  bottom: 16px;
  left: 30px;
}

/* 顶部菜单布局样式 */
.header-with-menu {
  height: 64px;
  padding: 0;
}

.header-top {
  height: 64px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 50px;
}

.header-logo .logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #18a058;
  white-space: nowrap;
}

.header-menu {
  padding: 0 24px;
}

.layout-content-top {
  margin-top: 0;
  height: calc(100vh - 64px);
}

/* 暗黑模式适配 */
:deep(.n-layout.n-layout--static-positioned) {
  background: var(--n-color);
}
</style>
