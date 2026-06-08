// 全局消息提示工具
// 用于在非组件环境中（如 axios 拦截器）显示消息

let messageInstance = null

// 防抖机制：记录最近显示的错误消息
const errorCache = {
  lastMessage: '',
  lastTime: 0,
  duration: 3000 // 3秒内相同消息不重复显示
}

export const setMessageInstance = (instance) => {
  messageInstance = instance
}

export const message = {
  success: (msg) => {
    if (messageInstance) {
      messageInstance.success(msg)
    } else {
      console.log('[Success]', msg)
    }
  },
  error: (msg) => {
    if (messageInstance) {
      const now = Date.now()
      // 如果是相同的错误消息，且在防抖时间内，则不显示
      if (errorCache.lastMessage === msg && (now - errorCache.lastTime) < errorCache.duration) {
        return
      }
      // 更新缓存
      errorCache.lastMessage = msg
      errorCache.lastTime = now
      messageInstance.error(msg)
    } else {
      console.error('[Error]', msg)
    }
  },
  warning: (msg) => {
    if (messageInstance) {
      messageInstance.warning(msg)
    } else {
      console.warn('[Warning]', msg)
    }
  },
  info: (msg) => {
    if (messageInstance) {
      messageInstance.info(msg)
    } else {
      console.info('[Info]', msg)
    }
  }
}
