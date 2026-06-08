<template>
  <div class="role-list">
    <div class="toolbar">
      <n-space>
        <n-input v-model:value="searchName" placeholder="搜索角色名称" clearable style="width: 250px" @keyup.enter="loadData">
          <template #suffix>
            <n-button text @click="loadData"><n-icon><SearchSharp /></n-icon></n-button>
          </template>
        </n-input>
        <n-button type="primary" @click="showModal()">
          <template #icon><n-icon><AddSharp /></n-icon></template>
          新增角色
        </n-button>
      </n-space>
    </div>
    
    <n-data-table :columns="columns" :data="dataSource" :pagination="pagination" :loading="loading" :remote="true"
      @update:page="handlePageChange" @update:page-size="handlePageSizeChange" />
    
    <n-modal v-model:show="visible" :title="editId ? '编辑角色' : '新增角色'" preset="dialog" style="width: 500px" :show-icon="false">
      <n-form ref="formRef" :model="formState" :rules="rules" label-placement="left" label-width="100">
        <n-form-item label="角色名称" path="name">
          <n-input v-model:value="formState.name" />
        </n-form-item>
        <n-form-item label="角色编码" path="permissionCode">
          <n-input v-model:value="formState.permissionCode" :disabled="!!editId" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formState.description" type="textarea" :rows="3" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space>
          <n-button @click="visible = false">取消</n-button>
          <n-button type="primary" @click="handleOk">确定</n-button>
        </n-space>
      </template>
    </n-modal>
    
    <!-- 菜单权限弹窗 -->
    <n-modal v-model:show="menuVisible" title="菜单权限设置" preset="dialog" style="width: 500px" :show-icon="false">
      <n-tree
        block-line
        checkable
        :data="menuTree"
        :checked-keys="checkedMenuKeys"
        @update:checked-keys="handleCheckedKeysChange"
      />
      <template #action>
        <n-space>
          <n-button @click="menuVisible = false">取消</n-button>
          <n-button type="primary" @click="saveMenuPermission">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, NButton, NSpace, NPopconfirm, NTag } from 'naive-ui'
import { SearchSharp, AddSharp, MenuSharp, CreateSharp, TrashSharp, KeySharp } from '@vicons/ionicons5'
import { getRoleList, addRole, updateRole, deleteRole } from '@/api/role'
import { getMenuTree, getRoleMenuIds, assignRoleMenus } from '@/api/menu'

const message = useMessage()
const loading = ref(false)
const visible = ref(false)
const menuVisible = ref(false)
const editId = ref(null)
const currentRoleId = ref(null)
const formRef = ref(null)
const searchName = ref('')
const dataSource = ref([])
const menuTree = ref([])
const checkedMenuKeys = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const formState = reactive({
  name: '',
  permissionCode: '',
  description: ''
})

const rules = {
  name: { required: true, message: '请输入角色名称', trigger: 'blur' },
  permissionCode: { required: true, message: '请输入角色编码', trigger: 'blur' }
}

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色名称', key: 'name' },
  {
    title: '角色编码',
    key: 'permissionCode',
    render: (row) => h(NTag, { type: 'info' }, { default: () => row.permissionCode })
  },
  { title: '描述', key: 'description' },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    render: (row) => {
      // 系统预置角色不允许删除
      const isSystemRole = ['ADMIN', 'PROJECT_ADMIN', 'DEVELOPER', 'USER'].includes(row.permissionCode)
      const buttons = [
        h(NButton, { 
          text: true, 
          type: 'primary', 
          onClick: () => showModal(row) 
        }, { 
          default: () => '编辑',
          icon: () => h('i', { class: 'n-icon' }, [h(CreateSharp)])
        }),
        h(NButton, { 
          text: true, 
          type: 'warning', 
          onClick: () => showMenuModal(row.id) 
        }, { 
          default: () => '菜单权限',
          icon: () => h('i', { class: 'n-icon' }, [h(KeySharp)])
        })
      ]
      
      if (!isSystemRole) {
        buttons.push(
          h(NPopconfirm, {
            onPositiveClick: () => handleDelete(row.id)
          }, {
            default: () => '确定删除此角色吗？',
            trigger: () => h(NButton, { 
              text: true, 
              type: 'error' 
            }, { 
              default: () => '删除',
              icon: () => h('i', { class: 'n-icon' }, [h(TrashSharp)])
            })
          })
        )
      }
      
      return h(NSpace, null, { default: () => buttons })
    }
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const data = await getRoleList({
      current: pagination.page,
      size: pagination.pageSize,
      name: searchName.value
    })
    dataSource.value = data.records
    pagination.itemCount = data.total
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const showModal = (record = null) => {
  editId.value = record ? record.id : null
  if (record) {
    formState.name = record.name
    formState.permissionCode = record.permissionCode
    formState.description = record.description || ''
  } else {
    formState.name = ''
    formState.permissionCode = ''
    formState.description = ''
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
          await updateRole(data)
          message.success('更新成功')
        } else {
          await addRole(data)
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
    await deleteRole(id)
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

// 菜单权限相关
const showMenuModal = async (roleId) => {
  currentRoleId.value = roleId
  
  try {
    // 加载所有菜单树
    const tree = await getMenuTree()
    menuTree.value = convertToTree(tree)
    
    // 加载角色已有的菜单
    const menuIds = await getRoleMenuIds(roleId)
    checkedMenuKeys.value = menuIds
    
    menuVisible.value = true
  } catch (error) {
    message.error('加载菜单权限失败')
  }
}

const convertToTree = (menus) => {
  return menus.map(menu => ({
    key: menu.id,
    label: menu.name,
    children: menu.children ? convertToTree(menu.children) : undefined
  }))
}

const handleCheckedKeysChange = (keys) => {
  checkedMenuKeys.value = keys
}

const saveMenuPermission = async () => {
  try {
    await assignRoleMenus(currentRoleId.value, checkedMenuKeys.value)
    message.success('保存成功')
    menuVisible.value = false
  } catch (error) {
    message.error('保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.role-list {
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
