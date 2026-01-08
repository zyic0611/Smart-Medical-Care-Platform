<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title">后台管理系统</div>

      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" prefix-icon="User" size="large"/>
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large"/>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="width: 100%;" size="large" @click="handleLogin">登 录</el-button>
        </el-form-item>

        <div style="text-align: right;">
          <el-link type="primary" href="/register">没有账号？去注册</el-link>
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
// ⬇️⬇️⬇️ 1. 引入我们封装好的“电话总机”
import request from '@/api/request'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const formRef = ref(null)

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

//登录逻辑
const handleLogin = () => {
  // 校验表单
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // A. 向后端发 POST 请求
        // 这里的 form 就是 { username: '...', password: '...' }
        // 因为 request.js 里配置了 baseURL，它会自动发给 http://localhost:9090/login
        const res = await request.post('/login', form)

        // B. 【关键】把后端给的 Token 存进浏览器
        // 以后每次发请求，request.js 会自动从这里取出来放在 Header 里
        localStorage.setItem('token', res.token)

        // 把用户名也存起来
        localStorage.setItem('user', JSON.stringify(res.user))

        // C. 提示成功并跳转
        ElMessage.success('登录成功')
        router.push('/') // 跳转到首页 (Layout)

      } catch (error) {
        // 登录失败（比如密码错），拦截器会弹窗提示，这里打印一下即可
        console.error("登录出错:", error)
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2d3a4b; /* 深色背景 */
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