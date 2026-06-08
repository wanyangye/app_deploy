<template>
  <n-config-provider :theme="theme" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <AppContent />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { defineComponent, h, ref, onMounted, onUnmounted } from 'vue'
import { NConfigProvider, NMessageProvider, NDialogProvider, useMessage, darkTheme } from 'naive-ui'
import { setMessageInstance } from '@/utils/message'
import { RouterView } from 'vue-router'

const theme = ref(null)

const themeOverrides = {
  common: {
    primaryColor: '#18a058',
    primaryColorHover: '#36ad6a',
    primaryColorPressed: '#0c7a43'
  }
}

// 监听主题变化事件
const handleThemeChange = (event) => {
  const newTheme = event.detail
  theme.value = newTheme === 'dark' ? darkTheme : null
}

onMounted(() => {
  // 加载保存的主题
  const savedTheme = localStorage.getItem('theme')
  theme.value = savedTheme === 'dark' ? darkTheme : null
  
  // 监听主题变化
  window.addEventListener('theme-change', handleThemeChange)
})

onUnmounted(() => {
  window.removeEventListener('theme-change', handleThemeChange)
})

// 创建一个内部组件来获取 message 实例
const AppContent = defineComponent({
  setup() {
    const message = useMessage()
    setMessageInstance(message)
    
    return () => h(RouterView)
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif;
}
</style>
