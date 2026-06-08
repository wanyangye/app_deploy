<template>
  <div class="operation-log">
    <div class="toolbar">
      <n-space>
        <n-input
          v-model:value="searchUsername"
          placeholder="搜索用户名"
          clearable
          style="width: 200px"
          @keyup.enter="loadData"
        >
          <template #suffix>
            <n-button text @click="loadData">
              <n-icon><SearchSharp /></n-icon>
            </n-button>
          </template>
        </n-input>
        <n-input
          v-model:value="searchModule"
          placeholder="搜索操作模块"
          clearable
          style="width: 200px"
          @keyup.enter="loadData"
        />
        <n-select
          v-model:value="searchStatus"
          :options="statusOptions"
          placeholder="操作状态"
          clearable
          style="width: 150px"
          @update:value="loadData"
        />
        <n-popconfirm @positive-click="handleClear">
          <template #trigger>
            <n-button type="error">
              <template #icon><n-icon><TrashSharp /></n-icon></template>
              清空日志
            </n-button>
          </template>
          确定要清空所有操作日志吗？
        </n-popconfirm>
      </n-space>
    </div>

    <n-data-table
      :columns="columns"
      :data="dataSource"
      :pagination="pagination"
      :loading="loading"
      :remote="true"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <!-- 详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      preset="card"
      title="操作详情"
      style="width: 800px"
      :bordered="false"
    >
      <n-descriptions :column="2" label-placement="left" bordered>
        <n-descriptions-item label="用户名">
          {{ currentLog.username }}
        </n-descriptions-item>
        <n-descriptions-item label="操作模块">
          {{ currentLog.module }}
        </n-descriptions-item>
        <n-descriptions-item label="操作类型">
          {{ currentLog.operationType }}
        </n-descriptions-item>
        <n-descriptions-item label="IP地址">
          {{ currentLog.ipAddress }}
        </n-descriptions-item>
        <n-descriptions-item label="操作描述" :span="2">
          {{ currentLog.description }}
        </n-descriptions-item>
        <n-descriptions-item label="请求方法" :span="2">
          <n-text code>{{ currentLog.method }}</n-text>
        </n-descriptions-item>
        <n-descriptions-item label="请求参数" :span="2">
          <n-code :code="formatJson(currentLog.params)" language="json" />
        </n-descriptions-item>
        <n-descriptions-item label="返回结果" :span="2">
          <n-code :code="formatJson(currentLog.result)" language="json" />
        </n-descriptions-item>
        <n-descriptions-item v-if="currentLog.errorMsg" label="错误信息" :span="2">
          <n-text type="error">{{ currentLog.errorMsg }}</n-text>
        </n-descriptions-item>
        <n-descriptions-item label="操作时间" :span="2">
          {{ currentLog.operationTime }}
        </n-descriptions-item>
      </n-descriptions>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, NButton, NTag, NPopconfirm, NSpace } from 'naive-ui'
import { SearchSharp, TrashSharp, CheckmarkCircleSharp, CloseCircleSharp, EyeSharp } from '@vicons/ionicons5'
import { getOperationLogList, deleteOperationLog, clearOperationLog } from '@/api/log'

const message = useMessage()
const loading = ref(false)
const searchUsername = ref('')
const searchModule = ref('')
const searchStatus = ref(null)
const dataSource = ref([])
const detailVisible = ref(false)
const currentLog = ref({})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const columns = [
  { title: 'ID', key: 'id', width: 180 },
  { title: '用户名', key: 'username', width: 120 },
  { title: '操作模块', key: 'module', width: 120 },
  { title: '操作类型', key: 'operationType', width: 100 },
  {
    title: '操作描述',
    key: 'description',
    ellipsis: { tooltip: true }
  },
  { title: 'IP地址', key: 'ipAddress', width: 140 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      return h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', size: 'small' },
        {
          default: () => row.status === 1 ? '成功' : '失败',
          icon: () => h('i', { class: 'n-icon' }, [
            h(row.status === 1 ? CheckmarkCircleSharp : CloseCircleSharp)
          ])
        }
      )
    }
  },
  { title: '操作时间', key: 'operationTime', width: 180 },
  {
    title: '操作',
    key: 'action',
    width: 150,
    render: (row) => {
      return h(
        NSpace,
        {},
        {
          default: () => [
            h(
              NButton,
              {
                text: true,
                type: 'success',
                size: 'small',
                onClick: () => showDetail(row)
              },
              {
                default: () => '详情',
                icon: () => h('i', { class: 'n-icon' }, [h(EyeSharp)])
              }
            ),
            h(
              NPopconfirm,
              { onPositiveClick: () => handleDelete(row.id) },
              {
                default: () => '确定要删除吗？',
                trigger: () => h(
                  NButton,
                  { text: true, type: 'error', size: 'small' },
                  {
                    default: () => '删除',
                    icon: () => h('i', { class: 'n-icon' }, [h(TrashSharp)])
                  }
                )
              }
            )
          ]
        }
      )
    }
  }
]

const loadData = async () => {
  try {
    loading.value = true
    const params = {
      current: pagination.page,
      size: pagination.pageSize
    }

    if (searchUsername.value) {
      params.username = searchUsername.value
    }

    if (searchModule.value) {
      params.module = searchModule.value
    }

    if (searchStatus.value !== null) {
      params.status = searchStatus.value
    }

    const data = await getOperationLogList(params)
    dataSource.value = data.records
    pagination.itemCount = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
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

const handleDelete = async (id) => {
  try {
    await deleteOperationLog(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const handleClear = async () => {
  try {
    await clearOperationLog()
    message.success('清空成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    const obj = JSON.parse(str)
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.operation-log {
  width: 100%;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
