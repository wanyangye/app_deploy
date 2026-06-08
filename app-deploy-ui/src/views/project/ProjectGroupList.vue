<template>
  <div class="project-group-list">
    <div class="toolbar">
      <n-space>
        <n-input
          v-model:value="searchName"
          placeholder="搜索项目组名称"
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
        <n-button type="primary" @click="showModal()">
          <template #icon>
            <n-icon><AddSharp /></n-icon>
          </template>
          新增项目组
        </n-button>
      </n-space>
    </div>

    <n-spin :show="loading">
      <n-empty v-if="!loading && dataSource.length === 0" description="暂无项目组数据" style="margin-top: 60px;" />

      <n-grid v-else :x-gap="16" :y-gap="16" :cols="3" responsive="screen" style="margin-top: 20px;">
        <n-gi v-for="group in dataSource" :key="group.id">
          <n-card :bordered="true" hoverable class="group-card">
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <n-icon size="24" color="#18a058">
                    <FolderOpenSharp />
                  </n-icon>
                  <div>
                    <div style="font-size: 16px; font-weight: 600;">{{ group.name }}</div>
                    <n-text depth="3" style="font-size: 12px;">{{ group.description }}</n-text>
                  </div>
                </div>
                <n-tag type="info" size="small">
                  {{ group.projectCount || 0 }} 个项目
                </n-tag>
              </div>
            </template>

            <n-space vertical :size="12">
              <div class="group-info-item">
                <n-text depth="3">负责人：</n-text>
                <n-text>{{ group.owner || '-' }}</n-text>
              </div>
              <div class="group-info-item">
                <n-text depth="3">联系方式：</n-text>
                <n-text>{{ group.ownerContact || '-' }}</n-text>
              </div>
              <div class="group-info-item">
                <n-text depth="3">创建时间：</n-text>
                <n-text>{{ group.createTime }}</n-text>
              </div>
            </n-space>

            <template #footer>
              <n-space justify="space-between">
                <n-button secondary size="small" type="primary" @click="goToProjectList(group.id)">
                  <template #icon><n-icon><FolderOpenSharp /></n-icon></template>
                  项目管理
                </n-button>
                <n-space :size="6">
                  <n-button secondary size="small" type="success" @click="showModal(group)">
                    <template #icon><n-icon><CreateSharp /></n-icon></template>
                    编辑
                  </n-button>
                  <n-popconfirm @positive-click="handleDelete(group.id)">
                    <template #trigger>
                      <n-button secondary size="small" type="error">
                        <template #icon><n-icon><TrashSharp /></n-icon></template>
                        删除
                      </n-button>
                    </template>
                    确定要删除该项目组吗？
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

    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="visible"
      :title="editId ? '编辑项目组' : '新增项目组'"
      preset="dialog"
      style="width: 600px"
      :show-icon="false"
    >
      <n-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        label-placement="left"
        label-width="100"
      >
        <n-form-item label="项目组名称" path="name">
          <n-input v-model:value="formState.name" placeholder="请输入项目组名称" />
        </n-form-item>

        <n-form-item label="项目组描述" path="description">
          <n-input
            v-model:value="formState.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目组描述"
          />
        </n-form-item>

        <n-form-item label="负责人" path="owner">
          <n-input v-model:value="formState.owner" placeholder="请输入负责人姓名" />
        </n-form-item>

        <n-form-item label="联系方式" path="ownerContact">
          <n-input v-model:value="formState.ownerContact" placeholder="请输入联系方式" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { SearchSharp, AddSharp, CreateSharp, TrashSharp, FolderOpenSharp } from '@vicons/ionicons5'
import { getProjectGroupList, addProjectGroup, updateProjectGroup, deleteProjectGroup } from '@/api/project-group'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const visible = ref(false)
const editId = ref(null)
const searchName = ref('')
const formRef = ref()

const dataSource = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 9,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [9, 18, 27]
})

const formState = reactive({
  name: '',
  description: '',
  owner: '',
  ownerContact: ''
})

const rules = {
  name: [
    { required: true, message: '请输入项目组名称', trigger: 'blur' }
  ],
  owner: [
    { required: true, message: '请输入负责人', trigger: 'blur' }
  ],
  ownerContact: [
    { required: true, message: '请输入联系方式', trigger: 'blur' }
  ]
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

    const data = await getProjectGroupList(params)

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

const showModal = (record) => {
  if (record) {
    editId.value = record.id
    Object.assign(formState, {
      name: record.name || '',
      description: record.description || '',
      owner: record.owner || '',
      ownerContact: record.ownerContact || ''
    })
  } else {
    editId.value = null
    Object.assign(formState, {
      name: '',
      description: '',
      owner: '',
      ownerContact: ''
    })
  }
  visible.value = true
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()

    const submitData = { ...formState }

    if (editId.value) {
      submitData.id = editId.value
      await updateProjectGroup(submitData)
      message.success('更新成功')
    } else {
      await addProjectGroup(submitData)
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
    await deleteProjectGroup(id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    console.error(error)
  }
}

// 跳转到项目管理页面，并筛选该项目组下的项目
const goToProjectList = (groupId) => {
  const group = dataSource.value.find(g => g.id === groupId)
  router.push({
    path: '/project',
    query: { 
      groupId,
      groupName: group ? group.name : ''
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.project-group-list {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.group-card {
  transition: all 0.3s;
}

.group-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.group-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
