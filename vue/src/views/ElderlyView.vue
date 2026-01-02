<template>
  <div class="container">
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd" v-if="userRole === 'ADMIN'">新增老人</el-button>
    </div>

    <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%">

      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="name" label="老人姓名" min-width="100" align="center" />
      <el-table-column prop="gender" label="性别" width="60" align="center" />
      <el-table-column prop="age" label="年龄" width="60" align="center" />

      <el-table-column label="负责护工" width="120" align="center">
        <template #default="scope">
          <el-tag type="success" v-if="scope.row.nurse">
            {{ scope.row.nurse.name }}
          </el-tag>
          <span v-else style="color: #999">未分配</span>
        </template>
      </el-table-column>

      <el-table-column label="护工电话" width="120" align="center">
        <template #default="scope">
          {{ scope.row.nurse?.phone || '-' }}
        </template>
      </el-table-column>

      <el-table-column label="床位号" width="100" align="center">
        <template #default="scope">
          <el-tag type="warning" v-if="scope.row.bed">
            {{ scope.row.bed.bedNumber }}
          </el-tag>
          <span v-else>无</span>
        </template>
      </el-table-column>

      <el-table-column label="健康状况" width="120" align="center">
        <template #default="scope">
          <el-tag type="success" v-if="scope.row.healthStatus == 0">健康</el-tag>
          <el-tag type="warning" v-else-if="scope.row.healthStatus == 1">亚健康</el-tag>
          <el-tag type="danger" v-else-if="scope.row.healthStatus == 2">患病</el-tag>
          <el-tag type="info" v-else>未知</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="入院时间" width="110" align="center" />

      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="scope">
          <el-tooltip content="编辑信息" placement="top">
            <el-button circle size="small" type="primary" v-if="userRole === 'ADMIN'" @click="handleEdit(scope.row)">
              <el-icon><Edit /></el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="影像档案" placement="top">
            <el-button circle size="small" type="warning" @click="openImagingDialog(scope.row)">
              <el-icon><Picture /></el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="删除老人" placement="top">
            <el-button circle size="small" type="danger" v-if="userRole === 'ADMIN'" @click="handleDelete(scope.row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </template>
      </el-table-column>

    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :page-size="queryParams.pageSize"
          @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="500px">
      <el-form :model="form" label-width="100px">

        <el-form-item label="老人姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="50" :max="120" />
        </el-form-item>

        <el-form-item label="健康状况">
          <el-select v-model="form.healthStatus" placeholder="请选择">
            <el-option label="健康" :value="0" />
            <el-option label="亚健康" :value="1" />
            <el-option label="患病" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="负责护工">
          <el-select v-model="form.nurseId" placeholder="请选择护工" style="width: 100%">
            <el-option
                v-for="nurse in nurseList"
                :key="nurse.id"
                :label="nurse.name"
                :value="nurse.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="床位号">
          <el-select v-model="form.bedId" placeholder="请选择床位" style="width: 100%" no-data-text="当前无空闲床位">
            <el-option
                v-for="bed in bedList"
                :key="bed.id"
                :label="bed.bedNumber"
                :value="bed.id"
            />
          </el-select>
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSave">保 存</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="imgDialogVisible"
        :title="currentElderName + ' 的医疗影像档案'"
        width="700px"
        destroy-on-close
    >

      <div style="margin-bottom: 15px;">
        <el-upload
            action="http://localhost:9090/file/upload"
            :headers="{ token: token }"
            :show-file-list="false"
            :on-success="handleImgUploadSuccess"
        >
          <el-button type="primary">
            <el-icon><Upload /></el-icon> 上传新影像
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持 jpg/png/dcm 格式，文件将存入 MinIO 云存储
            </div>
          </template>
        </el-upload>
      </div>
      <el-table :data="imagingList" border stripe style="width: 100% ">
        <el-table-column prop="fileName" label="影像名称" />
        <el-table-column prop="fileSize" label="大小" width="100" align="center">
          <template #default="scope">
            {{ (scope.row.fileSize / 1024 / 1024).toFixed(2) }} MB
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="160" align="center" />

        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button
                type="success"
                link
                icon="Download"
                @click="handleDownload(scope.row)">
              高速下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="imgDialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

import { getElderlyPage ,deleteElderly,updateElderly,addElderly} from '@/api/elderly' //引入API
import { selectAllEmployee } from '@/api/employee' // 复用员工查询接口拿护工列表！
import { selectFreeBed } from '@/api/bed'
import { selectByElderId,addimage} from '@/api/image'
import { startAsyncDownload, getDownloadStatus, fetchDownloadedFile } from '@/api/asyncdownload.js'

//html组件
import { Plus, Edit, Delete,Picture, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'


const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: ''
})

//定义操作用户
const userRole = ref('')

//获取 Token (给 el-upload 用)
const token = localStorage.getItem('token')

// 定义新增老人弹窗相关变量
const dialogVisible = ref(false)
const formTitle = ref('')
const form = ref({})

// 影像弹窗相关的变量
const imgDialogVisible = ref(false)
const currentElderName = ref('')
const imagingList = ref([])

// 定义下拉框的数据源 (先给空数组，防止报错)
const nurseList = ref([])
const bedList = ref([])


//用来添加影像
const currentElderId = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getElderlyPage(queryParams)
    tableData.value = res.list
    total.value = res.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

//页面显示
const handlePageChange = (newPage) => {
  queryParams.pageNum = newPage
  loadData()
}

onMounted(() => {
  // ---从缓存获取角色 ---
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const userObj = JSON.parse(userStr)
    userRole.value = userObj.role // 'ADMIN' 或 'USER'
  }


  loadData()
})

//  点击编辑
const handleEdit = async (row) => {

  // console.log("1. 点击编辑，接收到的行数据 row:", row)
  // console.log("   其中 ID 为:", row.id)

  form.value = JSON.parse(JSON.stringify(row)) // 深拷贝基础数据
  //
  // console.log("2. 赋值后，form.value 的数据:", form.value)

  // 手动提取关联对象的 ID 也就是处理外键
  if (row.nurse) {
    form.value.nurseId = row.nurse.id // 确保下拉框能回显
  }
  if (row.bed) {
    form.value.bedId = row.bed.id     // 确保下拉框能回显
  }
  // ⬆️⬆️⬆️ 新增结束 ⬆️⬆️⬆️

  formTitle.value = '修改老人信息'
  dialogVisible.value = true

  // 先加载标准的空闲床位 (必须加 await 等它查完)
  await loadOptions()

  // 把当前占用的床位塞进去
  if (row.bed) {
    // 检查一下列表里是不是已经有了 (防止重复添加)
    const exists = bedList.value.some(item => item.id === row.bed.id)

    if (!exists) {
      // 如果列表里没有这张床，我们就手动加进去
      bedList.value.push({
        id: row.bed.id,
        // 加个后缀提示用户，体验更好
        bedNumber: row.bed.bedNumber + ' (当前占用)'
      })
    }
  }
}

//  保存
const handleSave = async () => {

  // console.log("3. 点击保存，提交的 form 数据:", form.value)
  // console.log("   检查 ID 是否存在:", form.value.id)
  if (form.value.id) {

    // console.log(">>> 走的是 [修改] 逻辑")
    await updateElderly(form.value)
    ElMessage.success('修改成功')
  } else {
    // console.log(">>> 走的是 [新增] 逻辑 (因为没找到 ID)")
    await addElderly(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

// 打开弹窗的方法
const openImagingDialog = async (row) => {
  currentElderName.value = row.name //设置弹窗为老人的名字
  currentElderId.value = row.id//记住是给几号老人上传影像
  imgDialogVisible.value = true //打开弹窗

  try {
    // 请求前先清空列表，显示"暂无数据"或Loading状态，防止显示上一个人的缓存
    imagingList.value = []

    // 发起请求 (注意这里加了 await)
    // 后端返回的是 Result<List<MedicalImaging>>
    // 根据你的 axios 拦截器封装，这里可能是 res.data 或者直接是 res
    const res = await selectByElderId(row.id)

    // 赋值给表格数据源
    imagingList.value = res

  } catch (error) {
    console.error("获取影像列表失败:", error)
    // 弹窗报错
     ElMessage.error("影像数据加载失败")
  }
}

// 处理下载影像的方法 (异步多线程版)
const handleDownload = async (row) => {
  try {
    // 1. 提示用户开始
    ElMessage.success({
      message: '正在请求服务器加速下载，请稍候...',
      duration: 3000
    })

    // --- 第一步：开启任务 ---
    const startRes = await startAsyncDownload(row.id)
    // 根据你的 axios 拦截器，这里可能是 startRes.taskId 或者 startRes.data.taskId
    // 做个兼容写法：
    const taskId = startRes.taskId || startRes.data?.taskId

    console.log(`任务已建立，Task ID: ${taskId}`)

    // --- 第二步：轮询状态 ---
    let isComplete = false

    // 开启循环，直到状态变为 COMPLETED
    while (!isComplete) {
      // 等待 2 秒，防止请求太频繁
      await new Promise(resolve => setTimeout(resolve, 2000))

      const statusRes = await getDownloadStatus(taskId)
      // 同样做兼容处理
      const statusInfo = statusRes.data || statusRes
      const status = statusInfo.status

      console.log(`当前下载状态: ${status}`)

      if (status === 'COMPLETED') {
        isComplete = true // 跳出循环
      } else if (status === 'FAILED') {
        throw new Error(statusInfo.errorMsg || '服务器下载失败')
      }
      // 如果是 RUNNING 或 PENDING，循环继续...
    }

    // --- 第三步：状态完成，拉取最终文件流 ---
    ElMessage.success('服务器下载完成，正在传输到本地...')

    // 注意：fetchDownloadedFile 在 api.js 里必须配置 responseType: 'blob'
    const blobData = await fetchDownloadedFile(taskId)

    // -------------------------------------------------------
    // 下面是通用的“浏览器强制下载二进制流”的标准写法 (和之前一样)
    // -------------------------------------------------------

    const blob = new Blob([blobData]) // 创建 Blob 对象
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)

    // 设置文件名
    link.download = row.fileName

    // 触发下载
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)

  } catch (error) {
    console.error("下载流程异常:", error)
    ElMessage.error(error.message || "下载出错，请稍后重试")
  }
}

//用于上传影像
const handleImgUploadSuccess = async (response, uploadFile) => {
  // response: 后端 /file/upload 返回的 Result，里面是 MinIO 的 URL
  // uploadFile: ElementPlus 封装的文件对象，里面有 name 和 size

  if (response.code === '200') {
    // 组装要存到数据库的数据
    const imgRecord = {
      elderId: currentElderId.value,      // 谁的片子
      fileUrl: response.data,             // MinIO 链接
      fileName: uploadFile.name,          // 文件名 (例如 xray.jpg)
      fileSize: uploadFile.size           // 文件大小 (字节)，给多线程下载用！
    }

    // 调用保存接口
    await addimage(imgRecord)

    ElMessage.success('影像上传归档成功')

    // 刷新列表，立马能看到刚传的图
    const res = await selectByElderId(currentElderId.value)
    imagingList.value = res
  } else {
    ElMessage.error('上传失败')
  }
}



//  点击删除
const handleDelete = (id) => {
  console.log("点击了删除，ID：", id)

  ElMessageBox.confirm('确定要删除这位老人吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 1. 调用真正的删除接口
      await deleteElderly(id)
      ElMessage.success('删除成功')
      // 2. 刷新表格
      loadData()
    } catch (error) {
      console.error(error)
    }
  })
}


// 添加老人 护工床位下拉表 加载选项数据

const loadOptions = async () => {


  const nurses = await selectAllEmployee()
  nurseList.value = nurses

  const beds = await selectFreeBed()
  bedList.value = beds
}


//新增老人
const handleAdd = () => {
  // 因为我们在“编辑”的时候往列表里塞了脏数据（占用的床），所以点击“新增”时，必须保证列表是干净的（只有空闲床）。
  form.value = { gender: '男', age: 70 } // 默认值
  formTitle.value = '新增老人'
  dialogVisible.value = true

  // 打开弹窗时，加载下拉框数据
  loadOptions()
}
</script>

<style scoped>
.container { padding: 20px; }
.action-bar { margin-bottom: 20px; }
</style>