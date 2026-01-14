<template>
  <div class="container">

    <!-- 新增 删除 按钮-->
    <div class="action-bar">

      <div class="search-area">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入姓名搜索"
            clearable
            @clear="loadData"
            style="width: 200px; margin-right: 10px;"
        />
        <el-button type="primary" @click="loadData">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </div>

      <div class="button-area">
        <el-button type="primary" @click="handleAdd " v-if="userRole === 'ADMIN'">
          <el-icon><Plus /></el-icon> 新增员工
        </el-button>

        <el-button type="danger" @click="handleBatchDelete" v-if="userRole === 'ADMIN'">
          <el-icon><Delete /></el-icon> 批量删除
        </el-button>

        <el-upload
            action="http://localhost:9090/employee/import"
            :headers="{ token: token }"
            :on-success="handleImportSuccess"
            :show-file-list="false"
        >
          <el-button type="success" v-if="userRole === 'ADMIN'">
            <el-icon><Upload /></el-icon> 导入
          </el-button>
        </el-upload>
        <el-button type="warning" @click="handleExport">
          <el-icon><Download /></el-icon> 导出
        </el-button>
      </div>

    </div>



    <!--表单数据 -->
    <el-table
        :data="tableData"
        stripe
        border
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
    >

      <el-table-column type="selection" width="55" align="center" />


      <el-table-column prop="name" label="姓名" width="100" align="center" />

      <el-table-column label="头像" width="80" align="center">
        <template #default="scope">
          <div style="display: flex; justify-content: center; align-items: center;">
            <el-image
                style="width: 40px; height: 40px; border-radius: 50%"
                :src="scope.row.avatar"
                :preview-src-list="[scope.row.avatar]"
                preview-teleported
            />
          </div>
        </template>
      </el-table-column>



      <el-table-column prop="gender" label="性别" width="60" align="center" />

      <el-table-column prop="age" label="年龄" width="60" align="center" />

      <el-table-column prop="phone" label="手机" width="120" align="center" />

      <el-table-column prop="address" label="地址" min-width="150" show-overflow-tooltip />

      <el-table-column prop="createTime" label="入职时间" width="110" align="center" />

      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="scope">
          <div v-if="userRole === 'ADMIN'">
            <el-button circle size="small" type="primary" @click="handleEdit(scope.row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button circle size="small" type="danger" @click="handleDelete(scope.row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div v-else>
            <span style="font-size: 12px; color: #999">无权限</span>
          </div>
        </template>
      </el-table-column>

    </el-table>

    <!-- 分页-->
    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :page-size="queryParams.pageSize"
          @current-change="handlePageChange"
      />
    </div>

    <!--新增/修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="formTitle" width="500px">
      <el-form :model="form" label-width="80px">
        <!--上传头像区域-->
        <el-form-item label="头像">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:9090/file/upload"
              :headers="{ token: token }"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :data="{ prefix: 'employee/avater/' }"
          >
            <img v-if="form.avatar" :src="form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
            <el-radio value="未知">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄">
          <el-select v-model.number="form.age" placeholder="请选择年龄" style="width: 100%">
            <el-option
                v-for="age in ageRangeList"
                :key="age"
                :label="age + ' 岁'"
                :value="age"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="入职时间">
          <el-date-picker
              v-model="form.createTime"
              type="date"
              placeholder="留空则默认当前时间"
              value-format="YYYY-MM-DD"
              style="width: 100%"
          />
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


// -------------------- 1. 引入依赖 --------------------
// 1. 引入 Vue 框架的核心工具
import {ref, onMounted, reactive} from 'vue'
// 2. 引入 Element Plus 的弹窗
import { ElMessage, ElMessageBox } from 'element-plus'
// 3.引入api的js文件 可以称为电话表
import { deleteEmployeeBatch,getEmployeePage, addEmployee, updateEmployee, deleteEmployee } from '@/api/employee'
//4 引入图标组件
import { Plus, Edit, Delete, Search, Refresh, Download,Upload} from '@element-plus/icons-vue'

// -------------------- 2. 定义状态变量 (State) --------------------
const loading = ref(false) // 控制“加载中”动画的状态
const tableData = ref([])// 表格要显示的“数据列表”
const dialogVisible = ref(false)// 控制“新增/修改弹窗”是否显示
const formTitle = ref('')// “新增/修改弹窗”的标题文字
const form = ref({})// “新增/修改弹窗”里表单的数据对象

// 定义一个变量存当前用户的角色
const userRole = ref('')





// 总条数 (给分页组件用)
const total = ref(0)


//新增：分页查询参数 (用 reactive 包装对象)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10 ,// 每页显示10条
  name: '' // 搜索关键词，默认为空
})

// 年龄范围数据
// 生成一个从 18 岁到 60 岁的数组
const ageRangeList = ref([])
const generateAgeRange = () => {
  for (let i = 1; i <= 70; i++) {
    ageRangeList.value.push(i)
  }
}

// 存储被选中的行数据
// 用于批量删除
const multipleSelection = ref([])

// 2. 获取 Token (给 el-upload 用) 导入表单
const token = localStorage.getItem('token')

// -------------------- 3. 定义核心函数 (Functions) --------------------

// loadData 函数
const loadData = async () => {
  loading.value = true
  try {
    // 调用分页接口，传入 queryParams
    const res = await getEmployeePage(queryParams)

    // ⬇️ 后端返回的是 PageInfo 对象
    tableData.value = res.records  // 拿到列表数据
    total.value = res.total     // 拿到总条数 (关键！)

  } catch (error) {
    console.error("加载数据失败:", error)
  } finally {
    loading.value = false
  }
}

// 处理页码改变
const handlePageChange = (newPageNum) => {
  queryParams.pageNum = newPageNum // 更新参数里的页码
  loadData() // 重新加载数据
}

// 2. 页面“挂载”时，自动调用一次加载数据 (不变)
onMounted(() => {
  loadData()
  generateAgeRange() // 页面加载时调用，生成年龄数组


  //新增：获取当前用户角色
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const userObj = JSON.parse(userStr)
    userRole.value = userObj.role // 拿到 'ADMIN' 或 'USER'
  }


})


//新增员工函数
const handleAdd = () => {
  //给新增表单设置默认值，提升体验
  //显式定义 avatar 为空字符串，确保 Vue 追踪这个属性 用于头像显示
  form.value = {
    avatar: ''
  }

  formTitle.value = '新增员工'
  dialogVisible.value = true
}


const handleEdit = (row) => {
  form.value = Object.assign({}, row)
  formTitle.value = '修改员工'
  dialogVisible.value = true
}


const handleSave = async () => {
  // 1. 打印日志，证明按钮点击生效了
  console.log("1. 点击了保存按钮，开始执行...")
  console.log("2. 当前表单数据:", form.value)

  try {
    if (form.value.id) {
      console.log("3. 正在调用更新接口...")
      await updateEmployee(form.value)
      ElMessage.success('修改成功')
    } else {
      console.log("3. 正在调用新增接口...")
      // ⬇️ 关键点：如果这一步报错（比如后端没开，或者400/500），代码会直接跳到 catch
      await addEmployee(form.value)
      ElMessage.success('新增成功')
    }

    console.log("4. 接口调用成功，准备关闭弹窗")
    dialogVisible.value = false // 关闭弹窗
    loadData() // 刷新表格

  } catch (error) {
    // ⬇️ 如果你看到这条红色的日志，说明接口失败了
    console.error("❌ 保存失败，流程终止:", error)
    // 此时 dialogVisible.value = false 不会执行，所以弹窗不会关，看着像“没反应”
  }
}


const handleDelete = (id) => {
  ElMessageBox.confirm(
      '确定要删除该员工吗? 此操作不可撤销。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(async () => { // ⬇️ 修正点
        try {
          await deleteEmployee(id)
          ElMessage.success('删除成功')
          loadData() // 刷新表格
        } catch (error) {
          console.error("删除失败:", error)
        }
      })
      .catch(() => { // ⬇️ 修正点
        ElMessage.info('已取消删除')
      })
}

// 1. 处理表格选中状态改变的事件 用于批量选中
// Element Plus 会把当前所有选中的行对象作为参数传给这个函数
const handleSelectionChange = (val) => {
  multipleSelection.value = val
  // 此时 multipleSelection.value 存储的是 Employee 对象的数组
}


// 处理批量删除的逻辑
const handleBatchDelete = () => {
  // 1. 检查是否选中了数据
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条数据进行删除')
    return
  }

  // 2. 提取出所有被选中项的 ID 列表
  const ids = multipleSelection.value.map(item => item.id)

  // 3. 弹窗确认
  ElMessageBox.confirm(
      '确定要删除选中的 ' + ids.length + ' 个员工吗? 此操作不可撤销。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(async () => {
        await deleteEmployeeBatch(ids)
        ElMessage.success('批量删除成功')
        loadData()
      })
      .catch((err) => { // ⬅️ 1. 接收错误信息 err
        // ⬇️ 2. 判断错误类型
        if (err === 'cancel') {
          // 只有错误信息是 'cancel' 时，才是用户点的取消
          ElMessage.info('已取消批量删除')
        } else {
          // 否则，就是真正的报错（API 失败等）
          console.error('批量删除出错:', err)
          // 这里的错误通常 request.js 拦截器已经弹窗了，所以这里只需要打印日志
        }
      })
}

// 搜索框 重置逻辑
const handleReset = () => {
  queryParams.name = '' // 清空搜索框
  queryParams.pageNum = 1 // 重置回第一页
  loadData() // 重新加载数据
}

// 头像上传成功的回调函数
const handleAvatarSuccess = (response) => {
  // response 就是后端返回的 Result 对象
  console.log("上传成功，后端返回：", response)
  if (response.code === '200') {
    // 把图片 URL 填入表单
    form.value.avatar = response.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error('上传失败：' + response.msg)
  }
}

// 导出函数
const handleExport = () => {
  // 直接访问后端的导出接口
  // 注意：必须带上 token！但 window.open 没法自动带 Header 里的 token
  // 简易做法：直接打开 (如果后端拦截器放行了 export)
  // 标准做法：如果后端拦截器没放行，需要在 URL 里拼上 ?token=... 或者改用 axios 下载

  // 既然你的 WebConfig 拦截了所有请求，我们需要先去 WebConfig 放行 /employee/export
  // 或者用稍微复杂一点的 axios 下载流文件

  // 我们先用最简单的方式：去后端放行！
  window.open('http://localhost:9090/employee/export')
}


// 3. 定义导入成功的回调
const handleImportSuccess = (response) => {
  if (response.code === '200') {
    ElMessage.success('导入成功')
    loadData() // 刷新表格，显示新导入的数据
  } else {
    ElMessage.error(response.msg)
  }
}

</script>


<style scoped>
.container {
  padding: 20px;
}

/* 修改 action-bar 的样式，让两边对齐 */
.action-bar {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between; /* 左边搜索，右边按钮 */
  align-items: center;
}

.search-area {
  display: flex;
  align-items: center;
}

/* 上传图片部分的CSS美化 */

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px; /* 让加号垂直居中 */
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover; /* 防止图片变形 */
}

.button-area {
  /* 开启 Flex 布局，让子元素横向排列 */
  display: flex;

  /* 关键 1：垂直居中对齐 (解决“高度不一致/歪了”的问题) */
  align-items: center;

  /* 关键 2：统一间距 (解决“挨在一起”的问题) */
  /* gap 会自动在每个按钮之间加 10px，最左和最右不会加，非常智能 */
  gap: 10px;
}

/* 1. 去掉按钮默认边距 */
.button-area .el-button {
  margin-left: 0 !important;
}

/* 2. 修正 upload 组件的布局行为 */
.button-area .avatar-uploader,
.button-area .el-upload {
  display: flex; /* 确保它表现得像个弹性盒子，不会有多余间隙 */
  margin: 0;
}

</style>