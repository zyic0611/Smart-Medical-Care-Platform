<template>
  <div class="container">
    <el-row :gutter="20">

      <el-col :span="12">
        <el-card header="个人信息修改">
          <el-form :model="form" label-width="80px">

            <el-form-item label="头像">
              <el-upload
                  class="avatar-uploader"
                  action="http://localhost:9090/file/upload"
                  :headers="{ token: token }"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :data="{ prefix: 'user/avatar/' }"
              >
                <img v-if="form.avatar" :src="form.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>

            <el-form-item label="账号">
              <el-input v-model="form.username" disabled placeholder="账号不支持修改"></el-input>
            </el-form-item>

            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称"></el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleUpdateInfo">保存信息</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card header="安全设置">
          <el-form :model="passForm" :rules="rules" ref="passRef" label-width="100px">
            <el-form-item label="旧密码" prop="oldPass">
              <el-input v-model="passForm.oldPass" show-password placeholder="请输入旧密码"></el-input>
            </el-form-item>
            <el-form-item label="新密码" prop="newPass">
              <el-input v-model="passForm.newPass" show-password placeholder="请输入新密码"></el-input>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPass">
              <el-input v-model="passForm.confirmPass" show-password placeholder="请再次输入新密码"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="danger" @click="handleUpdatePass">确认修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()
const token = localStorage.getItem('token') // 上传头像需要 Token

// --- 1. 个人信息部分 ---
const form = ref({}) // 存储当前用户信息

// 初始化：从缓存里把用户信息拿出来回显
onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    form.value = JSON.parse(userStr)
  }
})

// 头像上传成功回调
const handleAvatarSuccess = (res) => {
  if (res.code === '200') {
    form.value.avatar = res.data
    ElMessage.success('头像上传成功，别忘了点保存哦')
  } else {
    ElMessage.error(res.msg)
  }
}

// 保存个人信息
const handleUpdateInfo = async () => {
  try {
    // 发送请求更新数据库
    // 注意：后端接口是 /updateUser，返回最新的 user 对象
    const res = await request.put('/updateUser', form.value)

    ElMessage.success('信息更新成功')

    // 关键：更新本地缓存！这样右上角的头像和名字才会变
    localStorage.setItem('user', JSON.stringify(res))

    // 刷新页面，让 Layout 里的 Header 重新读取缓存显示新头像
    // (或者用 location.reload() 简单粗暴)
    setTimeout(() => {
      location.reload()
    }, 500)

  } catch (error) {
    console.error(error)
  }
}

// --- 2. 修改密码部分 ---
const passRef = ref(null)
const passForm = reactive({
  oldPass: '',
  newPass: '',
  confirmPass: ''
})

// 密码校验规则
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== passForm.newPass) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}
const rules = {
  oldPass: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPass: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPass: [{ validator: validatePass2, trigger: 'blur' }]
}

// 确认修改密码
const handleUpdatePass = () => {
  passRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.put('/updatePassword', passForm)
        ElMessage.success('密码修改成功，请重新登录')
        // 强制退出
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      } catch (error) {
        console.error(error)
      }
    }
  })
}
</script>

<style scoped>
.container {
  padding: 20px;
}

/* 头像上传样式 (复用之前的) */
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
  line-height: 100px;
}
.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}
</style>