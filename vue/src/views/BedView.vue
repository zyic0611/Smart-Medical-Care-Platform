<template>
  <div class="container">
    <div class="action-bar">
      <div class="search-area">
        <el-input v-model="queryParams.bedNumber" placeholder="请输入床号" style="width: 200px; margin-right: 10px" clearable @clear="loadData"/>
        <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon> 搜索</el-button>
      </div>
      <div class="button-area">
        <el-button type="primary" @click="handleAdd" v-if="userRole === 'ADMIN'"><el-icon><Plus /></el-icon> 新增床位</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="bedNumber" label="床位号" align="center" />

      <el-table-column label="状态" align="center">
        <template #default="scope">
          <el-tag type="success" v-if="scope.row.status === 0">空闲</el-tag>
          <el-tag type="danger" v-else>占用</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="scope">
          <div v-if="userRole === 'ADMIN'">
            <el-button
                circle size="small" type="primary"
                @click="handleEdit(scope.row)"
            >
              <el-icon><Edit /></el-icon>
            </el-button>

            <el-button
                circle size="small" type="danger"
                @click="handleDelete(scope.row.id)"
                :disabled="scope.row.status === 1"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>

          <div v-else>
            <span style="font-size: 12px; color: #999">无权限</span>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination background layout="total, prev, pager, next, jumper" :total="total" :page-size="queryParams.pageSize" @current-change="handlePageChange"/>
    </div>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="床位号">
          <el-input v-model="form.bedNumber" placeholder="例如：A-101" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">空闲</el-radio>
            <el-radio :value="1">占用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSave">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBedPage, addBed, updateBed, deleteBed } from '@/api/bed'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formTitle = ref('')
const form = ref({})
const queryParams = reactive({ pageNum: 1, pageSize: 10, bedNumber: '' })
// 定义用户角色
const userRole = ref('')

const loadData = async () => {
  loading.value = true
  const res = await getBedPage(queryParams)
  tableData.value = res.list
  total.value = res.total
  loading.value = false
}

const handlePageChange = (newPage) => {
  queryParams.pageNum = newPage
  loadData()
}

const handleAdd = () => {
  form.value = { status: 0 } // 默认空闲
  formTitle.value = '新增床位'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  formTitle.value = '编辑床位'
  dialogVisible.value = true
}

const handleSave = async () => {
  if (form.value.id) {
    await updateBed(form.value)
    ElMessage.success('修改成功')
  } else {
    await addBed(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该床位吗？', '提示', { type: 'warning' })
      .then(async () => {
        await deleteBed(id)
        ElMessage.success('删除成功')
        loadData()
      }).catch(() => {})
}

onMounted(() => {
  // 1. 获取角色
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const userObj = JSON.parse(userStr)
    userRole.value = userObj.role // 'ADMIN' 或 'USER'
  }

  // 2. 加载数据
  loadData()
})
</script>

<style scoped>
.container { padding: 20px; }
.action-bar { margin-bottom: 20px; display: flex; justify-content: space-between; }
.search-area { display: flex; }
</style>