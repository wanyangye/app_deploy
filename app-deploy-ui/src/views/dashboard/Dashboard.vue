<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <n-grid :x-gap="16" :y-gap="16" :cols="4" responsive="screen">
      <n-gi>
        <n-card :bordered="false" class="stat-card" hoverable @click="router.push('/project')">
          <n-statistic label="项目总数" :value="stats.projectCount">
            <template #prefix>
              <n-icon size="32" color="#18a058">
                <FolderOpenSharp />
              </n-icon>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      
      <n-gi>
        <n-card :bordered="false" class="stat-card" hoverable @click="router.push('/server')">
          <n-statistic label="服务器总数" :value="stats.serverCount">
            <template #prefix>
              <n-icon size="32" color="#2080f0">
                <ServerSharp />
              </n-icon>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      
      <n-gi>
        <n-card :bordered="false" class="stat-card" hoverable @click="router.push('/build')">
          <n-statistic label="构建总数" :value="stats.buildCount">
            <template #prefix>
              <n-icon size="32" color="#f0a020">
                <RocketSharp />
              </n-icon>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      
      <n-gi>
        <n-card :bordered="false" class="stat-card">
          <n-statistic label="成功率">
            <template #prefix>
              <n-icon size="32" color="#18a058">
                <CheckmarkCircleSharp />
              </n-icon>
            </template>
            <template #default>
              {{ stats.successRate }}%
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
    </n-grid>


    <!-- 图表区域 -->
    <n-grid :x-gap="16" :y-gap="16" :cols="2" style="margin-top: 16px;" responsive="screen">
      <n-gi>
        <n-card title="构建趋势" :bordered="false">
          <div ref="buildTrendChart" style="width: 100%; height: 300px;"></div>
        </n-card>
      </n-gi>
      
      <n-gi>
        <n-card title="构建状态分布" :bordered="false">
          <div ref="buildStatusChart" style="width: 100%; height: 300px;"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 最近活动 -->
    <n-grid :x-gap="16" :y-gap="16" :cols="2" style="margin-top: 16px;" responsive="screen">
      <!-- 最近构建 -->
      <n-gi>
        <n-card title="最近构建" :bordered="false">

          <n-spin :show="buildLoading">
            <n-empty v-if="!buildLoading && recentBuilds.length === 0" description="暂无构建记录" size="small" />
            <n-list v-else hoverable clickable>
              <n-list-item v-for="build in recentBuilds" :key="build.id" @click="viewBuildDetail(build.id)">
                <n-space justify="space-between" align="center" style="width: 100%;">
                  <n-space align="center" :size="12">
                    <n-icon size="20" :color="getStatusColor(build.status)">
                      <RocketSharp />
                    </n-icon>
                    <div>
                      <n-ellipsis style="max-width: 300px;">
                        {{ build.projectName }}
                      </n-ellipsis>
                      <n-space :size="4" style="margin-top: 4px;">
                        <n-tag :type="getStatusType(build.status)" size="tiny">{{ getStatusText(build.status) }}</n-tag>
                        <n-text depth="3" style="font-size: 12px;">· {{ build.triggerByName }}</n-text>
                      </n-space>
                    </div>
                  </n-space>
                  <n-text depth="3" style="font-size: 12px; white-space: nowrap;">{{ build.startTime }}</n-text>
                </n-space>
              </n-list-item>
            </n-list>
          </n-spin>
        </n-card>
      </n-gi>
      
      <!-- 日志切换展示 -->
      <n-gi>
        <n-card :bordered="false">
          <template #header>
            <n-tabs v-model:value="logTabValue" type="line" @update:value="handleLogTabChange">
              <n-tab name="operation" tab="最近操作" />
              <n-tab name="login" tab="最近登录" />
            </n-tabs>
          </template>

          
          <!-- 操作日志 -->
          <div v-show="logTabValue === 'operation'">
            <n-spin :show="opLogLoading">
              <n-empty v-if="!opLogLoading && recentOpLogs.length === 0" description="暂无操作记录" size="small" />
              <n-list v-else hoverable clickable>
                <n-list-item v-for="log in recentOpLogs" :key="log.id">
                  <n-space justify="space-between" align="center" style="width: 100%;">
                    <n-space align="center" :size="12">
                      <n-icon size="20" :color="log.status === 1 ? '#18a058' : '#d03050'">
                        <CheckmarkCircleSharp v-if="log.status === 1" />
                        <CloseCircleSharp v-else />
                      </n-icon>
                      <div>
                        <n-ellipsis style="max-width: 280px;">
                          {{ log.module }} - {{ log.operationType }}
                        </n-ellipsis>
                        <n-space :size="4" style="margin-top: 4px;">
                          <n-text depth="3" style="font-size: 12px;">{{ log.username }}</n-text>
                          <n-text depth="3" style="font-size: 12px;">· {{ log.description }}</n-text>
                        </n-space>
                      </div>
                    </n-space>
                    <n-text depth="3" style="font-size: 12px; white-space: nowrap;">{{ log.operationTime }}</n-text>
                  </n-space>
                </n-list-item>
              </n-list>
            </n-spin>
          </div>
          
          <!-- 登录日志 -->
          <div v-show="logTabValue === 'login'">
            <n-spin :show="loginLogLoading">
              <n-empty v-if="!loginLogLoading && recentLoginLogs.length === 0" description="暂无登录记录" size="small" />
              <n-list v-else hoverable clickable>
                <n-list-item v-for="log in recentLoginLogs" :key="log.id">
                  <n-space justify="space-between" align="center" style="width: 100%;">
                    <n-space align="center" :size="12">
                      <n-icon size="20" :color="log.status === 1 ? '#18a058' : '#d03050'">
                        <LogInSharp v-if="log.status === 1" />
                        <LogOutSharp v-else />
                      </n-icon>
                      <div>
                        <n-ellipsis style="max-width: 280px;">
                          {{ log.username }}
                        </n-ellipsis>
                        <n-space :size="4" style="margin-top: 4px;">
                          <n-tag :type="log.status === 1 ? 'success' : 'error'" size="tiny">{{ log.status === 1 ? '成功' : '失败' }}</n-tag>
                          <n-text depth="3" style="font-size: 12px;">· {{ log.ipAddress }}</n-text>
                        </n-space>
                      </div>
                    </n-space>
                    <n-text depth="3" style="font-size: 12px; white-space: nowrap;">{{ log.loginTime }}</n-text>
                  </n-space>
                </n-list-item>
              </n-list>
            </n-spin>
          </div>
        </n-card>
      </n-gi>
    </n-grid>
    
    <!-- 项目信息 -->
    <n-card :bordered="false" style="margin-top: 16px;">
      <n-grid :x-gap="24" :y-gap="16" :cols="3" responsive="screen">
        <n-gi>
          <div class="info-section">
            <n-icon size="40" color="#18a058" style="margin-bottom: 12px;">
              <PersonSharp />
            </n-icon>
             <h3>开源作者</h3>
                        <n-divider style="margin: 12px 0" />
                        <n-space vertical size="small">
                          <n-text strong style="font-size: 16px; color: #18a058;">Steven</n-text>
                          <n-text depth="3" style="font-size: 13px;">• 技术博主</n-text>
                          <n-text depth="3" style="font-size: 13px;">• 全栈 Java/Python 开发</n-text>
                          <n-text depth="3" style="font-size: 13px;">• 技术专家</n-text>
                        </n-space>

          </div>
        </n-gi>
        
        <n-gi>
          <div class="info-section">
            <n-icon size="40" color="#2080f0" style="margin-bottom: 12px;">
              <CubeSharp />
            </n-icon>
            <h3>项目功能</h3>
            <n-divider style="margin: 12px 0" />
            <n-space vertical size="small">
              <n-text depth="3" style="font-size: 13px;">• 项目管理（Git配置、构建命令）</n-text>
              <n-text depth="3" style="font-size: 13px;">• 一键触发构建与部署</n-text>
              <n-text depth="3" style="font-size: 13px;">• 实时构建日志推送</n-text>
              <n-text depth="3" style="font-size: 13px;">• 服务器SSH管理与连接测试</n-text>
              <n-text depth="3" style="font-size: 13px;">• 自动化部署流程</n-text>
            </n-space>
          </div>
        </n-gi>
        
        <n-gi>
          <div class="info-section">
            <n-icon size="40" color="#f0a020" style="margin-bottom: 12px;">
              <CodeSlashSharp />
            </n-icon>
            <h3>技术栈</h3>
            <n-divider style="margin: 12px 0" />
            <n-space vertical size="small">
              <n-text depth="3" style="font-size: 13px;"><strong>后端:</strong> Spring Boot 3 + MyBatis-Plus</n-text>
              <n-text depth="3" style="font-size: 13px;"><strong>前端:</strong> Vue3 + Vite + Naive UI</n-text>
              <n-text depth="3" style="font-size: 13px;"><strong>数据库:</strong> MySQL 8.0</n-text>
              <n-text depth="3" style="font-size: 13px;"><strong>工具:</strong> JGit、JSch、ECharts</n-text>
            </n-space>
          </div>
        </n-gi>
      </n-grid>
    </n-card>
    
    <!-- 更新日志 -->
    <n-card title="更新日志" :bordered="false" style="margin-top: 16px;">
      <n-timeline>
        <n-timeline-item
          type="success"
          title="v1.0.0"
          time="2026-06-08"
        >
          <template #default>
            <n-space vertical size="small" style="margin-top: 8px;">
              <n-text depth="2">• 项目管理功能：支持 Java/Vue 项目配置、Git 仓库集成</n-text>
              <n-text depth="2">• 一键构建部署：支持 Maven/Gradle/npm 等构建工具</n-text>
              <n-text depth="2">• 实时构建日志：WebSocket 实时推送构建日志</n-text>
              <n-text depth="2">• 服务器管理：SSH 连接测试、SFTP 文件上传</n-text>
              <n-text depth="2">• 权限管理：基于角色的权限控制系统</n-text>
              <n-text depth="2">• 数据可视化：构建趋势图、状态分布图</n-text>
              <n-text depth="2">• 主题切换：支持亮色/暗黑模式自由切换，优化视觉体验</n-text>
              <n-text depth="2">• 插件市场：一键安装基础环境（Git、JDK、Node.js、Docker、Python、Maven）</n-text>
              <n-text depth="2">• 插件市场：一键安装中间件（Nginx、MySQL、Redis、MongoDB）</n-text>
              <n-text depth="2">• 实时日志：插件安装/卸载过程 WebSocket 实时日志推送</n-text>
              <n-text depth="2">• 失败重试：支持安装失败后重新安装，自动清理失败记录</n-text>
              <n-text depth="2">• 文件管理：支持服务器文件浏览、上传、下载、删除等操作</n-text>
              <n-text depth="2">• 服务器监控：新增服务器监控面板，实时查看 CPU、内存、磁盘、网络等统计信息</n-text>
              <n-text depth="2">• 可视化图表：监控面板采用 ECharts 仪表盘图表，数据展示更直观</n-text>
              <n-text depth="2">• 全屏模式：SSH 控制台、监控面板和项目设置弹窗均支持全屏展示</n-text>
              <n-text depth="2">• 交互优化：监控面板支持手动刷新，操作更灵活</n-text>
              <n-text depth="2">• 默认SQLite：开箱即用，无需配置MySQL，轻量化部署更简单</n-text>
              <n-text depth="2">• 多环境部署：支持开发、测试、生产等多环境分项目管理与部署</n-text>
              <n-text depth="2">• 版本回退：Java项目支持一键回退到任意历史版本，弹窗选择备份文件后执行</n-text>
              <n-text depth="2">• 自动备份：每次部署自动备份jar包，使用时间戳命名（yyyy-MM-dd_HH_mm_ss）</n-text>
              <n-text depth="2">• 备份管理：自动保留最近3个备份，超出自动清理旧备份</n-text>
              <n-text depth="2">• 回退流程：停止应用 → 恢复备份 → 启动服务 → 实时检测端口</n-text>
              <n-text depth="2">• 多服务器支持：回退操作同时应用到所有配置的服务器</n-text>
              <n-text depth="2">• 项目设置：新增统一设置弹窗，整合运行日志、构建历史、成员管理、版本回退功能</n-text>
              <n-text depth="2">• 实时日志：支持选择多台服务器查看运行日志，自定义日志文件路径</n-text>
              <n-text depth="2">• 构建历史：在项目设置中查看构建记录，支持查看构建日志</n-text>
              <n-text depth="2">• 防重提交：新增、修改接口添加防重复提交保护</n-text>
              <n-text depth="2">• 项目名称：项目名称必须为英文，不允许重复</n-text>
              <n-text depth="2">• 项目组管理：新增项目组功能，支持按组织架构分组管理项目</n-text>
              <n-text depth="2">• 项目组字段：包含名称、描述、负责人、负责人联系方式等核心字段</n-text>
              <n-text depth="2">• 项目归属：创建/编辑项目时可选择所属项目组，支持项目分组展示</n-text>
              <n-text depth="2">• 项目筛选：从项目组卡片直接跳转，自动筛选显示该组下所有项目</n-text>
              <n-text depth="2">• 统计功能：项目组卡片实时显示包含的项目数量</n-text>
              <n-text depth="2">• 卡片式布局：项目组列表采用卡片式设计，支持搜索和分页</n-text>
              <n-text depth="2">• 删除保护：只能删除没有关联项目的项目组，确保数据完整性</n-text>
            </n-space>
          </template>
        </n-timeline-item>
        
        <n-timeline-item
          type="success"
          title="v1.4.0"
          time="2025-12-05"
        >
          <template #default>
            <n-space vertical size="small" style="margin-top: 8px;">
              <n-text depth="2">• 项目设置：新增统一设置弹窗，整合运行日志、构建历史、成员管理、版本回退功能</n-text>
              <n-text depth="2">• 实时日志：支持选择多台服务器查看运行日志，自定义日志文件路径</n-text>
              <n-text depth="2">• 构建历史：在项目设置中查看构建记录，支持查看构建日志</n-text>
              <n-text depth="2">• 全屏模式：项目设置弹窗支持全屏展示，查看日志更方便</n-text>
              <n-text depth="2">• 防重提交：新增、修改接口添加防重复提交保护</n-text>
              <n-text depth="2">• 项目名称：项目名称必须为英文，不允许重复</n-text>
            </n-space>
          </template>
        </n-timeline-item>
        
        <n-timeline-item
          type="warning"
          title="v1.3.0"
          time="2025-12-02"
        >
          <template #default>
            <n-space vertical size="small" style="margin-top: 8px;">
              <n-text depth="2">• 默认SQLite：开箱即用，无需配置MySQL，轻量化部署更简单</n-text>
              <n-text depth="2">• 多环境部署：支持开发、测试、生产等多环境分项目管理与部署</n-text>
              <n-text depth="2">• 版本回退：Java项目支持一键回退到任意历史版本，弹窗选择备份文件后执行</n-text>
              <n-text depth="2">• 自动备份：每次部署自动备份jar包，使用时间戳命名（yyyy-MM-dd_HH_mm_ss）</n-text>
              <n-text depth="2">• 备份管理：自动保留最近3个备份，超出自动清理旧备份</n-text>
              <n-text depth="2">• 回退流程：停止应用 → 恢复备份 → 启动服务 → 实时检测端口</n-text>
              <n-text depth="2">• 多服务器支持：回退操作同时应用到所有配置的服务器</n-text>
            </n-space>
          </template>
        </n-timeline-item>
        
        <n-timeline-item
          type="warning"
          title="v1.2.0"
          time="2025-12-02"
        >
          <template #default>
            <n-space vertical size="small" style="margin-top: 8px;">
              <n-text depth="2">• 文件管理：支持服务器文件浏览、上传、下载、删除等操作</n-text>
              <n-text depth="2">• 服务器监控：新增服务器监控面板，实时查看 CPU、内存、磁盘、网络等统计信息</n-text>
              <n-text depth="2">• 可视化图表：监控面板采用 ECharts 仪表盘图表，数据展示更直观</n-text>
              <n-text depth="2">• 全屏模式：SSH 控制台和监控面板均支持全屏展示</n-text>
              <n-text depth="2">• 交互优化：监控面板支持手动刷新，操作更灵活</n-text>
            </n-space>
          </template>
        </n-timeline-item>
        
        <n-timeline-item
          type="info"
          title="v1.1.0"
          time="2025-12-01"
        >
          <template #default>
            <n-space vertical size="small" style="margin-top: 8px;">
              <n-text depth="2">• 主题切换：支持亮色/暗黑模式自由切换，优化视觉体验</n-text>
              <n-text depth="2">• 插件市场：一键安装基础环境（Git、JDK、Node.js、Docker、Python、Maven）</n-text>
              <n-text depth="2">• 插件市场：一键安装中间件（Nginx、MySQL、Redis、MongoDB）</n-text>
              <n-text depth="2">• 实时日志：插件安装/卸载过程 WebSocket 实时日志推送</n-text>
              <n-text depth="2">• 失败重试：支持安装失败后重新安装，自动清理失败记录</n-text>
            </n-space>
          </template>
        </n-timeline-item>
        
        <n-timeline-item
          type="success"
          title="v1.0.0"
          time="2025-11-20"
        >
          <template #default>
            <n-space vertical size="small" style="margin-top: 8px;">
              <n-text depth="2">• 项目管理功能：支持 Java/Vue 项目配置、Git 仓库集成</n-text>
              <n-text depth="2">• 一键构建部署：支持 Maven/Gradle/npm 等构建工具</n-text>
              <n-text depth="2">• 实时构建日志：WebSocket 实时推送构建日志</n-text>
              <n-text depth="2">• 服务器管理：SSH 连接测试、SFTP 文件上传</n-text>
              <n-text depth="2">• 权限管理：基于角色的权限控制系统</n-text>
              <n-text depth="2">• 数据可视化：构建趋势图、状态分布图</n-text>
            </n-space>
          </template>
        </n-timeline-item>
      </n-timeline>
    </n-card>

    
    <!-- 底部作者信息 -->
    <div class="footer-author">
      <n-text depth="3" style="font-size: 12px;">
        开源作者：Steven
      </n-text>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NTag, NButton } from 'naive-ui'
import {
  FolderOpenSharp,
  ServerSharp,
  RocketSharp,
  CheckmarkCircleSharp,
  CloseCircleSharp,
  LogInSharp,
  LogOutSharp,
  PersonSharp,
  CubeSharp,
  CodeSlashSharp,
  CafeSharp,
  ExtensionPuzzleSharp
} from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { getBuildList, getStats, getBuildTrend, getStatusDistribution } from '@/api/build'
import { getOperationLogList } from '@/api/log'
import { getLoginLogList } from '@/api/log'

const router = useRouter()
const loading = ref(false)
const buildLoading = ref(false)
const opLogLoading = ref(false)
const loginLogLoading = ref(false)
const buildTrendChart = ref(null)
const buildStatusChart = ref(null)

const stats = ref({
  projectCount: 0,
  serverCount: 0,
  buildCount: 0,
  successRate: 0
})

const recentBuilds = ref([])
const recentOpLogs = ref([])
const recentLoginLogs = ref([])
const logTabValue = ref('operation') // 日志tab默认显示操作日志

const getStatusType = (status) => {
  const map = {
    'PENDING': 'default',
    'RUNNING': 'info',
    'SUCCESS': 'success',
    'FAILED': 'error'
  }
  return map[status] || 'default'
}

const getStatusText = (status) => {
  const map = {
    'PENDING': '等待中',
    'RUNNING': '运行中',
    'SUCCESS': '成功',
    'FAILED': '失败'
  }
  return map[status] || status
}

const viewDetail = (id) => {
  if (!id) {
    console.error('构建 ID不存在')
    return
  }
  router.push(`/build-detail/${id}`)
}

const viewBuildDetail = (id) => {
  if (!id) {
    console.error('构建 ID不存在')
    return
  }
  router.push(`/build-detail/${id}`)
}

const getStatusColor = (status) => {
  const map = {
    'PENDING': '#f0a020',
    'RUNNING': '#2080f0',
    'SUCCESS': '#18a058',
    'FAILED': '#d03050'
  }
  return map[status] || '#999'
}

// 初始化构建趋势图表
const initBuildTrendChart = async () => {
  if (!buildTrendChart.value) return
  
  try {
    // 从后端获取数据
    const trendData = await getBuildTrend()
    
    const chart = echarts.init(buildTrendChart.value)
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['成功', '失败']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: trendData.dates || []
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '成功',
          type: 'line',
          smooth: true,
          data: trendData.successData || [],
          itemStyle: { color: '#18a058' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(24, 160, 88, 0.3)' },
              { offset: 1, color: 'rgba(24, 160, 88, 0.05)' }
            ])
          }
        },
        {
          name: '失败',
          type: 'line',
          smooth: true,
          data: trendData.failedData || [],
          itemStyle: { color: '#d03050' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(208, 48, 80, 0.3)' },
              { offset: 1, color: 'rgba(208, 48, 80, 0.05)' }
            ])
          }
        }
      ]
    }
    chart.setOption(option)
    
    // 响应式
    window.addEventListener('resize', () => chart.resize())
  } catch (error) {
    console.error('加载构建趋势数据失败:', error)
  }
}

// 初始化构建状态分布图表
const initBuildStatusChart = async () => {
  if (!buildStatusChart.value) return
  
  try {
    // 从后端获取数据
    const statusData = await getStatusDistribution()
    
    // 颜色映射
    const colorMap = {
      '成功': '#18a058',
      '失败': '#d03050',
      '运行中': '#2080f0',
      '等待中': '#f0a020'
    }
    
    // 为每个数据项添加颜色
    const chartData = statusData.map(item => ({
      ...item,
      itemStyle: { color: colorMap[item.name] || '#999' }
    }))
    
    const chart = echarts.init(buildStatusChart.value)
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: '10%',
        top: 'center'
      },
      series: [
        {
          name: '构建状态',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: chartData
        }
      ]
    }
    chart.setOption(option)
    
    // 响应式
    window.addEventListener('resize', () => chart.resize())
  } catch (error) {
    console.error('加载构建状态分布数据失败:', error)
  }
}

const loadData = async () => {
  try {
    loading.value = true
    
    // 加载统计数据
    const statsData = await getStats()
    
    // 设置统计数据
    stats.value.projectCount = statsData.projectCount || 0
    stats.value.serverCount = statsData.serverCount || 0
    stats.value.buildCount = statsData.buildCount || 0
    stats.value.successRate = statsData.successRate || 0
    
    // 初始化图表
    setTimeout(() => {
      initBuildTrendChart()
      initBuildStatusChart()
    }, 100)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 加载最近构建
const loadRecentBuilds = async () => {
  try {
    buildLoading.value = true
    const buildData = await getBuildList({ current: 1, size: 5 })
    recentBuilds.value = buildData.records || []
  } catch (error) {
    console.error('加载构建记录失败:', error)
  } finally {
    buildLoading.value = false
  }
}

// 加载最近操作日志
const loadRecentOpLogs = async () => {
  try {
    opLogLoading.value = true
    const logData = await getOperationLogList({ current: 1, size: 5 })
    recentOpLogs.value = logData.records || []
  } catch (error) {
    console.error('加载操作日志失败:', error)
  } finally {
    opLogLoading.value = false
  }
}

// 加载最近登录日志
const loadRecentLoginLogs = async () => {
  try {
    loginLogLoading.value = true
    const logData = await getLoginLogList({ current: 1, size: 5 })
    recentLoginLogs.value = logData.records || []
  } catch (error) {
    console.error('加载登录日志失败:', error)
  } finally {
    loginLogLoading.value = false
  }
}

// 处理日志tab切换
const handleLogTabChange = (value) => {
  // tab切换时可以重新加载数据
  if (value === 'operation' && recentOpLogs.value.length === 0) {
    loadRecentOpLogs()
  } else if (value === 'login' && recentLoginLogs.value.length === 0) {
    loadRecentLoginLogs()
  }
}

onMounted(() => {
  loadData()
  loadRecentBuilds()
  loadRecentOpLogs()
  loadRecentLoginLogs()
})
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.stat-card {
  transition: all 0.3s;
}

.stat-card:hover {
  cursor: pointer;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 快捷入口卡片 */
.shortcut-card {
  cursor: pointer;
  transition: all 0.3s;
}

.shortcut-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.n-statistic) {
  display: flex;
  align-items: center;
  gap: 16px;
}

:deep(.n-statistic-value__prefix) {
  margin-right: 0;
}

.info-section {
  text-align: center;
  padding: 20px;
  border-radius: 8px;
  transition: all 0.3s;
}

.info-section:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.info-section h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.info-section :deep(.n-space) {
  text-align: left;
}

/* 打赏区域样式 */
.donate-section {
  text-align: center;
  padding: 20px;
}

.donate-header {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.donate-qrcode {
  display: flex;
  justify-content: center;
  gap: 60px;
  margin: 20px 0;
}

.qrcode-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s;
}

.qrcode-item:hover {
  transform: translateY(-4px);
}

.qrcode-img {
  width: 50%;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}



.donate-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

/* 底部作者信息 */
.footer-author {
  text-align: center;
  padding: 24px 0;
  margin-top: 16px;
}
</style>
