<template>
  <div class="menu-list">
    <div class="toolbar">
      <n-space>
        <n-input v-model:value="searchName" placeholder="搜索菜单名称" clearable style="width: 250px" @keyup.enter="loadData">
          <template #suffix>
            <n-button text @click="loadData"><n-icon><SearchSharp /></n-icon></n-button>
          </template>
        </n-input>
        <n-button type="primary" @click="showModal()">
          <template #icon><n-icon><AddSharp /></n-icon></template>
          新增菜单
        </n-button>
      </n-space>
    </div>
    
    <n-data-table 
      :columns="columns" 
      :data="dataSource" 
      :loading="loading"
      default-expand-all
      :row-key="row => row.id"
    />
    
    <n-modal v-model:show="visible" :title="editId ? '编辑菜单' : '新增菜单'" preset="dialog" style="width: 600px" :show-icon="false">
      <n-form ref="formRef" :model="formState" :rules="rules" label-placement="left" label-width="100">
        <n-form-item label="父菜单" path="parentId">
          <n-tree-select v-model:value="formState.parentId" :options="menuTreeOptions" clearable placeholder="顶级菜单" />
        </n-form-item>
        <n-form-item label="菜单名称" path="name">
          <n-input v-model:value="formState.name" />
        </n-form-item>
        <n-form-item label="菜单路径" path="path">
          <n-input v-model:value="formState.path" placeholder="/path" />
        </n-form-item>
        <n-form-item label="图标" path="icon">
          <n-input v-model:value="formState.icon" placeholder="SpeedometerSharp" />
        </n-form-item>
        <n-form-item label="排序" path="sortOrder">
          <n-input-number v-model:value="formState.sortOrder" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="菜单类型" path="type">
          <n-radio-group v-model:value="formState.type">
            <n-radio value="MENU">菜单</n-radio>
            <n-radio value="BUTTON">按钮</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="是否可见" path="isVisible">
          <n-radio-group v-model:value="formState.isVisible">
            <n-radio :value="1">是</n-radio>
            <n-radio :value="0">否</n-radio>
          </n-radio-group>
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
import { SearchSharp, AddSharp, CreateSharp, TrashSharp, AddCircleSharp } from '@vicons/ionicons5'
import { getMenuList, addMenu, updateMenu, deleteMenu, getMenuTree } from '@/api/menu'

const message = useMessage()
const loading = ref(false)
const visible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const searchName = ref('')
const dataSource = ref([])
const allMenuTree = ref([])
const menuTreeOptions = ref([])

const formState = reactive({
  parentId: 0,
  name: '',
  path: '',
  icon: '',
  sortOrder: 0,
  type: 'MENU',
  isVisible: 1
})

const rules = {
  name: { required: true, message: '请输入菜单名称', trigger: 'blur' }
}

const columns = [
  { 
    title: 'ID', 
    key: 'id', 
    width: 80 
  },
  { 
    title: '菜单名称', 
    key: 'name',
    width: 200
  },
  { 
    title: '路径', 
    key: 'path',
    width: 200
  },
  { 
    title: '图标', 
    key: 'icon',
    width: 150
  },
  { 
    title: '排序', 
    key: 'sortOrder', 
    width: 80 
  },
  {
    title: '类型',
    key: 'type',
    width: 100,
    render: (row) => h(NTag, { type: row.type === 'MENU' ? 'info' : 'warning', size: 'small' },
      { default: () => row.type === 'MENU' ? '菜单' : '按钮' })
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) => h(NSpace, null, {
      default: () => [
        h(NButton, { 
          text: true, 
          type: 'success',
          size: 'small',
          onClick: () => showModal(null, row.id) 
        }, { 
          default: () => '新增子菜单',
          icon: () => h('i', { class: 'n-icon' }, [h(AddCircleSharp)])
        }),
        h(NButton, { 
          text: true, 
          type: 'primary', 
          size: 'small',
          onClick: () => showModal(row) 
        }, { 
          default: () => '编辑',
          icon: () => h('i', { class: 'n-icon' }, [h(CreateSharp)])
        }),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row.id)
        }, {
          default: () => '确定删除此菜单吗？',
          trigger: () => h(NButton, { 
            text: true, 
            type: 'error',
            size: 'small'
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
    // 加载树形菜单数据
    const data = await getMenuTree()
    
    // 如果有搜索条件，过滤数据
    if (searchName.value) {
      dataSource.value = filterMenuTree(data, searchName.value)
    } else {
      dataSource.value = data
    }
    
    allMenuTree.value = data
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 过滤菜单树
const filterMenuTree = (menus, keyword) => {
  const result = []
  for (const menu of menus) {
    if (menu.name.includes(keyword)) {
      result.push(menu)
    } else if (menu.children && menu.children.length > 0) {
      const filteredChildren = filterMenuTree(menu.children, keyword)
      if (filteredChildren.length > 0) {
        result.push({
          ...menu,
          children: filteredChildren
        })
      }
    }
  }
  return result
}

const loadMenuTree = async () => {
  try {
    const data = await getMenuTree()
    allMenuTree.value = data
    menuTreeOptions.value = convertToTreeSelect(data)
  } catch (error) {
    console.error(error)
  }
}

const convertToTreeSelect = (menus) => {
  return [
    { label: '顶级菜单', key: 0, value: 0 },
    ...menus.map(menu => ({
      label: menu.name,
      key: menu.id,
      value: menu.id,
      children: menu.children ? convertToTreeSelect(menu.children) : undefined
    }))
  ]
}

const showModal = (record = null, parentId = null) => {
  editId.value = record ? record.id : null
  if (record) {
    formState.parentId = record.parentId
    formState.name = record.name
    formState.path = record.path || ''
    formState.icon = record.icon || ''
    formState.sortOrder = record.sortOrder || 0
    formState.menuType = record.menuType || 'MENU'
    formState.visible = record.visible || 1
  } else {
    formState.parentId = parentId || 0
    formState.name = ''
    formState.path = ''
    formState.icon = ''
    formState.sortOrder = 0
    formState.menuType = 'MENU'
    formState.visible = 1
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
          await updateMenu(data)
          message.success('更新成功')
        } else {
          await addMenu(data)
          message.success('添加成功')
        }
        visible.value = false
        loadData()
        loadMenuTree()
      } catch (error) {
        message.error(editId.value ? '更新失败' : '添加失败')
      }
    }
  })
}

const handleDelete = async (id) => {
  try {
    await deleteMenu(id)
    message.success('删除成功')
    loadData()
    loadMenuTree()
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadData()
  loadMenuTree()
})
</script>

<style scoped>
.menu-list {
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
