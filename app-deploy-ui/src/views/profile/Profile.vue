<template>
  <div class="profile">
    <n-card title="个人信息" :bordered="false">
      <n-tabs type="line" animated>
        <n-tab-pane name="basic" tab="基本信息">
          <n-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-placement="left"
            label-width="100"
            style="max-width: 600px; margin-top: 24px;"
          >
            <n-form-item label="用户名" path="username">
              <n-input v-model:value="profileForm.username" disabled />
            </n-form-item>
            
            <n-form-item label="昵称" path="nickname">
              <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
            </n-form-item>
            
            <n-form-item label="邮箱" path="email">
              <n-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
            </n-form-item>
            
            <n-form-item>
              <n-space>
                <n-button type="primary" @click="handleUpdateProfile" :loading="profileLoading">
                  <template #icon>
                    <n-icon><SaveSharp /></n-icon>
                  </template>
                  保存修改
                </n-button>
                <n-button @click="loadUserInfo">
                  <template #icon>
                    <n-icon><RefreshSharp /></n-icon>
                  </template>
                  重置
                </n-button>
              </n-space>
            </n-form-item>
          </n-form>
        </n-tab-pane>
        
        <n-tab-pane name="password" tab="修改密码">
          <n-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-placement="left"
            label-width="100"
            style="max-width: 600px; margin-top: 24px;"
          >
            <n-form-item label="原密码" path="oldPassword">
              <n-input
                v-model:value="passwordForm.oldPassword"
                type="password"
                show-password-on="click"
                placeholder="请输入原密码"
              />
            </n-form-item>
            
            <n-form-item label="新密码" path="newPassword">
              <n-input
                v-model:value="passwordForm.newPassword"
                type="password"
                show-password-on="click"
                placeholder="请输入新密码（至少6位）"
              />
            </n-form-item>
            
            <n-form-item label="确认密码" path="confirmPassword">
              <n-input
                v-model:value="passwordForm.confirmPassword"
                type="password"
                show-password-on="click"
                placeholder="请再次输入新密码"
              />
            </n-form-item>
            
            <n-form-item>
              <n-space>
                <n-button type="primary" @click="handleUpdatePassword" :loading="passwordLoading">
                  <template #icon>
                    <n-icon><KeySharp /></n-icon>
                  </template>
                  修改密码
                </n-button>
                <n-button @click="resetPasswordForm">
                  <template #icon>
                    <n-icon><RefreshSharp /></n-icon>
                  </template>
                  重置
                </n-button>
              </n-space>
            </n-form-item>
          </n-form>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { SaveSharp, RefreshSharp, KeySharp } from '@vicons/ionicons5'
import { getCurrentUser, updateProfile, updatePassword } from '@/api/user'

const message = useMessage()
const profileFormRef = ref()
const passwordFormRef = ref()
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  username: '',
  nickname: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePasswordSame = (rule, value) => {
  if (value !== passwordForm.newPassword) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const profileRules = {
  nickname: {
    required: true,
    message: '请输入昵称',
    trigger: 'blur'
  },
  email: [
    {
      required: true,
      message: '请输入邮箱',
      trigger: 'blur'
    },
    {
      type: 'email',
      message: '请输入正确的邮箱格式',
      trigger: 'blur'
    }
  ]
}

const passwordRules = {
  oldPassword: {
    required: true,
    message: '请输入原密码',
    trigger: 'blur'
  },
  newPassword: [
    {
      required: true,
      message: '请输入新密码',
      trigger: 'blur'
    },
    {
      min: 6,
      message: '密码至少6位',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      required: true,
      message: '请再次输入新密码',
      trigger: 'blur'
    },
    {
      validator: validatePasswordSame,
      message: '两次输入的密码不一致',
      trigger: 'blur'
    }
  ]
}

const loadUserInfo = async () => {
  try {
    const data = await getCurrentUser()
    console.log('loadUserInfo',  data)
    profileForm.username = data.user.username || ''
    profileForm.nickname = data.user.nickname || ''
    profileForm.email = data.user.email || ''
  } catch (error) {
    console.error(error)
  }
}

const handleUpdateProfile = async () => {
  try {
    await profileFormRef.value?.validate()
    profileLoading.value = true
    
    await updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email
    })
    
    message.success('个人信息更新成功')
    
    // 更新本地存储的用户信息
    const userData = await getCurrentUser()
    localStorage.setItem('user', JSON.stringify(userData))
    
    // 刷新页面让 Layout 中的用户信息更新
    setTimeout(() => {
      window.location.reload()
    }, 500)
  } catch (error) {
    if (error?.errorFields) {
      return
    }
    console.error(error)
  } finally {
    profileLoading.value = false
  }
}

const handleUpdatePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    passwordLoading.value = true
    
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    
    message.success('密码修改成功，请重新登录')
    
    // 清空密码表单
    resetPasswordForm()
    
    // 延迟后退出登录
    setTimeout(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('tokenName')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }, 1500)
  } catch (error) {
    if (error?.errorFields) {
      return
    }
    console.error(error)
  } finally {
    passwordLoading.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile {
  width: 100%;
}
</style>
