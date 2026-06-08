<template>
  <div class="project-add-page">
    <n-card :title="isEdit ? '编辑项目' : '新增项目'" :bordered="false">
      <template #header-extra>
        <n-button text @click="handleBack">
          <template #icon>
            <n-icon><ArrowBackSharp /></n-icon>
          </template>
          返回列表
        </n-button>
      </template>

      <!-- 步骤条 -->
      <n-steps :current="currentStep" style="margin-bottom: 24px;">
        <n-step title="基础信息" description="配置项目基本信息" />
        <n-step title="Git配置" description="配置代码仓库信息" />
        <n-step title="构建配置" description="配置构建相关参数" />
        <n-step title="部署配置" description="配置部署服务器和脚本" />
      </n-steps>

      <!-- 表单内容 -->
      <n-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        label-placement="left"
        label-width="120"
        style="max-width: 800px; margin: 0 auto;"
      >
        <!-- 步骤1: 基础信息 -->
        <div v-show="currentStep === 1" class="step-content">
          <n-grid :cols="2" :x-gap="24">
            <n-form-item-gi label="项目名称" path="name">
              <n-input 
                v-model:value="formState.name" 
                placeholder="英文开头，只能包含字母、数字、连字符、下划线"
                size="large"
              />
            </n-form-item-gi>

            <n-form-item-gi label="项目类型" path="projectType">
              <n-select
                v-model:value="formState.projectType"
                :options="projectTypeOptions"
                placeholder="请选择项目类型"
                size="large"
                @update:value="handleProjectTypeChange"
              />
            </n-form-item-gi>
          </n-grid>

          <n-grid :cols="2" :x-gap="24">
            <n-form-item-gi label="环境" path="env">
              <n-select
                v-model:value="formState.env"
                :options="envOptions"
                tag
                filterable
                placeholder="请选择或自定义环境"
                size="large"
              />
            </n-form-item-gi>

            <n-form-item-gi label="所属项目组" path="groupId">
              <n-select
                v-model:value="formState.groupId"
                :options="projectGroupOptions"
                clearable
                placeholder="请选择项目组"
                size="large"
              />
            </n-form-item-gi>
          </n-grid>

          <n-form-item label="项目描述" path="description">
            <n-input 
              v-model:value="formState.description" 
              type="textarea" 
              :rows="3" 
              placeholder="请输入项目描述"
            />
          </n-form-item>
        </div>

        <!-- 步骤2: Git配置 -->
        <div v-show="currentStep === 2" class="step-content">
          <n-form-item label="Git地址" path="gitUrl">
            <n-input 
              v-model:value="formState.gitUrl" 
              placeholder="https://github.com/xxx/xxx.git"
              size="large"
            />
          </n-form-item>

          <n-grid :cols="2" :x-gap="24">
            <n-form-item-gi label="分支" path="branch">
              <n-input 
                v-model:value="formState.branch" 
                placeholder="master"
                size="large"
              />
            </n-form-item-gi>

            <n-form-item-gi label="项目目录" path="projectDir">
              <n-input 
                v-model:value="formState.projectDir" 
                placeholder="backend（可选，默认为根目录）"
                size="large"
              >
                <template #suffix>
                  <n-text depth="3" style="font-size: 12px;">如项目在子目录中</n-text>
                </template>
              </n-input>
            </n-form-item-gi>
          </n-grid>

          <n-grid :cols="2" :x-gap="24">
            <n-form-item-gi label="Git用户名" path="gitAccount">
              <n-input 
                v-model:value="formState.gitAccount"
                placeholder="admin(可选)"
                size="large"
              />
            </n-form-item-gi>

            <n-form-item-gi label="Git密码" path="gitPassword">
              <n-input 
                v-model:value="formState.gitPassword" 
                type="password" 
                show-password-on="click" 
                placeholder="请输入Git密码(可选)"
                size="large"
              />
            </n-form-item-gi>
          </n-grid>
        </div>

        <!-- 步骤3: 构建配置 -->
        <div v-show="currentStep === 3" class="step-content">
          <n-grid :cols="2" :x-gap="24">
            <n-form-item-gi label="构建命令" path="buildCommand">
              <n-select
                v-model:value="formState.buildCommand"
                :options="buildCommandOptions"
                tag
                filterable
                placeholder="请选择或自定义构建命令"
                size="large"
              />
            </n-form-item-gi>

            <n-form-item-gi label="产物路径" path="buildDir">
              <n-select
                v-model:value="formState.buildDir"
                :options="buildDirOptions"
                tag
                filterable
                placeholder="请选择或自定义产物路径"
                size="large"
              />
            </n-form-item-gi>
          </n-grid>

          <n-alert type="info" style="margin-top: 16px;">
            <template #header>提示</template>
            构建命令和产物路径会根据项目类型自动填充，您也可以自定义修改
          </n-alert>
        </div>

        <!-- 步骤4: 部署配置 -->
        <div v-show="currentStep === 4" class="step-content">
          <n-form-item label="部署服务器" path="serverIds">
            <n-select
              v-model:value="formState.serverIds"
              :options="serverOptions"
              multiple
              clearable
              placeholder="请选择部署服务器（可多选）"
              size="large"
            />
          </n-form-item>

          <n-grid :cols="2" :x-gap="24">
            <n-form-item-gi label="自动部署" path="autoDeploy">
              <n-radio-group v-model:value="formState.autoDeploy" size="large" @update:value="handleAutoDeployChange">
                <n-radio :value="1">是</n-radio>
                <n-radio :value="0">否</n-radio>
              </n-radio-group>
              <template #feedback>
                <n-text depth="3">启用后会自动生成部署脚本并执行</n-text>
              </template>
            </n-form-item-gi>

            <n-form-item-gi v-if="formState.autoDeploy === 1" label="应用端口" path="appPort">
              <n-input-number 
                v-model:value="formState.appPort" 
                :min="1" 
                :max="65535" 
                placeholder="8080" 
                style="width: 100%"
                size="large"
              />
            </n-form-item-gi>
          </n-grid>

          <n-form-item v-if="formState.autoDeploy === 1" label="部署目录" path="deployPath">
            <n-input 
              v-model:value="formState.deployPath" 
              placeholder="/home/deploy/"
              size="large"
            >
              <template #suffix>
                <n-text depth="3" style="font-size: 12px;">服务器上存放应用的目录</n-text>
              </template>
            </n-input>
          </n-form-item>

          <n-form-item v-if="formState.autoDeploy === 1" label="部署脚本" path="deployScript">
            <n-input
              v-model:value="formState.deployScript"
              type="textarea"
              :rows="6"
              placeholder="部署脚本将自动生成，也可自定义"
            />
            <template #feedback>
              <n-text depth="3">脚本会在构建完成后在服务器上执行</n-text>
            </template>
          </n-form-item>
        </div>
      </n-form>

      <!-- 底部操作按钮 -->
      <template #footer>
        <div style="display: flex; justify-content: space-between; padding-top: 16px; border-top: 1px solid #f0f0f0;">
          <n-button 
            v-if="currentStep > 1" 
            size="large"
            @click="handlePrevStep"
          >
            <template #icon>
              <n-icon><ChevronBackSharp /></n-icon>
            </template>
            上一步
          </n-button>
          <div v-else></div>

          <n-space>
            <n-button size="large" @click="handleCancel">取消</n-button>
            <n-button 
              v-if="currentStep < 4" 
              type="primary" 
              size="large"
              @click="handleNextStep"
            >
              下一步
              <template #icon>
                <n-icon><ChevronForwardSharp /></n-icon>
              </template>
            </n-button>
            <n-button 
              v-else 
              type="primary" 
              size="large"
              @click="handleSubmit"
              :loading="submitLoading"
            >
              <template #icon>
                <n-icon><CheckmarkSharp /></n-icon>
              </template>
              {{ isEdit ? '保存修改' : '完成创建' }}
            </n-button>
          </n-space>
        </div>
      </template>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { 
  ArrowBackSharp, 
  ChevronBackSharp, 
  ChevronForwardSharp,
  CheckmarkSharp 
} from '@vicons/ionicons5'
import { addProject, getProject, updateProject } from '@/api/project'
import { getServerList } from '@/api/server'
import { getAllProjectGroups } from '@/api/project-group'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// 当前步骤
const currentStep = ref(1)
const submitLoading = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const projectId = ref(null)

// 表单数据
const formState = reactive({
  name: '',
  description: '',
  gitUrl: '',
  branch: 'master',
  projectDir: '',
  gitAccount: '',
  gitPassword: '',
  projectType: 'JAVA',
  env: 'development',
  groupId: null,
  buildCommand: '',
  buildDir: '',
  serverIds: [],
  autoDeploy: 1,
  appPort: 8080,
  deployPath: '/www/wwwroot/',
  deployScript: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { 
      pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, 
      message: '英文开头，只能包含字母、数字、连字符、下划线', 
      trigger: 'blur' 
    }
  ],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  env: [{ required: true, message: '请选择环境', trigger: 'change' }],
  groupId: [{ required: true, message: '请选择项目组', trigger: 'change' }],
  gitUrl: [{ required: true, message: '请输入Git地址', trigger: 'blur' }],
  branch: [{ required: true, message: '请输入分支', trigger: 'blur' }],
  buildCommand: [{ required: true, message: '请输入构建命令', trigger: 'blur' }],
  buildDir: [{ required: true, message: '请输入产物路径', trigger: 'blur' }],
  serverIds: [{ type: 'array', required: true, message: '请选择部署服务器', trigger: 'change' }]
}

// 项目类型选项
const projectTypeOptions = [
  { label: 'SpringBoot项目', value: 'JAVA' },
  { label: 'Vue 项目', value: 'VUE' }
]

// 环境选项
const envOptions = ref([
  { label: '开发环境', value: 'development' },
  { label: '测试环境', value: 'test' },
  { label: '预发环境', value: 'staging' },
  { label: '生产环境', value: 'production' }
])

// 构建命令选项
const buildCommandOptions = ref([
  { label: 'mvn clean package -DskipTests', value: 'mvn clean package -DskipTests' },
  { label: 'npm run build', value: 'npm run build' },
  { label: 'pnpm run build', value: 'pnpm run build' },
  { label: 'yarn build', value: 'yarn build' }
])

// 产物路径选项
const buildDirOptions = ref([
  { label: 'target/*.jar', value: 'target/*.jar' },
  { label: 'target/*.war', value: 'target/*.war' },
  { label: 'dist', value: 'dist' },
  { label: 'build', value: 'build' }
])

// 服务器选项
const serverOptions = ref([])

// 项目组选项
const projectGroupOptions = ref([])

// 加载服务器列表
const loadServers = async () => {
  try {
    const res = await getServerList({ current: 1, size: 100 })
    serverOptions.value = res.records.map(server => ({
      label: `${server.name} (${server.host})`,
      value: server.id
    }))
  } catch (error) {
    message.error('加载服务器列表失败')
  }
}

// 加载项目组列表
const loadProjectGroups = async () => {
  try {
    const res = await getAllProjectGroups()
    projectGroupOptions.value = res.map(group => ({
      label: group.name,
      value: group.id
    }))
  } catch (error) {
    message.error('加载项目组列表失败')
  }
}

// 处理项目类型变化
const handleProjectTypeChange = (type) => {
  if (type === 'JAVA') {
    formState.buildCommand = 'mvn clean package -DskipTests'
    formState.buildDir = 'target/*.jar'
    // 编辑模式下不自动重置端口，新增模式才设置默认值
    if (!isEdit.value) {
      formState.appPort = 8080
    }
    
    // 生成Java部署脚本
    if (formState.autoDeploy === 1) {
      generateDeployScript()
    }
  } else if (type === 'VUE') {
    formState.buildCommand = 'npm run build'
    formState.buildDir = 'dist'
    // 编辑模式下不自动重置端口，新增模式才设置默认值
    if (!isEdit.value) {
      formState.appPort = 80
    }
    
    // 生成Vue部署脚本
    if (formState.autoDeploy === 1) {
      generateDeployScript()
    }
  }
}

// 处理自动部署变化
const handleAutoDeployChange = (value) => {
  if (value === 1) {
    generateDeployScript()
  } else {
    formState.deployScript = ''
  }
}

// 生成部署脚本
const generateDeployScript = () => {
  const { projectType, name, appPort, deployPath } = formState
  
  if (projectType === 'JAVA') {
    formState.deployScript = `#!/bin/bash
# Java项目部署脚本

APP_NAME="${name}"
APP_PORT={{appPort}}
DEPLOY_PATH="{{uploadPath}}"

# 进入部署目录
cd \${DEPLOY_PATH} || exit 1

# 创建必要的目录
mkdir -p logs backup

# 停止旧应用
if [ -f "\${APP_NAME}.pid" ]; then
  OLD_PID=\$(cat \${APP_NAME}.pid)
  if ps -p \$OLD_PID > /dev/null 2>&1; then
    echo "[INFO] Stopping old application (PID: \$OLD_PID)..."
    kill \$OLD_PID
    sleep 3
    # 强制停止
    if ps -p \$OLD_PID > /dev/null 2>&1; then
      kill -9 \$OLD_PID
    fi
  fi
  rm -f \${APP_NAME}.pid
fi

# 备份旧版本
if [ -f "\${APP_NAME}.jar" ]; then
  BACKUP_NAME="\${APP_NAME}_\$(date +%Y%m%d_%H%M%S).jar"
  mv \${APP_NAME}.jar backup/\${BACKUP_NAME}
  echo "[INFO] Backup old version: \${BACKUP_NAME}"
fi

# 查找并重命名新上传的jar包
echo "[INFO] Looking for uploaded jar file..."
UPLOADED_JAR=\$(ls -t *.jar 2>/dev/null | head -n 1)
if [ -n "\$UPLOADED_JAR" ]; then
  if [ "\$UPLOADED_JAR" != "\${APP_NAME}.jar" ]; then
    mv "\$UPLOADED_JAR" "\${APP_NAME}.jar"
    echo "[INFO] Renamed \$UPLOADED_JAR to \${APP_NAME}.jar"
  fi
else
  echo "[ERROR] No jar file found in current directory!"
  exit 1
fi

# 清空旧日志
echo "" > logs/\${APP_NAME}.log

# 启动新应用
echo "[INFO] Starting new application..."
echo "[INFO] Application port: \${APP_PORT}"
nohup java -jar \${APP_NAME}.jar --server.port=\${APP_PORT} > logs/\${APP_NAME}.log 2>&1 &
NEW_PID=$!
echo \$NEW_PID > \${APP_NAME}.pid
echo "[INFO] Application PID: \$NEW_PID"

# 等待应用启动（输出启动日志）
echo ""
echo "[LOG] Application startup logs:"
echo "================================================"
sleep 3
# 输出启动日志，最多10秒
timeout 10 tail -f logs/\${APP_NAME}.log 2>/dev/null &
TAIL_PID=$!
sleep 10
kill \$TAIL_PID 2>/dev/null
echo "================================================"

# 检查进程是否运行
if ps -p \$NEW_PID > /dev/null 2>&1; then
  echo "[SUCCESS] Application is running!"
  echo "[INFO] PID: \$NEW_PID"
  echo "[INFO] Port: \${APP_PORT}"
  echo "[INFO] View logs: tail -f logs/\${APP_NAME}.log"
else
  echo "[WARNING] Process check: Application process not found"
  echo "[INFO] This might be normal if the application starts quickly"
  echo "[INFO] Check logs for details: tail -n 50 logs/\${APP_NAME}.log"
fi`
  } else if (projectType === 'VUE') {
    formState.deployScript = `#!/bin/bash
# Vue项目部署脚本

APP_NAME="${name}"
DEPLOY_PATH="${deployPath}"
NGINX_ROOT="/usr/share/nginx/html"

# 创建部署目录
mkdir -p \${DEPLOY_PATH}

# 备份旧版本
if [ -d "\${NGINX_ROOT}/\${APP_NAME}" ]; then
  BACKUP_NAME="\${APP_NAME}_\$(date +%Y%m%d_%H%M%S)"
  mv \${NGINX_ROOT}/\${APP_NAME} \${DEPLOY_PATH}/backup/\${BACKUP_NAME}
  echo "已备份旧版本: \${BACKUP_NAME}"
fi

# 部署新版本
echo "部署新版本..."
cp -r dist \${NGINX_ROOT}/\${APP_NAME}

# 重载Nginx
nginx -s reload

echo "部署完成！访问地址: http://YOUR_SERVER/\${APP_NAME}"`
  }
}

// 上一步
const handlePrevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

// 下一步
const handleNextStep = async () => {
  // 简单的必填项检查
  let isValid = true
  let errorMsg = ''
  
  switch (currentStep.value) {
    case 1:
      if (!formState.name) {
        isValid = false
        errorMsg = '请输入项目名称'
      } else if (!formState.projectType) {
        isValid = false
        errorMsg = '请选择项目类型'
      } else if (!formState.env) {
        isValid = false
        errorMsg = '请选择环境'
      } else if (!formState.groupId) {
        isValid = false
        errorMsg = '请选择项目组'
      } else if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(formState.name)) {
        isValid = false
        errorMsg = '项目名称必须英文开头,只能包含字母、数字、连字符、下划线'
      }
      break
    case 2:
      if (!formState.gitUrl) {
        isValid = false
        errorMsg = '请输入Git地址'
      } else if (!formState.branch) {
        isValid = false
        errorMsg = '请输入分支'
      }
      break
    case 3:
      if (!formState.buildCommand) {
        isValid = false
        errorMsg = '请输入构建命令'
      } else if (!formState.buildDir) {
        isValid = false
        errorMsg = '请输入产物路径'
      }
      break
  }
  
  if (isValid) {
    currentStep.value++
  } else {
    message.warning(errorMsg)
  }
}

// 提交表单
const handleSubmit = async () => {
  // 验证第4步的必填项
  if (!formState.serverIds || formState.serverIds.length === 0) {
    message.warning('请选择部署服务器')
    return
  }
  
  try {
    submitLoading.value = true
    
    if (isEdit.value) {
      // 编辑模式
      const submitData = {
        ...formState,
        id: projectId.value
      }
      await updateProject(submitData)
      message.success('更新项目成功')
    } else {
      // 新增模式，验证项目组必填
      if (!formState.groupId) {
        message.warning('请选择项目组')
        return
      }
      await addProject(formState)
      message.success('创建项目成功')
    }
    
    router.push('/project')
  } catch (error) {
    if (error.message) {
      message.error(error.message)
    } else {
      message.error(isEdit.value ? '更新项目失败' : '创建项目失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 取消
const handleCancel = () => {
  router.push('/project')
}

// 返回列表
const handleBack = () => {
  router.push('/project')
}

// 加载项目详情
const loadProjectDetail = async () => {
  try {
    const response = await getProject(projectId.value)
    const project = response.project
    const serverIds = response.serverIds || []
    
    // 回显数据
    Object.assign(formState, {
      name: project.name || '',
      description: project.description || '',
      gitUrl: project.gitUrl || '',
      branch: project.branch || 'master',
      projectDir: project.projectDir || '',
      gitAccount: project.gitAccount || '',
      gitPassword: project.gitPassword || '',
      projectType: project.projectType || 'JAVA',
      env: project.env || 'development',
      groupId: project.groupId || null,
      buildCommand: project.buildCommand || '',
      buildDir: project.buildDir || '',
      serverIds: serverIds,
      autoDeploy: project.autoDeploy != null ? project.autoDeploy : 1,
      appPort: project.appPort != null ? project.appPort : 8080,
      deployPath: project.deployPath || '/www/wwwroot/',
      deployScript: project.deployScript || ''
    })
    
    // 更新构建命令和产物路径选项
    handleProjectTypeChange(project.projectType)
  } catch (error) {
    message.error(error.message || '加载项目详情失败')
    console.error(error)
  }
}

onMounted(async () => {
  // 判断是编辑还是新增
  if (route.params.id) {
    isEdit.value = true
    projectId.value = route.params.id
  }
  
  await loadServers()
  await loadProjectGroups()
  
  if (isEdit.value) {
    // 编辑模式：加载项目详情
    await loadProjectDetail()
  } else {
    // 新增模式：初始化默认值
    handleProjectTypeChange('JAVA')
  }
})
</script>

<style scoped>
.project-add-page {
  padding: 20px;
  height: calc(100vh - 120px);
  overflow: hidden;
}

.step-content {
  height: calc(100vh - 450px);
  min-height: 280px;
  max-height: 500px;
  padding: 16px 0;
  overflow-y: auto;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.n-step__title) {
  font-weight: 500;
}

:deep(.n-card__footer) {
  padding-top: 0;
}

:deep(.n-form-item-label) {
  font-weight: 500;
}

:deep(.n-steps) {
  padding: 0 40px;
}

:deep(.n-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.n-card__content) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

:deep(.n-form) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
