<template>
  <div class="file-manager">
    <!-- 上传进度提示 -->
    <n-alert v-if="isUploading" type="success" style="margin-bottom: 16px">
      <template #icon>
        <n-icon :component="CloudUploadOutline" />
      </template>
      {{ uploadingInfo }}
      <n-progress
        type="line"
        :percentage="uploadProgress"
        :show-indicator="false"
        style="margin-top: 8px"
      />
    </n-alert>

    <!-- 下载进度提示 -->
    <n-alert v-if="isDownloading" type="info" style="margin-bottom: 16px">
      <template #icon>
        <n-icon :component="DownloadOutline" />
      </template>
      正在下载: {{ downloadingFileName }} - {{ downloadProgress }}%
      <n-progress
        type="line"
        :percentage="downloadProgress"
        :show-indicator="false"
        style="margin-top: 8px"
      />
    </n-alert>

    <!-- 服务器选择 -->
    <n-card title="文件管理" :bordered="false">
      <template #header-extra>
        <n-select
          v-model:value="selectedServerId"
          :options="serverOptions"
          placeholder="请选择服务器"
          style="width: 200px"
          @update:value="handleServerChange"
        />
      </template>

      <div v-if="selectedServerId">
        <!-- 路径导航 -->
        <n-space style="margin-bottom: 16px">
          <n-breadcrumb>
            <n-breadcrumb-item @click="navigateTo('/')">
              <n-icon :component="HomeOutline" />
            </n-breadcrumb-item>
            <n-breadcrumb-item
              v-for="(part, index) in pathParts"
              :key="index"
              @click="navigateToPath(index)"
              style="cursor: pointer"
            >
              {{ part }}
            </n-breadcrumb-item>
          </n-breadcrumb>
        </n-space>

        <!-- 操作按钮 -->
        <n-space style="margin-bottom: 16px">
          <n-button type="primary" @click="handleUpload" :disabled="isUploading || isDownloading">
            <template #icon>
              <n-icon :component="CloudUploadOutline" />
            </template>
            上传文件
          </n-button>
          <n-button type="primary" @click="handleUploadFolder" :disabled="isUploading || isDownloading">
            <template #icon>
              <n-icon :component="FolderOpenSharp" />
            </template>
            上传文件夹
          </n-button>
          <n-button @click="showCreateDirModal = true" :disabled="isUploading || isDownloading">
            <template #icon>
              <n-icon :component="FolderOpenSharp" />
            </template>
            新建文件夹
          </n-button>
          <n-button @click="loadFiles" :disabled="isUploading || isDownloading">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
            刷新
          </n-button>
        </n-space>

        <!-- 文件列表 -->
        <n-data-table
          :columns="columns"
          :data="fileList"
          :loading="loading"
          :pagination="false"
        />
      </div>

      <n-empty
        v-else
        description="请先选择服务器"
        style="margin-top: 60px"
      />
    </n-card>

    <!-- 上传文件弹窗 -->
    <input
      ref="fileInput"
      type="file"
      style="display: none"
      @change="handleFileChange"
    />

    <!-- 上传文件夹弹窗 -->
    <input
      ref="folderInput"
      type="file"
      webkitdirectory
      directory
      multiple
      style="display: none"
      @change="handleFolderChange"
    />

    <!-- 新建文件夹弹窗 -->
    <n-modal
      v-model:show="showCreateDirModal"
      preset="dialog"
      title="新建文件夹"
      positive-text="确定"
      negative-text="取消"
      @positive-click="handleCreateDir"
    >
      <n-form style="margin-top: 16px">
        <n-form-item label="文件夹名称">
          <n-input v-model:value="newDirName" placeholder="请输入文件夹名称" />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { NButton, NIcon, NTag, NProgress, useMessage, useDialog } from 'naive-ui'
import {
  FolderOutline,
  DocumentOutline,
  CloudUploadOutline,
  RefreshOutline,
  TrashOutline,
  DownloadOutline,
  HomeOutline,
  FolderOpenSharp
} from '@vicons/ionicons5'
import { listFiles, uploadFile, uploadBatch, downloadFile, deleteFile, createDirectory } from '@/api/file'
import { getServerList } from '@/api/server'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const selectedServerId = ref(null)
const serverOptions = ref([])
const currentPath = ref('/')
const fileList = ref([])
const fileInput = ref(null)
const folderInput = ref(null)
const showCreateDirModal = ref(false)
const newDirName = ref('')
const downloadProgress = ref(0)
const isDownloading = ref(false)
const downloadingFileName = ref('')
const uploadProgress = ref(0)
const isUploading = ref(false)
const uploadingInfo = ref('')

// 路径部分
const pathParts = computed(() => {
  if (currentPath.value === '/') return []
  return currentPath.value.split('/').filter(p => p)
})

// 文件类型图标
const getFileIcon = (isDirectory) => {
  return isDirectory ? FolderOutline : DocumentOutline
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN')
}

// 表格列定义
const columns = [
  {
    title: '名称',
    key: 'name',
    render: (row) => {
      return h(
        'div',
        {
          style: 'display: flex; align-items: center; gap: 8px; cursor: pointer',
          onClick: () => handleFileClick(row)
        },
        [
          h(NIcon, { size: 20, component: getFileIcon(row.isDirectory) }),
          h('span', row.name)
        ]
      )
    }
  },
  {
    title: '大小',
    key: 'size',
    width: 120,
    render: (row) => row.isDirectory ? '-' : formatSize(row.size)
  },
  {
    title: '权限',
    key: 'permissions',
    width: 120
  },
  {
    title: '修改时间',
    key: 'modifiedTime',
    width: 180,
    render: (row) => formatTime(row.modifiedTime)
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    render: (row) => {
      return h(
        'div',
        { style: 'display: flex; gap: 8px' },
        [
          !row.isDirectory && h(
            NButton,
            {
              text: true,
              type: 'primary',
              disabled: isDownloading.value,
              loading: isDownloading.value && downloadingFileName.value === row.name,
              onClick: () => handleDownload(row)
            },
            {
              default: () => isDownloading.value && downloadingFileName.value === row.name ? '下载中...' : '下载',
              icon: () => h(NIcon, { component: DownloadOutline })
            }
          ),
          h(
            NButton,
            {
              text: true,
              type: 'error',
              disabled: isDownloading.value,
              onClick: () => handleDelete(row)
            },
            {
              default: () => '删除',
              icon: () => h(NIcon, { component: TrashOutline })
            }
          )
        ]
      )
    }
  }
]

// 加载服务器列表
const loadServers = async () => {
  try {
    const res = await getServerList({ current: 1, size: 100 })
    serverOptions.value = res.records.map(server => ({
      label: server.name,
      value: server.id
    }))
    
    // 默认选择第一台服务器
    if (serverOptions.value.length > 0 && !selectedServerId.value) {
      selectedServerId.value = serverOptions.value[0].value
      // 自动加载文件列表
      loadFiles()
    }
  } catch (error) {
    message.error('加载服务器列表失败')
  }
}

// 服务器切换
const handleServerChange = () => {
  currentPath.value = '/'
  loadFiles()
}

// 加载文件列表
const loadFiles = async () => {
  if (!selectedServerId.value) return

  try {
    loading.value = true
    fileList.value = await listFiles({
      serverId: selectedServerId.value,
      path: currentPath.value
    })
  } catch (error) {
    message.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

// 文件点击
const handleFileClick = (file) => {
  if (file.isDirectory) {
    navigateTo(file.path)
  }
}

// 路径导航
const navigateTo = (path) => {
  currentPath.value = path
  loadFiles()
}

const navigateToPath = (index) => {
  const parts = currentPath.value.split('/').filter(p => p)
  const newPath = '/' + parts.slice(0, index + 1).join('/')
  navigateTo(newPath)
}

// 上传文件
const handleUpload = () => {
  fileInput.value.click()
}

// 上传文件夹
const handleUploadFolder = () => {
  folderInput.value.click()
}

const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  try {
    loading.value = true
    await uploadFile(selectedServerId.value, currentPath.value, file)
    message.success('文件上传成功')
    loadFiles()
    fileInput.value.value = ''
  } catch (error) {
    message.error('文件上传失败')
  } finally {
    loading.value = false
  }
}

// 处理文件夹上传
const handleFolderChange = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  try {
    isUploading.value = true
    uploadProgress.value = 0
    uploadingInfo.value = `准备上传 ${files.length} 个文件...`

    // 提取文件的相对路径
    const paths = files.map(file => {
      // 获取文件的完整路径（包括文件夹结构）
      return file.webkitRelativePath || file.name
    })

    // 显示上传进度提示
    const uploadMsg = message.loading(
      uploadingInfo.value,
      { duration: 0 }
    )

    const result = await uploadBatch(
      selectedServerId.value,
      currentPath.value,
      files,
      paths,
      (percent, loaded, total) => {
        uploadProgress.value = percent
        uploadingInfo.value = `正在上传... ${percent}% (${formatSize(loaded)} / ${formatSize(total)})`
        uploadMsg.content = uploadingInfo.value
      }
    )

    uploadMsg.destroy()

    // 显示结果
    if (result.failCount > 0) {
      message.warning(`${result.message}. 失败文件: ${result.failedFiles.join(', ')}`)
    } else {
      message.success(result.message)
    }

    loadFiles()
    folderInput.value.value = ''
  } catch (error) {
    console.error('上传文件夹错误:', error)
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
    uploadingInfo.value = ''
  }
}

// 下载文件
const handleDownload = async (file) => {
  try {
    isDownloading.value = true
    downloadingFileName.value = file.name
    downloadProgress.value = 0
    
    // 显示下载进度提示
    const downloadMsg = message.loading(
      `正在下载 "${file.name}"... 0%`,
      { duration: 0 }
    )
    
    const blob = await downloadFile(
      selectedServerId.value,
      file.path,
      (percent, loaded, total) => {
        downloadProgress.value = percent
        // 更新进度提示
        downloadMsg.content = `正在下载 "${file.name}"... ${percent}% (${formatSize(loaded)} / ${formatSize(total)})`
      }
    )
    
    downloadMsg.destroy()
    
    // 检查是否是有效的 blob
    if (!blob || blob.size === 0) {
      message.error('下载文件为空')
      return
    }
    
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    message.success(`"${file.name}" 下载成功 (${formatSize(blob.size)})`)
  } catch (error) {
    console.error('下载文件错误:', error)
    // 错误消息已经在拦截器中处理，这里不需要再次显示
  } finally {
    isDownloading.value = false
    downloadingFileName.value = ''
    downloadProgress.value = 0
  }
}

// 删除文件
const handleDelete = (file) => {
  const title = file.isDirectory ? '确认删除文件夹' : '确认删除文件'
  const content = file.isDirectory 
    ? `确定要删除文件夹 "${file.name}" 吗？\n\n警告：此操作将删除该文件夹及其所有内容，且无法恢复！`
    : `确定要删除文件 "${file.name}" 吗？`
  
  dialog.warning({
    title: title,
    content: content,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        loading.value = true
        await deleteFile(selectedServerId.value, file.path)
        message.success('删除成功')
        loadFiles()
      } catch (error) {
        message.error('删除失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 创建文件夹
const handleCreateDir = async () => {
  if (!newDirName.value) {
    message.warning('请输入文件夹名称')
    return false
  }

  try {
    await createDirectory({
      serverId: selectedServerId.value,
      path: currentPath.value,
      dirName: newDirName.value
    })
    message.success('创建成功')
    newDirName.value = ''
    showCreateDirModal.value = false
    loadFiles()
  } catch (error) {
    message.error('创建失败')
    return false
  }
}

onMounted(() => {
  loadServers()
})
</script>

<style scoped>
.file-manager {
  width: 100%;
}
</style>
