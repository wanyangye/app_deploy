<template>
  <div class="project-list">
    <div class="toolbar">
      <n-space vertical :size="16">
        <!-- 项目组筛选提示 -->
        <n-alert v-if="route.query.groupId" type="success" closable @close="clearGroupFilter">
          <template #icon>
            <n-icon><FolderOpenSharp /></n-icon>
          </template>
          当前显示项目组「{{ route.query.groupName || '加载中...' }}」下的项目
        </n-alert>

        <!-- 环境切换和搜索 -->
        <n-space>
          <n-input
            v-model:value="searchName"
            placeholder="搜索项目名称"
            clearable
            style="width: 250px"
            @keyup.enter="loadData"
          >
            <template #suffix>
              <n-button text @click="loadData">
                <n-icon><SearchSharp /></n-icon>
              </n-button>
            </template>
          </n-input>
          <n-button  type="primary" @click="router.push('/project/add')">
            <template #icon>
              <n-icon><AddSharp /></n-icon>
            </template>
            新增项目
          </n-button>
        </n-space>

        <!-- 环境筛选Tab -->
        <n-tabs v-model:value="currentEnv" type="line" @update:value="handleEnvChange">
          <n-tab name="all" tab="全部" />
          <n-tab
            v-for="env in envList"
            :key="env"
            :name="env"
            :tab="getEnvLabel(env)"
          />
        </n-tabs>
      </n-space>
    </div>

    <!-- 项目卡片列表 -->
    <n-spin :show="loading">
      <n-empty v-if="!loading && dataSource.length === 0" description="暂无项目数据" style="margin-top: 60px;" />

      <n-grid v-else :x-gap="16" :y-gap="16" :cols="3" responsive="screen">
        <n-gi v-for="project in dataSource" :key="project.id">
          <n-card :bordered="true" hoverable class="project-card">
            <!-- 卡片头部 -->
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <n-icon size="24" :color="project.projectType === 'JAVA' ? '#6db33f' : '#18a058'">
                    <LeafSharp v-if="project.projectType === 'JAVA'" />
                    <LogoVue v-else />
                  </n-icon>
                  <div>
                    <div style="font-size: 16px; font-weight: 600;">{{ project.name }}</div>
                    <div  style="font-size: 12px;color: grey">ID:&nbsp;{{project.id}}</div>
                    <n-text depth="3" style="font-size: 12px;">{{ project.description }}</n-text>
                  </div>
                </div>
                <n-tag :type="project.projectType === 'JAVA' ? 'info' : 'success'" size="small">
                  {{ project.projectType }}
                </n-tag>
              </div>
            </template>

            <!-- 卡片内容 -->
            <n-space vertical :size="12">

              <div class="project-info-item">
                <n-text depth="3">Git地址：</n-text>
                <n-text>{{ project.gitUrl }}</n-text>
              </div>
              <div class="project-info-item">
                <n-text depth="3">分支：</n-text>
                <n-tag size="small" type="warning">{{ project.branch }}</n-tag>
              </div>
              <div class="project-info-item">
                <n-text depth="3">环境：</n-text>
                <n-tag size="small" :type="getEnvType(project.env)">{{ getEnvLabel(project.env || 'development') }}</n-tag>
              </div>
              <div class="project-info-item">
                <n-text depth="3">创建时间：</n-text>
                <n-text>{{ project.createTime }}</n-text>
              </div>
              <div v-if="project.lastDeployTime" class="project-info-item">
                <n-text depth="3">最近部署：</n-text>
                <n-text type="success">{{ project.lastDeployTime }}</n-text>
              </div>
            </n-space>

            <!-- 卡片底部操作 -->
            <template #footer>
              <n-space justify="space-between">
                <!-- 左侧按钮组 -->
                <n-space :size="8">
                  <!-- 部署按钮 -->
                  <n-popconfirm
                    @positive-click="triggerBuildAction(project.id)"
                  >
                    <template #trigger>
                      <n-button secondary size="small" type="primary">
                        <template #icon><n-icon><PlayCircleSharp /></n-icon></template>
                        立即构建
                      </n-button>
                    </template>
                    确定要部署该项目吗？
                  </n-popconfirm>
                </n-space>

                <!-- 管理按钮组 -->
                <n-space  :size="6">
                  <n-button secondary size="small" @click="showSettingsModal(project)">
                    <template #icon><n-icon><SettingsSharp /></n-icon></template>
                    设置
                  </n-button>
                  <n-button secondary size="small" type="success" @click="router.push(`/project/edit/${project.id}`)">
                    <template #icon><n-icon><CreateSharp /></n-icon></template>
                  </n-button>
                  <n-popconfirm @positive-click="handleDelete(project.id)">
                    <template #trigger>
                      <n-button secondary size="small" type="error">
                        <template #icon><n-icon><TrashSharp /></n-icon></template>
                      </n-button>
                    </template>
                    确定要删除该项目吗？
                  </n-popconfirm>
                </n-space>
              </n-space>
            </template>
          </n-card>
        </n-gi>
      </n-grid>

      <!-- 分页 -->
      <div v-if="dataSource.length > 0" style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-count="Math.ceil(pagination.itemCount / pagination.pageSize)"
          :page-sizes="pagination.pageSizes"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-spin>

    <n-modal
      v-model:show="visible"
      :title="editId ? '编辑项目' : '新增项目'"
      preset="dialog"
      style="width: 900px"
      :show-icon="false"
    >
      <n-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        label-placement="left"
        label-width="auto"
      >
        <!-- 基础信息 -->
        <n-divider title-placement="left" style="margin-top: 0">基础信息</n-divider>
        <n-grid :cols="2" :x-gap="24">
          <n-form-item-gi label="项目名称" path="name">
            <n-input v-model:value="formState.name" placeholder="英文开头，只能包含字母、数字、连字符、下划线" />
          </n-form-item-gi>
          <n-form-item-gi label="项目类型" path="projectType">
            <n-select
              v-model:value="formState.projectType"
              :options="projectTypeOptions"
              placeholder="请选择项目类型"
              @update:value="handleProjectTypeChange"
            />
          </n-form-item-gi>
        </n-grid>

        <n-form-item label="项目描述" path="description">
          <n-input v-model:value="formState.description" type="textarea" :rows="2" placeholder="请输入项目描述" />
        </n-form-item>

        <!-- Git配置 -->
        <n-divider title-placement="left">Git配置</n-divider>
        <n-form-item label="Git地址" path="gitUrl">
          <n-input v-model:value="formState.gitUrl" placeholder="https://github.com/xxx/xxx.git" />
        </n-form-item>

        <n-grid :cols="2" :x-gap="24">
          <n-form-item-gi label="分支" path="branch">
            <n-input v-model:value="formState.branch" placeholder="master" />
          </n-form-item-gi>
          <n-form-item-gi label="环境" path="env">
            <n-select
              v-model:value="formState.env"
              :options="envOptions"
              tag
              filterable
              placeholder="请选择或自定义环境"
            />
          </n-form-item-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="24">
          <n-form-item-gi label="Git用户名" path="gitAccount">
            <n-input v-model:value="formState.gitAccount" placeholder="admin" />
          </n-form-item-gi>
          <n-form-item-gi label="Git密码" path="gitPassword">
            <n-input v-model:value="formState.gitPassword" type="password" show-password-on="click" placeholder="请输入Git密码" />
          </n-form-item-gi>
        </n-grid>

        <!-- 构建配置 -->
        <n-divider title-placement="left">构建配置</n-divider>
        <n-grid :cols="2" :x-gap="24">
          <n-form-item-gi label="构建命令" path="buildCommand">
            <n-select
              v-model:value="formState.buildCommand"
              :options="buildCommandOptions"
              tag
              filterable
              placeholder="请选择或自定义构建命令"
            />
          </n-form-item-gi>
          <n-form-item-gi label="产物路径" path="buildDir">
            <n-select
              v-model:value="formState.buildDir"
              :options="buildDirOptions"
              tag
              filterable
              placeholder="请选择或自定义产物路径"
            />
          </n-form-item-gi>
        </n-grid>

        <!-- 部署配置 -->
        <n-divider title-placement="left">部署配置</n-divider>
        <n-form-item label="部署服务器" path="serverIds">
          <n-select
            v-model:value="formState.serverIds"
            :options="serverOptions"
            multiple
            clearable
            placeholder="请选择部署服务器（可多选）"
          />
        </n-form-item>

        <n-grid :cols="2" :x-gap="24">
          <n-form-item-gi label="自动部署" path="autoDeploy">
            <n-radio-group v-model:value="formState.autoDeploy" @update:value="handleAutoDeployChange">
              <n-radio :value="1">是</n-radio>
              <n-radio :value="0">否</n-radio>
            </n-radio-group>
          </n-form-item-gi>
          <n-form-item-gi v-if="formState.autoDeploy === 1" label="应用端口" path="appPort">
            <n-input-number v-model:value="formState.appPort" :min="1" :max="65535" placeholder="8080" style="width: 100%" />
          </n-form-item-gi>
        </n-grid>

        <n-form-item v-if="formState.autoDeploy === 1" label="部署目录" path="deployPath">
          <n-input v-model:value="formState.deployPath" placeholder="/home/deploy/">
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
        </n-form-item>
      </n-form>

      <template #action>
        <n-space>
          <n-button @click="visible = false">取消</n-button>
          <n-button type="primary" @click="handleOk">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 成员管理弹窗 -->
    <n-modal v-model:show="memberVisible" title="项目成员管理" preset="dialog" style="width: 700px" :show-icon="false">
      <n-space vertical>
        <n-button type="primary" size="small" @click="addMemberRow">
          <template #icon><n-icon><AddSharp /></n-icon></template>
          添加成员
        </n-button>

        <n-data-table :columns="memberColumns" :data="projectMembers" :pagination="false" />
      </n-space>

      <template #action>
        <n-space>
          <n-button @click="memberVisible = false">取消</n-button>
          <n-button type="primary" @click="saveMember">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 版本回退弹窗 -->
    <n-modal v-model:show="rollbackVisible" title="选择要回退的版本" preset="dialog" style="width: 700px" :show-icon="false">
      <n-spin :show="loadingBackups">
        <n-empty v-if="!loadingBackups && backupList.length === 0" description="暂无备份版本" />

        <n-list v-else bordered>
          <n-list-item
            v-for="(backup, index) in backupList"
            :key="index"
            style="cursor: pointer;"
            :class="{ 'selected-backup': selectedBackup === backup.fileName }"
            @click="selectedBackup = backup.fileName"
          >
            <template #prefix>
              <n-radio :checked="selectedBackup === backup.fileName" />
            </template>
            <n-thing>
              <template #header>
                <n-text strong>{{ backup.fileName }}</n-text>
              </template>
              <template #description>
                <n-text depth="3">备份时间: {{ backup.date }}</n-text>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </n-spin>

      <template #action>
        <n-space>
          <n-button @click="rollbackVisible = false">取消</n-button>
          <n-button
            type="warning"
            :disabled="!selectedBackup"
            @click="handleRollback"
          >
            确认回退
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 项目设置弹窗 -->
    <n-modal
      v-model:show="settingsVisible"
      preset="card"
      :bordered="false"
      :segmented="{ content: true }"
      :style="settingsFullscreen ? 'width: 100vw; height: 100vh; max-width: 100vw;' : 'width: 900px;'"
      :content-style="settingsFullscreen ? 'height: calc(100vh - 60px);' : 'height: 520px;'"
      @after-leave="settingsFullscreen = false"
    >
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
          <span>项目设置[{{ currentSettingsProject?.name || '' }}]</span>
          <n-button text @click="settingsFullscreen = !settingsFullscreen" style="margin-right: 20px;">
            <template #icon>
              <n-icon size="18">
                <ContractSharp v-if="settingsFullscreen" />
                <ExpandSharp v-else />
              </n-icon>
            </template>
          </n-button>
        </div>
      </template>
      <div class="settings-container" :style="settingsFullscreen ? 'height: 100%;' : ''">
        <!-- 左侧菜单 -->
        <div class="settings-menu">
          <n-menu
            v-model:value="settingsActiveKey"
            :options="settingsMenuOptions"
            :indent="16"
          />
        </div>
        
        <!-- 右侧内容 -->
        <div class="settings-content">
          <!-- 项目日志 -->
          <div v-if="settingsActiveKey === 'logs'" class="settings-panel">
            <n-space vertical :size="12">
              <n-space justify="space-between">
                <n-text strong style="font-size: 16px;">运行日志</n-text>
                <n-space>
                  <n-select
                    v-model:value="selectedLogServer"
                    :options="projectServerOptions"
                    placeholder="选择服务器"
                    style="width: 180px;"
                    :disabled="logConnected"
                  />
                  <n-input
                    v-model:value="customLogPath"
                    placeholder="日志路径，如: /logs/app.log"
                    style="width: 200px;"
                    :disabled="logConnected"
                  />
                  <n-button 
                    v-if="!logConnected"
                    type="primary" 
                    size="small" 
                    :disabled="!selectedLogServer"
                    @click="startViewLog"
                  >
                    <template #icon><n-icon><PlayCircleSharp /></n-icon></template>
                    开始查看
                  </n-button>
                  <n-button 
                    v-else
                    type="error" 
                    size="small"
                    @click="stopViewLog"
                  >
                    <template #icon><n-icon><StopCircleSharp /></n-icon></template>
                    停止
                  </n-button>
                </n-space>
              </n-space>
              
              <n-alert v-if="projectServerOptions.length === 0" type="warning">
                该项目未绑定服务器，无法查看运行日志
              </n-alert>
              
              <div v-else class="log-console" ref="runLogRef" :style="settingsFullscreen ? 'height: calc(100vh - 200px);' : 'height: 380px;'">
                <pre>{{ runLogs || '点击"开始查看"连接服务器实时查看日志...' }}</pre>
              </div>
            </n-space>
          </div>

          <!-- 构建历史 -->
          <div v-else-if="settingsActiveKey === 'builds'" class="settings-panel">
            <n-space vertical :size="12">
              <n-space justify="space-between">
                <n-text strong style="font-size: 16px;">构建历史</n-text>
                <n-button size="small" @click="loadProjectBuilds">
                  <template #icon><n-icon><RefreshSharp /></n-icon></template>
                  刷新
                </n-button>
              </n-space>
              
              <n-spin :show="loadingBuilds">
                <n-empty v-if="!loadingBuilds && projectBuilds.length === 0" description="暂无构建记录" />
                
                <n-data-table
                  v-else
                  :columns="buildColumns"
                  :data="projectBuilds"
                  :pagination="false"
                  :max-height="settingsFullscreen ? 'calc(100vh - 220px)' : '380px'"
                  size="small"
                />
              </n-spin>
            </n-space>
          </div>

          <!-- 成员管理 -->
          <div v-else-if="settingsActiveKey === 'members'" class="settings-panel">
            <n-space vertical :size="12">
              <n-space justify="space-between">
                <n-text strong style="font-size: 16px;">项目成员</n-text>
                <n-button type="primary" size="small" @click="addMemberRow">
                  <template #icon><n-icon><AddSharp /></n-icon></template>
                  添加成员
                </n-button>
              </n-space>
              
              <n-data-table :columns="memberColumns" :data="projectMembers" :pagination="false" />
              
              <n-space justify="end">
                <n-button type="primary" @click="saveMember">保存成员</n-button>
              </n-space>
            </n-space>
          </div>

          <!-- 版本回退 -->
          <div v-else-if="settingsActiveKey === 'rollback'" class="settings-panel">
            <n-space vertical :size="12">
              <n-text strong style="font-size: 16px;">版本回退</n-text>
              
              <n-alert v-if="currentSettingsProject?.projectType !== 'JAVA'" type="info">
                版本回退仅支持 Java 项目
              </n-alert>
              
              <n-alert v-else-if="currentSettingsProject?.autoDeploy !== 1" type="info">
                请先开启自动部署功能
              </n-alert>
              
              <template v-else>
                <n-spin :show="loadingBackups">
                  <n-empty v-if="!loadingBackups && backupList.length === 0" description="暂无备份版本" />
                  
                  <n-list v-else bordered>
                    <n-list-item
                      v-for="(backup, index) in backupList"
                      :key="index"
                      style="cursor: pointer;"
                      :class="{ 'selected-backup': selectedBackup === backup.fileName }"
                      @click="selectedBackup = backup.fileName"
                    >
                      <template #prefix>
                        <n-radio :checked="selectedBackup === backup.fileName" />
                      </template>
                      <n-thing>
                        <template #header>
                          <n-text strong>{{ backup.fileName }}</n-text>
                        </template>
                        <template #description>
                          <n-text depth="3">备份时间: {{ backup.date }}</n-text>
                        </template>
                      </n-thing>
                    </n-list-item>
                  </n-list>
                </n-spin>
                
                <n-space justify="end">
                  <n-button
                    type="warning"
                    :disabled="!selectedBackup"
                    @click="handleRollback"
                  >
                    确认回退
                  </n-button>
                </n-space>
              </template>
            </n-space>
          </div>
        </div>
      </div>
    </n-modal>

    <!-- 日志查看弹窗 -->
    <n-modal
      v-model:show="logModalVisible"
      title="构建日志"
      style="width: 900px;"
      preset="card"
      :bordered="false"
      @after-leave="closeLogSocket"
    >
      <n-space vertical :size="12">
        <n-space justify="space-between">
          <n-space>
            <n-tag :type="getStatusType(currentBuild?.status)">{{ getStatusText(currentBuild?.status) }}</n-tag>
            <n-text>{{ currentBuild?.projectName }}</n-text>
            <n-text depth="3">#{{ currentBuild?.id }}</n-text>
          </n-space>
          <n-text depth="3">{{ currentBuild?.startTime }}</n-text>
        </n-space>
        
        <div class="log-console" ref="logConsoleRef">
          <pre>{{ buildLogs }}</pre>
        </div>
      </n-space>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, h, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, NButton, NSpace, NTag, NPopconfirm, NSelect, NPagination, NEmpty, NEllipsis, NList, NListItem, NThing, NRadio, NSpin, NTabs, NTab } from 'naive-ui'
import { SearchSharp, AddSharp, RocketSharp, CreateSharp, TrashSharp, PeopleSharp, PlayCircleSharp, LogoVue, LeafSharp, TimeSharp, SettingsSharp, DocumentTextSharp, RefreshSharp, StopCircleSharp, ExpandSharp, ContractSharp, FolderOpenSharp } from '@vicons/ionicons5'
import { getProjectList, getProject, addProject, updateProject, deleteProject, getProjectMembers, assignProjectMembers } from '@/api/project'
import { getServerList } from '@/api/server'
import { triggerBuild, getBackupList, rollbackBuild, getBuildList, getBuild } from '@/api/build'
import { getCurrentUser, getUserList } from '@/api/user'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const loading = ref(false)
const visible = ref(false)
const memberVisible = ref(false)
const rollbackVisible = ref(false)
const currentProjectId = ref(null)
const editId = ref(null)
const searchName = ref('')
const formRef = ref()

// 设置弹窗相关
const settingsVisible = ref(false)
const settingsActiveKey = ref('logs')
const currentSettingsProject = ref(null)
const projectBuilds = ref([])
const loadingBuilds = ref(false)
const settingsFullscreen = ref(false)

// 运行日志相关
const runLogs = ref('')
const runLogRef = ref()
const selectedLogServer = ref(null)
const projectServerOptions = ref([])
const logConnected = ref(false)
const customLogPath = ref('')
let runLogSocket = null

// 日志弹窗相关
const logModalVisible = ref(false)
const currentBuild = ref(null)
const buildLogs = ref('')
const logConsoleRef = ref()
let logSocket = null

// 设置菜单选项
const settingsMenuOptions = [
  { label: '项目日志', key: 'logs', icon: () => h('span', { style: 'margin-right: 8px;' }, '') },
  { label: '构建历史', key: 'builds', icon: () => h('span', { style: 'margin-right: 8px;' }, '') },
  { label: '成员管理', key: 'members', icon: () => h('span', { style: 'margin-right: 8px;' }, '') },
  { label: '版本回退', key: 'rollback', icon: () => h('span', { style: 'margin-right: 8px;' }, '') }
]

const dataSource = ref([])
const servers = ref([])
const userPermissions = ref([])
const allUsers = ref([])
const projectMembers = ref([])
const backupList = ref([])
const loadingBackups = ref(false)
const selectedBackup = ref(null)
const currentEnv = ref('all')
const envList = ref([])

// 构建历史表格列
const buildColumns = [

  {
    title: '构建人',
    key: 'triggerByName',
    width: 80,
    ellipsis: { tooltip: true }
  },
  {
    title: '环境',
    key: 'env',
    width: 100,
    render: (row) => h(NTag, { type: getEnvType(row.env), size: 'small' }, () => getEnvLabel(row.env || '-'))
  },
  {
    title: '构建时长',
    key: 'duration',
    width: 120,
    render: (row) => {
      if (!row.duration) return '-'
      return h(
          NTag,
        { depth: 3 },
        () => `${row.duration} s`
      )
    }
  },
  {
    title: '开始时间',
    key: 'startTime',
    width: 160
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => h(NTag, { type: getStatusType(row.status), size: 'small' }, () => getStatusText(row.status))
  },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: (row) => h(
      NButton,
      { text: true, type: 'primary', size: 'small', onClick: () => viewBuildLog(row) },
      () => '查看日志'
    )
  }
]

const pagination = reactive({
  page: 1,
  pageSize: 9,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [9, 18, 27]
})

const projectTypeOptions = [
  { label: 'Java', value: 'JAVA' },
  { label: 'Vue', value: 'VUE' }
]

// 构建命令选项（根据项目类型动态变化）
const buildCommandOptions = ref([
  { label: 'mvn clean package -DskipTests', value: 'mvn clean package -DskipTests' },
  { label: 'mvn clean package', value: 'mvn clean package' },
  { label: 'mvn clean install -DskipTests', value: 'mvn clean install -DskipTests' },
  { label: 'gradle build', value: 'gradle build' },
  { label: 'gradle build -x test', value: 'gradle build -x test' }
])

// 产物路径选项
const buildDirOptions = [
  { label: 'target/*.jar', value: 'target/*.jar' },
  { label: 'target/*.war', value: 'target/*.war' },
  { label: 'build/libs/*.jar', value: 'build/libs/*.jar' },
  { label: 'dist', value: 'dist' },
  { label: 'build', value: 'build' },
  { label: 'out', value: 'out' }
]

const serverOptions = ref([])

// 环境选项
const envOptions = ref([
  { label: '开发环境', value: 'development' },
  { label: '测试环境', value: 'test' },
  { label: '生产环境', value: 'production' }
])

// 权限计算
const isAdmin = computed(() => userPermissions.value.includes('ADMIN'))
const isProjectAdmin = computed(() =>
  userPermissions.value.includes('ADMIN') || userPermissions.value.includes('PROJECT_ADMIN')
)
const canManageProject = computed(() => isProjectAdmin.value)

const formState = reactive({
  name: '',
  description: '',
  gitUrl: '',
  branch: 'master',
  gitAccount: '',
  gitPassword: '',
  projectType: 'JAVA',
  buildCommand: '',
  buildDir: '',
  serverIds: [],
  autoDeploy: 0,
  deployScript: '',
  deployPath: '/home/deploy/',
  appPort: 8080,
  env: 'development'
})

const rules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { 
      pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/,
      message: '项目名称只能包含英文字母、数字、连字符和下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  gitUrl: { required: true, message: '请输入Git地址', trigger: 'blur' },
  projectType: { required: true, message: '请选择项目类型', trigger: 'change' },
  buildCommand: { required: true, message: '请输入构建命令', trigger: 'blur' },
  buildDir: { required: true, message: '请输入产物路径', trigger: 'blur' }
}

const loadData = async () => {
  try {
    loading.value = true

    const params = {
      current: pagination.page,
      size: pagination.pageSize
    }

    if (searchName.value) {
      params.name = searchName.value
    }

    if (currentEnv.value && currentEnv.value !== 'all') {
      params.env = currentEnv.value
    }

    // 支持按项目组筛选
    if (route.query.groupId) {
      params.groupId = route.query.groupId
    }

    const data = await getProjectList(params)

    dataSource.value = data.records
    pagination.itemCount = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadServers = async () => {
  try {
    const data = await getServerList({ current: 1, size: 100 })
    servers.value = data.records
    serverOptions.value = data.records.map(server => ({
      label: server.name,
      value: server.id
    }))
  } catch (error) {
    console.error(error)
  }
}

const handlePageChange = (page) => {
  pagination.page = page
  loadData()
}

const handlePageSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

// 清除项目组筛选
const clearGroupFilter = () => {
  router.push('/project')
}

const showModal = async (record) => {
  if (record) {
    editId.value = record.id

    // 获取项目详情和服务器ID列表
    try {
      const response = await getProject(record.id)
      const project = response.project
      const serverIds = response.serverIds || []

      Object.assign(formState, {
        name: project.name || '',
        description: project.description || '',
        gitUrl: project.gitUrl || '',
        branch: project.branch || 'master',
        gitAccount: project.gitAccount || '',
        gitPassword: project.gitPassword || '',
        projectType: project.projectType || 'JAVA',
        buildCommand: project.buildCommand || '',
        buildDir: project.buildDir || '',
        serverIds: serverIds,
        autoDeploy: project.autoDeploy != null ? project.autoDeploy : 0,
        deployScript: project.deployScript || generateDefaultDeployScript(project.projectType),
        deployPath: project.deployPath || '/home/deploy/',
        appPort: project.appPort || 8080,
        env: project.env || 'development'
      })
    } catch (error) {
      console.error('获取项目详情失败:', error)
      message.error(error.message || '获取项目详情失败')
    }
  } else {
    editId.value = null
    Object.assign(formState, {
      name: '',
      description: '',
      gitUrl: '',
      branch: 'master',
      gitAccount: '',
      gitPassword: '',
      projectType: 'JAVA',
      buildCommand: '',
      buildDir: '',
      serverIds: [],
      autoDeploy: 0,
      deployScript: generateDefaultDeployScript('JAVA'),
      deployPath: '/home/deploy/',
      appPort: 8080,
      env: 'development'
    })
  }
  visible.value = true
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()

    const submitData = {
      ...formState,
      serverIds: formState.serverIds || [],
      autoDeploy: formState.autoDeploy || 0,
      deployPath: formState.deployPath || '/home/deploy/',
      appPort: formState.appPort || 8080
    }

    if (editId.value) {
      submitData.id = editId.value
      await updateProject(submitData)
      message.success('更新成功')
    } else {
      await addProject(submitData)
      message.success('添加成功')
    }

    visible.value = false
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (id) => {
  try {
    await deleteProject(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const viewBuilds = (projectId) => {
  router.push(`/build/${projectId}`)
}

// 处理项目类型变化，动态更新构建命令选项和部署脚本
const handleProjectTypeChange = (value) => {
  if (value === 'JAVA') {
    buildCommandOptions.value = [
      { label: 'mvn clean package -DskipTests', value: 'mvn clean package -DskipTests' },
      { label: 'mvn clean package', value: 'mvn clean package' },
      { label: 'mvn clean install -DskipTests', value: 'mvn clean install -DskipTests' },
      { label: 'mvn clean install', value: 'mvn clean install' },
      { label: 'gradle build -x test', value: 'gradle build -x test' },
      { label: 'gradle build', value: 'gradle build' },
      { label: 'gradle bootJar', value: 'gradle bootJar' }
    ]
    // 如果当前构建命令为空，设置默认值
    if (!formState.buildCommand) {
      formState.buildCommand = 'mvn clean package -DskipTests'
    }
  } else if (value === 'VUE') {
    buildCommandOptions.value = [
      { label: 'npm install && npm run build', value: 'npm install && npm run build' },
      { label: 'npm run build', value: 'npm run build' },
      { label: 'yarn install && yarn build', value: 'yarn install && yarn build' },
      { label: 'yarn build', value: 'yarn build' },
      { label: 'pnpm install && pnpm build', value: 'pnpm install && pnpm build' },
      { label: 'pnpm build', value: 'pnpm build' }
    ]
    // 如果当前构建命令为空，设置默认值
    if (!formState.buildCommand) {
      formState.buildCommand = 'npm install && npm run build'
    }
  }

  // 更新部署脚本
  if (formState.autoDeploy === 1) {
    formState.deployScript = generateDefaultDeployScript(value)
  }
}

// 处理自动部署选项变化
const handleAutoDeployChange = (value) => {
  if (value === 1) {
    // 开启自动部署时，根据项目类型生成部署脚本
    formState.deployScript = generateDefaultDeployScript(formState.projectType)
  }
}

// 生成默认部署脚本（根据项目类型）
const generateDefaultDeployScript = (projectType) => {
  const type = projectType || formState.projectType

  if (type === 'VUE') {
    // Vue 项目部署脚本（直接上传静态文件）
    return `#!/bin/bash
# Vue 项目部署脚本

DEPLOY_DIR="{{uploadPath}}"
WEB_ROOT="/www/wwwroot"
PROJECT_NAME="{{projectName}}"
TARGET_DIR="\$WEB_ROOT/\$PROJECT_NAME"
BACKUP_DIR="\$WEB_ROOT/backup"

echo "========================================"
echo "开始部署Vue项目"
echo "========================================"

# 1. 备份旧版本
echo "[步骤1] 备份旧版本..."
if [ -d "\$TARGET_DIR" ]; then
  mkdir -p \$BACKUP_DIR
  BACKUP_NAME="\${PROJECT_NAME}_\$(date +%Y%m%d_%H%M%S)"
  mv \$TARGET_DIR \$BACKUP_DIR/\$BACKUP_NAME
  echo "已备份为: \$BACKUP_NAME"

  # 保留最近3个备份
  ls -t \$BACKUP_DIR | grep "^\${PROJECT_NAME}_" | tail -n +4 | xargs -I {} rm -rf \$BACKUP_DIR/{}
fi

# 2. 部署新版本
echo "[步骤2] 部署新版本..."
mkdir -p \$TARGET_DIR

# 检查是否为压缩包
if [ -f "\$DEPLOY_DIR/dist.zip" ]; then
  echo "检测到 dist.zip 文件"
  # 先解压到临时目录
  cd \$DEPLOY_DIR
  unzip -o -q dist.zip

  # 检查解压后的结构
  if [ -d "\$DEPLOY_DIR/dist" ]; then
    # 解压后有 dist 目录，复制其内容
    cp -r \$DEPLOY_DIR/dist/* \$TARGET_DIR/
    echo "已解压 dist.zip 并复制到 \$TARGET_DIR"
  else
    # 解压后直接是文件，移动到目标目录
    mv \$DEPLOY_DIR/* \$TARGET_DIR/ 2>/dev/null || true
    echo "已解压 dist.zip 到 \$TARGET_DIR"
  fi
elif [ -f "\$DEPLOY_DIR/dist.tar.gz" ]; then
  tar -xzf \$DEPLOY_DIR/dist.tar.gz -C \$DEPLOY_DIR
  cp -r \$DEPLOY_DIR/dist/* \$TARGET_DIR/
  echo "已解压 dist.tar.gz 到 \$TARGET_DIR"
elif [ -d "\$DEPLOY_DIR/dist" ]; then
  cp -r \$DEPLOY_DIR/dist/* \$TARGET_DIR/
  echo "已复制 dist 目录到 \$TARGET_DIR"
else
  echo "错误: 未找到构建产物（dist目录或压缩包）"
  exit 1
fi

# 3. 设置文件权限
echo "[步骤3] 设置文件权限..."
chmod -R 755 \$TARGET_DIR

echo "========================================"
echo "部署完成！"
echo "部署目录: \$TARGET_DIR"
echo "访问路径: http://your-domain/\$PROJECT_NAME"
echo "========================================"`
  } else {
    // Java 项目部署脚本（启动 jar 包）
    return `#!/bin/bash
# Java 项目部署脚本(注意：可以不用修改 直接使用)

APP_PORT={{appPort}}
DEPLOY_DIR="{{uploadPath}}"
LOG_FILE="\$DEPLOY_DIR/app.log"

echo "========================================"
echo "开始部署Java应用"
echo "========================================"

# 1. 停止旧进程
echo "[步骤1] 正在停止旧进程..."
PID=\$(lsof -t -i:\$APP_PORT 2>/dev/null)
if [ ! -z "\$PID" ]; then
  kill -15 \$PID
  sleep 3
  if ps -p \$PID > /dev/null 2>&1; then
    kill -9 \$PID
    echo "已强制停止进程: \$PID"
  else
    echo "已优雅停止进程: \$PID"
  fi
else
  echo "未找到运行中的进程"
fi

sleep 2

# 2. 查找新上传的jar（备份操作已在上传前完成）
echo "[步骤2] 查找jar文件..."
JAR_FILE=\$(ls -t \$DEPLOY_DIR/*.jar 2>/dev/null | grep -v '\\.bak\$' | head -n 1)

if [ -z "\$JAR_FILE" ] || [ ! -f "\$JAR_FILE" ]; then
  echo "错误: 未找到可用的jar文件"
  exit 1
fi

JAR_NAME=\$(basename "\$JAR_FILE")
echo "找到jar文件: \$JAR_NAME"

# 3. 启动新应用
echo "[步骤3] 正在启动应用..."
cd \$DEPLOY_DIR

# 清空旧日志
> \$LOG_FILE

# 后台启动应用
nohup java -jar -Xms512m -Xmx1024m -Dserver.port=\$APP_PORT "\$JAR_FILE" > \$LOG_FILE 2>&1 &
NEW_PID=\$!

echo "应用已启动，PID: \$NEW_PID"
echo "JAR文件: \$JAR_NAME"
echo "日志文件: \$LOG_FILE"

# 4. 等待应用启动并实时显示日志
echo "[步骤4] 等待应用启动..."
echo "----------------------------------------"
echo "应用启动日志："
echo "----------------------------------------"

# 实时显示日志并检测启动成功
START_TIME=\$(date +%s)
TIMEOUT=60
STARTED=false

# 使用 tail -f 实时显示日志，同时检测启动状态
(
  tail -f \$LOG_FILE &
  TAIL_PID=\$!

  while true; do
    CURRENT_TIME=\$(date +%s)
    ELAPSED=\$((CURRENT_TIME - START_TIME))

    # 检查超时
    if [ \$ELAPSED -gt \$TIMEOUT ]; then
      kill \$TAIL_PID 2>/dev/null
      echo ""
      echo "----------------------------------------"
      echo "警告: 应用启动超时（60秒）"
      echo "请检查日志文件: \$LOG_FILE"
      exit 1
    fi

    # 检查端口是否已监听
    if lsof -t -i:\$APP_PORT > /dev/null 2>&1; then
      sleep 2
      kill \$TAIL_PID 2>/dev/null
      STARTED=true
      break
    fi

    # 检查进程是否还在运行
    if ! ps -p \$NEW_PID > /dev/null 2>&1; then
      kill \$TAIL_PID 2>/dev/null
      echo ""
      echo "----------------------------------------"
      echo "错误: 应用进程已退出"
      echo "请检查日志文件: \$LOG_FILE"
      exit 1
    fi

    sleep 1
  done

  if [ "\$STARTED" = true ]; then
    echo ""
    echo "----------------------------------------"
    echo "应用启动成功！"
    echo "========================================"
    echo "部署完成！"
    echo "JAR文件: \$JAR_NAME"
    echo "应用端口: \$APP_PORT"
    echo "应用PID: \$NEW_PID"
    echo "日志文件: \$LOG_FILE"
    echo "========================================"
    exit 0
  fi
) || exit 1`
  }
}

const triggerBuildAction = async (projectId) => {
  try {
    const data = await triggerBuild(projectId)
    message.success('构建任务已创建，正在跳转...')
    // 跳转到构建详情页，实时查看构建日志
    router.push(`/build-detail/${data.buildId}`)
  } catch (error) {
    console.error(error)
    message.error('部署失败')
  }
}

const loadUserPermissions = async () => {
  try {
    const data = await getCurrentUser()
    userPermissions.value = data.permissions || []
  } catch (error) {
    console.error('加载用户权限失败', error)
  }
}

// 成员管理相关
const memberColumns = [
  {
    title: '用户',
    key: 'userId',
    render: (row, index) => {
      const userOptions = allUsers.value.map(u => ({
        label: `${u.nickname || u.username} (${u.username})`,
        value: u.id
      }))
      return h(
        NSelect,
        {
          value: row.userId,
          options: userOptions,
          placeholder: '请选择用户',
          style: { width: '250px' },
          onUpdateValue: (value) => {
            projectMembers.value[index].userId = value
          }
        }
      )
    }
  },
  {
    title: '角色',
    key: 'roleType',
    render: (row, index) => {
      const roleOptions = [
        { label: '开发者', value: 'DEVELOPER' },
        { label: '成员', value: 'MEMBER' }
      ]
      return h(
        NSelect,
        {
          value: row.roleType,
          options: roleOptions,
          style: { width: '120px' },
          onUpdateValue: (value) => {
            projectMembers.value[index].roleType = value
          }
        }
      )
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row, index) => {
      // 不能删除拥有者
      if (row.roleType === 'OWNER') {
        return h(NTag, { type: 'success' }, { default: () => '拥有者' })
      }
      return h(
        NButton,
        {
          text: true,
          type: 'error',
          size: 'small',
          onClick: () => removeMemberRow(index)
        },
        { default: () => '移除' }
      )
    }
  }
]

const showMemberModal = async (projectId) => {
  currentProjectId.value = projectId

  try {
    // 加载所有用户
    const userData = await getUserList({ current: 1, size: 100 })
    allUsers.value = userData.records

    // 加载项目成员
    const members = await getProjectMembers(projectId)
    projectMembers.value = members.map(m => ({
      userId: m.userId,
      roleType: m.roleType,
      user: m.user
    }))

    memberVisible.value = true
  } catch (error) {
    message.error('加载成员列表失败')
  }
}

const addMemberRow = () => {
  projectMembers.value.push({
    userId: null,
    roleType: 'MEMBER'
  })
}

const removeMemberRow = (index) => {
  projectMembers.value.splice(index, 1)
}

const saveMember = async () => {
  try {
    // 过滤掉拥有者和空的成员
    const members = projectMembers.value
      .filter(m => m.userId && m.roleType !== 'OWNER')
      .map(m => ({
        userId: m.userId,
        roleType: m.roleType
      }))

    await assignProjectMembers(currentProjectId.value, members)
    message.success('保存成功')
    memberVisible.value = false
    
    // 如果是在设置弹窗中，重新加载成员列表
    if (settingsVisible.value) {
      loadProjectMembers()
    }
  } catch (error) {
    message.error('保存失败')
  }
}

const showRollbackModal = async (projectId) => {
  currentProjectId.value = projectId
  selectedBackup.value = null
  rollbackVisible.value = true

  try {
    loadingBackups.value = true
    const data = await getBackupList(projectId)
    backupList.value = data || []
  } catch (error) {
    message.error('加载备份列表失败')
    console.error(error)
  } finally {
    loadingBackups.value = false
  }
}

const handleRollback = async () => {
  if (!selectedBackup.value) {
    message.error('请选择要回退的版本')
    return
  }

  try {
    const data = await rollbackBuild({
      projectId: currentProjectId.value,
      backupFileName: selectedBackup.value
    })
    message.success('回退任务已创建，正在跳转...')
    rollbackVisible.value = false
    settingsVisible.value = false
    // 跳转到构建详情页，实时查看回退日志
    router.push(`/build-detail/${data.buildId}`)
  } catch (error) {
    console.error(error)
    message.error('回退失败')
  }
}

// 加载环境列表
const loadEnvs = async () => {
  try {
    const tokenName = localStorage.getItem('tokenName') || 'Authorization'
    const response = await fetch('/api/project/envs', {
      headers: {
        [tokenName]: `${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.code === 200) {
      envList.value = result.data || []
    }
  } catch (error) {
    console.error('加载环境列表失败:', error)
  }
}

// 环境切换
const handleEnvChange = (env) => {
  currentEnv.value = env
  pagination.page = 1
  loadData()
}

// 获取环境标签
const getEnvLabel = (env) => {
  const envMap = {
    'development': '开发环境',
    'test': '测试环境',
    'production': '生产环境'
  }
  return envMap[env] || env
}

// 获取环境Tag类型
const getEnvType = (env) => {
  const typeMap = {
    'development': 'info',
    'test': 'warning',
    'production': 'error'
  }
  return typeMap[env] || 'default'
}

// 获取构建状态类型
const getStatusType = (status) => {
  const map = {
    'PENDING': 'default',
    'RUNNING': 'info',
    'SUCCESS': 'success',
    'FAILED': 'error'
  }
  return map[status] || 'default'
}

// 获取构建状态文本
const getStatusText = (status) => {
  const map = {
    'PENDING': '等待中',
    'RUNNING': '运行中',
    'SUCCESS': '成功',
    'FAILED': '失败'
  }
  return map[status] || status
}

// 打开设置弹窗
const showSettingsModal = async (project) => {
  currentSettingsProject.value = project
  currentProjectId.value = project.id
  settingsActiveKey.value = 'logs'
  settingsVisible.value = true
  
  // 重置日志状态
  runLogs.value = ''
  selectedLogServer.value = null
  logConnected.value = false
  stopViewLog()
  
  // 加载项目服务器列表
  await loadProjectServers(project.id)
  
  // 加载构建历史
  loadProjectBuilds()
  
  // 加载成员列表
  loadProjectMembers()
  
  // 加载备份列表
  if (project.projectType === 'JAVA' && project.autoDeploy === 1) {
    loadBackups()
  }
}

// 加载项目绑定的服务器列表
const loadProjectServers = async (projectId) => {
  try {
    const response = await getProject(projectId)
    const serverIds = response.serverIds || []
    
    // 筛选出项目绑定的服务器
    projectServerOptions.value = servers.value
      .filter(s => serverIds.includes(s.id))
      .map(s => ({
        label: s.name + ' (' + s.host + ')',
        value: s.id
      }))
    
    // 默认选中第一台
    if (projectServerOptions.value.length > 0) {
      selectedLogServer.value = projectServerOptions.value[0].value
    }
  } catch (error) {
    console.error('加载项目服务器失败:', error)
  }
}

// 加载项目构建列表
const loadProjectBuilds = async () => {
  if (!currentProjectId.value) return
  
  try {
    loadingBuilds.value = true
    const data = await getBuildList({ 
      projectId: currentProjectId.value,
      current: 1,
      size: 20
    })
    projectBuilds.value = data.records || []
  } catch (error) {
    console.error('加载构建列表失败:', error)
  } finally {
    loadingBuilds.value = false
  }
}

// 加载项目成员
const loadProjectMembers = async () => {
  if (!currentProjectId.value) return
  
  try {
    // 加载所有用户
    const userData = await getUserList({ current: 1, size: 100 })
    allUsers.value = userData.records
    
    // 加载项目成员
    const members = await getProjectMembers(currentProjectId.value)
    projectMembers.value = members.map(m => ({
      userId: m.userId,
      roleType: m.roleType,
      user: m.user
    }))
  } catch (error) {
    console.error('加载成员列表失败:', error)
  }
}

// 加载备份列表
const loadBackups = async () => {
  if (!currentProjectId.value) return
  
  try {
    loadingBackups.value = true
    selectedBackup.value = null
    const data = await getBackupList(currentProjectId.value)
    backupList.value = data || []
  } catch (error) {
    console.error('加载备份列表失败:', error)
  } finally {
    loadingBackups.value = false
  }
}

// 查看构建日志
const viewBuildLog = async (build) => {
  currentBuild.value = build
  buildLogs.value = ''
  logModalVisible.value = true
  
  // 加载已有日志
  try {
    const data = await getBuild(build.id)
    if (data.log) {
      buildLogs.value = data.log
    }
  } catch (error) {
    console.error('加载日志失败:', error)
  }
  
  // 如果正在运行，连接WebSocket实时查看
  if (build.status === 'RUNNING' || build.status === 'PENDING') {
    connectLogSocket(build.id)
  }
}

// 连接日志WebSocket
const connectLogSocket = (buildId) => {
  closeLogSocket()
  
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws/build/${buildId}`
  
  logSocket = new WebSocket(wsUrl)
  
  logSocket.onopen = () => {
    console.log('WebSocket连接已建立')
  }
  
  logSocket.onmessage = (event) => {
    buildLogs.value += event.data + '\n'
    
    nextTick(() => {
      if (logConsoleRef.value) {
        logConsoleRef.value.scrollTop = logConsoleRef.value.scrollHeight
      }
    })
  }
  
  logSocket.onerror = (error) => {
    console.error('WebSocket错误:', error)
  }
  
  logSocket.onclose = () => {
    console.log('WebSocket连接已关闭')
    // 刷新构建状态
    loadProjectBuilds()
  }
}

// 关闭日志WebSocket
const closeLogSocket = () => {
  if (logSocket) {
    logSocket.close()
    logSocket = null
  }
}

// 开始查看运行日志
const startViewLog = () => {
  if (!currentProjectId.value || !selectedLogServer.value) {
    message.warning('请选择服务器')
    return
  }
  
  runLogs.value = ''
  
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  let wsUrl = `${protocol}//${window.location.host}/ws/project/log/${currentProjectId.value}/${selectedLogServer.value}`
  
  // 如果有自定义日志路径，添加到查询参数
  if (customLogPath.value) {
    wsUrl += `?logPath=${encodeURIComponent(customLogPath.value)}`
  }
  
  runLogSocket = new WebSocket(wsUrl)
  
  runLogSocket.onopen = () => {
    console.log('运行日志WebSocket连接已建立')
    logConnected.value = true
  }
  
  runLogSocket.onmessage = (event) => {
    runLogs.value += event.data
    
    nextTick(() => {
      if (runLogRef.value) {
        runLogRef.value.scrollTop = runLogRef.value.scrollHeight
      }
    })
  }
  
  runLogSocket.onerror = (error) => {
    console.error('运行日志WebSocket错误:', error)
    message.error('日志连接失败')
    logConnected.value = false
  }
  
  runLogSocket.onclose = () => {
    console.log('运行日志WebSocket连接已关闭')
    logConnected.value = false
  }
}

// 停止查看运行日志
const stopViewLog = () => {
  if (runLogSocket) {
    runLogSocket.close()
    runLogSocket = null
  }
  logConnected.value = false
}

onMounted(() => {
  loadUserPermissions()
  loadData()
  loadServers()
  loadEnvs()
})

onUnmounted(() => {
  closeLogSocket()
  stopViewLog()
})
</script>

<style scoped>
.project-list {
  width: 100%;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
}

/* 项目卡片样式 */
.project-card {
  height: 100%;
  transition: all 0.3s;
  border-radius: 8px;
}

.project-info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.selected-backup {
  background-color: rgba(24, 160, 88, 0.1);
  border-left: 3px solid #18a058;
}

/* 设置弹窗样式 */
.settings-container {
  display: flex;
  height: 480px;
  margin: -12px;
}

.settings-menu {
  width: 160px;
  min-width: 160px;
  border-right: 1px solid var(--n-border-color);
  background-color: var(--n-color);
}

.settings-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.settings-panel {
  height: 100%;
}

/* 日志控制台样式 */
.log-console {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 4px;
  max-height: 500px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.log-console pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.log-console::-webkit-scrollbar {
  width: 8px;
}

.log-console::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 4px;
}

.log-console::-webkit-scrollbar-thumb:hover {
  background: #777;
}
</style>
