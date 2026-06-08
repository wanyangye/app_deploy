<template>
  <div class="user-list">
    <div class="toolbar">
      <n-space>
        <n-input v-model:value="searchAccount" placeholder="搜索用户名" clearable style="width: 250px" @keyup.enter="loadData">
          <template #suffix>
            <n-button text @click="loadData"><n-icon><SearchSharp /></n-icon></n-button>
          </template>
        </n-input>
        <n-button type="primary" @click="showModal()">
          <template #icon><n-icon><AddSharp /></n-icon></template>
          新增用户
        </n-button>
      </n-space>
    </div>
    
    <n-data-table :columns="columns" :data="dataSource" :pagination="pagination" :loading="loading" :remote="true"
      @update:page="handlePageChange" @update:page-size="handlePageSizeChange" />
    
    <n-modal v-model:show="visible" :title="editId ? '编辑用户' : '新增用户'" preset="dialog" style="width: 600px" :show-icon="false">
      <n-form ref="formRef" :model="formState" :rules="rules" label-placement="left" label-width="100">
        <n-form-item label="用户名" path="account">
          <n-input v-model:value="formState.account" :disabled="!!editId" />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input v-model:value="formState.password" type="password" show-password-on="click" :placeholder="editId ? '不修改请留空' : '默认密码123456'" />
        </n-form-item>
        <n-form-item label="昵称" path="fullName">
          <n-input v-model:value="formState.fullName" />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="formState.email" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="formState.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="角色" path="roleIds">
          <n-select v-model:value="formState.roleIds" :options="roleOptions" multiple placeholder="请选择角色" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space>
          <n-button @click="visible = false">取消</n-button>
          <n-button type="primary" @click="handleOk">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, NButton, NSpace, NTag, NPopconfirm } from 'naive-ui'
import { SearchSharp, AddSharp, CreateSharp, TrashSharp } from '@vicons/ionicons5'
import { getUserList, addUser, updateUser, deleteUser } from '@/api/user'
import { getAllRoles } from '@/api/role'

const message = useMessage()
const loading = ref(false)
const visible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const searchAccount = ref('')
const dataSource = ref([])
const roleOptions = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const formState = reactive({
  account: '',
  password: '',
  fullName: '',
  email: '',
  status: 1,
  roleIds: []
})

const rules = {
  account: { required: true, message: '请输入用户名', trigger: 'blur' },
  fullName: { required: true, message: '请输入昵称', trigger: 'blur' }
}

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'account' },
  { title: '昵称', key: 'fullName' },
  { title: '邮箱', key: 'email' },
  {
    title: '状态',
    key: 'status',
    render: (row) => h(NTag, { type: row.status === 1 ? 'success' : 'error' }, 
      { default: () => row.status === 1 ? '启用' : '禁用' })
  },
  {
    title: '角色',
    key: 'roles',
    render: (row) => {
      if (!row.roles || row.roles.length === 0) return '-'
      return h(NSpace, null, {
        default: () => row.roles.map(role => h(NTag, { type: 'info', size: 'small' }, { default: () => role.name }))
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row) => h(NSpace, null, {
      default: () => [
        h(NButton, { 
          text: true, 
          type: 'primary', 
          onClick: () => showModal(row) 
        }, { 
          default: () => '编辑',
          icon: () => h('i', { class: 'n-icon' }, [h(CreateSharp)])
        }),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row.id)
        }, {
          default: () => '确定删除此用户吗？',
          trigger: () => h(NButton, { 
            text: true, 
            type: 'error' 
          }, { 
            default: () => '删除',
            icon: () => h('i', { class: 'n-icon' }, [h(TrashSharp)])
          })
        })
      ]
    })
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const data = await getUserList({
      current: pagination.page,
      size: pagination.pageSize,
      account: searchAccount.value
    })
    dataSource.value = data.records
    pagination.itemCount = data.total
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const data = await getAllRoles()
    roleOptions.value = data.map(role => ({
      label: role.name,
      value: role.id
    }))
  } catch (error) {
    message.error('加载角色失败')
  }
}

const showModal = (record = null) => {
  editId.value = record ? record.id : null
  if (record) {
    formState.account = record.account
    formState.password = ''
    formState.fullName = record.fullName
    formState.email = record.email || ''
    formState.status = record.status
    formState.roleIds = record.roles ? record.roles.map(r => r.id) : []
  } else {
    formState.account = ''
    formState.password = ''
    formState.fullName = ''
    formState.email = ''
    formState.status = 1
    formState.roleIds = []
  }
  visible.value = true
}

const handleOk = () => {
  formRef.value?.validate(async (errors) => {
    if (!errors) {
      try {
        const data = { ...formState }
        if (editId.value) {
          data.id = editId.value
          await updateUser(data)
          message.success('更新成功')
        } else {
          await addUser(data)
          message.success('添加成功')
        }
        visible.value = false
        loadData()
      } catch (error) {
        message.error(editId.value ? '更新失败' : '添加失败')
      }
    }
  })
}

const handleDelete = async (id) => {
  try {
    await deleteUser(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
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

onMounted(() => {
  loadData()
  loadRoles()
})
</script>

<style scoped>
.user-list {
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
