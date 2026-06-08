<template>
  <div class="login-log">
    <div class="toolbar">
      <n-space>
        <n-input
          v-model:value="searchUsername"
          placeholder="搜索用户名"
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
        <n-select
          v-model:value="searchStatus"
          :options="statusOptions"
          placeholder="登录状态"
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
          确定要清空所有登录日志吗？
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, NButton, NTag, NPopconfirm } from 'naive-ui'
import { SearchSharp, TrashSharp, CheckmarkCircleSharp, CloseCircleSharp } from '@vicons/ionicons5'
import { getLoginLogList, deleteLoginLog, clearLoginLog } from '@/api/log'

const message = useMessage()
const loading = ref(false)
const searchUsername = ref('')
const searchStatus = ref(null)
const dataSource = ref([])

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
  { title: '用户名', key: 'username' },
  { title: 'IP地址', key: 'ipAddress' },
  { title: '浏览器', key: 'browser', width: 100 },
  { title: '操作系统', key: 'os', width: 100 },
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
  { title: '提示消息', key: 'message' },
  { title: '登录时间', key: 'loginTime', width: 180 },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render: (row) => {
      return h(
        NPopconfirm,
        { onPositiveClick: () => handleDelete(row.id) },
        {
          default: () => '确定要删除吗？',
          trigger: () => h(
            NButton,
            { text: true, type: 'error', size: 'small' },
            { default: () => '删除' }
          )
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

    if (searchStatus.value !== null) {
      params.status = searchStatus.value
    }

    const data = await getLoginLogList(params)
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
    await deleteLoginLog(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const handleClear = async () => {
  try {
    await clearLoginLog()
    message.success('清空成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.login-log {
  width: 100%;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
