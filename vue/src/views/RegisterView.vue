<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title">欢迎注册</div>

      <el-form :model="form" :rules="rules" ref="formRef">

        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" prefix-icon="User" size="large"/>
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large"/>
        </el-form-item>

        <el-form-item prop="confirmPass">
          <el-input v-model="form.confirmPass" type="password" placeholder="请再次输入密码" prefix-icon="Lock" show-password size="large"/>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="width: 100%;" size="large" @click="handleRegister">注 册</el-button>
        </el-form-item>

        <div style="text-align: right;">
          <el-link type="primary" href="/login">已有账号？去登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/api/request' // 引入请求工具

const router = useRouter()
const formRef = ref(null)
const form = reactive({
  username: '',
  password: '',
  confirmPass: ''
})

// 自定义校验规则：检查两次密码是否一致
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPass: [{ validator: validatePass2, trigger: 'blur' }] // 使用自定义校验
}

const handleRegister = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 调用注册接口
        await request.post('/register', form)

        ElMessage.success('注册成功，请登录')
        router.push('/login') // 注册成功跳回登录页

      } catch (error) {
        console.error("注册失败:", error)
      }
    }
  })
}
</script>

<style scoped>
/* 复用登录页的样式，或者直接复制过来 */
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2d3a4b;
}
.login-box {
  width: 350px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0,0,0,0.1);
}
.title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 30px;
  color: #333;
}
</style>