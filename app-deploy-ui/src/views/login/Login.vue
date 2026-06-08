<template>
  <div class="login-container">
    <!-- 左侧装饰区 -->
    <div class="login-left">
      <div class="decoration">
        <n-icon size="120" color="#fff" class="logo-icon">
          <RocketSharp />
        </n-icon>
        <h1 class="brand-title">AppDeploy</h1>
        <p class="brand-desc">轻量级 CICD 自动化部署平台</p>
        <div class="feature-list">
          <div class="feature-item">
            <n-icon size="24" color="#fff">
              <RocketSharp />
            </n-icon>
            <span>一键构建部署</span>
          </div>
          <div class="feature-item">
            <n-icon size="24" color="#fff">
              <GitBranchSharp />
            </n-icon>
            <span>Git 仓库集成</span>
          </div>
          <div class="feature-item">
            <n-icon size="24" color="#fff">
              <ServerSharp />
            </n-icon>
            <span>多服务器管理</span>
          </div>
          <div class="feature-item">
            <n-icon size="24" color="#fff">
              <PulseSharp />
            </n-icon>
            <span>实时构建日志</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="login-box">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>Welcome to AppDeploy</p>
        </div>
        
        <n-form
          ref="formRef"
          :model="formValue"
          :rules="rules"
          size="large"
        >
          <n-form-item path="account">
            <n-input
              v-model:value="formValue.account"
              placeholder="请输入用户名"
              clearable
            >
              <template #prefix>
                <n-icon :component="PersonSharp" />
              </template>
            </n-input>
          </n-form-item>
          
          <n-form-item path="password">
            <n-input
              v-model:value="formValue.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <n-icon :component="LockClosedSharp" />
              </template>
            </n-input>
          </n-form-item>
          
          <n-form-item>
            <n-button
              type="primary"
              block
              size="large"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </n-button>
          </n-form-item>
        </n-form>
        
        <!-- 底部作者信息 -->
        <div class="login-footer">
          <n-text depth="3" style="font-size: 12px;">
            开源作者：Steven
          </n-text>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { 
  PersonSharp, 
  LockClosedSharp,
  RocketSharp,
  GitBranchSharp,
  ServerSharp,
  PulseSharp
} from '@vicons/ionicons5'
import { getUserInfo, login } from '@/api/auth'

const router = useRouter()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)

const formValue = reactive({
  account: 'admin',
  password: ''
})

const rules = {
  account: {
    required: true,
    message: '请输入用户名',
    trigger: 'blur'
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur'
  }
}

const handleLogin = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    
    const data = await login(formValue)

    localStorage.setItem('tokenName', data.tokenName)
    localStorage.setItem('token', data.tokenValue)

    const user = await getUserInfo()
    localStorage.setItem('user', JSON.stringify(user))
    
    message.success('登录成功')
    router.push('/')
  } catch (error) {
    if (error?.errorFields) {
      // 表单验证错误
      return
    }
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #18a058 0%, #36ad6a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  top: -200px;
  right: -200px;
}

.login-left::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  bottom: -150px;
  left: -150px;
}

.decoration {
  text-align: center;
  color: white;
  z-index: 1;
  padding: 40px;
}

.logo-icon {
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.brand-title {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.brand-desc {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 60px;
}

.feature-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  max-width: 400px;
  margin: 0 auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.1);
  padding: 16px 20px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.login-right {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.05);
}

.login-box {
  width: 360px;
  padding: 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h2 {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  opacity: 0.6;
}

.login-footer {
  margin-top: 32px;
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.login-tips {
  margin-top: 24px;
}

@media (max-width: 768px) {
  .login-left {
    display: none;
  }
  
  .login-right {
    width: 100%;
  }
}
</style>
