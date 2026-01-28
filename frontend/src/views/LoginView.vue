<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title">智慧医疗管理系统</div>

      <el-tabs v-model="activeTab" stretch class="login-tabs">

        <el-tab-pane label="账号登录" name="account">
          <el-form :model="accountForm" :rules="accountRules" ref="accountRef">
            <el-form-item prop="username">
              <el-input
                  v-model="accountForm.username"
                  placeholder="请输入账号"
                  :prefix-icon="User"
                  size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                  v-model="accountForm.password"
                  type="password"
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  show-password
                  size="large"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="短信登录" name="mobile">
          <el-form :model="mobileForm" :rules="mobileRules" ref="mobileRef">
            <el-form-item prop="phone">
              <el-input
                  v-model="mobileForm.phone"
                  placeholder="请输入手机号"
                  :prefix-icon="Iphone"
                  size="large"
              />
            </el-form-item>
            <el-form-item prop="code">
              <div style="display: flex; width: 100%; gap: 10px;">
                <el-input
                    v-model="mobileForm.code"
                    placeholder="验证码"
                    :prefix-icon="ChatLineRound"
                    size="large"
                />
                <el-button
                    type="primary"
                    plain
                    :disabled="countDown > 0"
                    @click="sendCode"
                    style="width: 130px;"
                >
                  {{ countDown > 0 ? `${countDown}s后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div style="margin-top: 20px;">
        <el-button type="primary" style="width: 100%;" size="large" @click="handleLogin">
          {{ activeTab === 'account' ? '账 号 登 录' : '短 信 登 录' }}
        </el-button>
      </div>

      <div style="text-align: right; margin-top: 15px;">
        <el-link type="primary" href="/register">没有账号？去注册</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock, Iphone, ChatLineRound } from '@element-plus/icons-vue' // 记得引入新图标
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()
const activeTab = ref('account') // 默认显示账号登录

// --- 账号登录数据 ---
const accountRef = ref(null)
const accountForm = reactive({ username: '', password: '' })
const accountRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// --- 🌟 修改点 3：手机登录数据与逻辑 ---
const mobileRef = ref(null)
const mobileForm = reactive({ phone: '', code: '' })
const countDown = ref(0) // 倒计时变量

// 手机号正则校验
const validatePhone = (rule, value, callback) => {
  if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

const mobileRules = {
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 发送验证码逻辑
const sendCode = async () => {
  // 1. 先校验手机号格式是否正确
  mobileRef.value.validateField('phone', async (valid) => {
    if (valid) {
      try {

        await request.post(`/user/code?phone=${mobileForm.phone}`)

        ElMessage.success('验证码发送成功')

        // 2. 开启倒计时
        countDown.value = 60
        const timer = setInterval(() => {
          countDown.value--
          if (countDown.value <= 0) {
            clearInterval(timer)
          }
        }, 1000)
      } catch (error) {
        // 这里的报错通常会被你的 request.js 拦截并弹窗
        console.error("验证码发送失败:", error)
      }
    }
  })
}

// 统一登录逻辑
const handleLogin = () => {
  // 判断当前是哪个 Tab
  const isMobile = activeTab.value === 'mobile'
  const currentFormRef = isMobile ? mobileRef : accountRef

  // 校验当前表单
  currentFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let res;
        if (isMobile) {
          // 手机登录接口
          res = await request.post('/user/login/mobile', mobileForm)

        } else {

          res = await request.post('/user/login', accountForm)
        }


        // 存储 Token 和 用户信息
        localStorage.setItem('token', res.token)
        localStorage.setItem('user', JSON.stringify(res.user))

        ElMessage.success('登录成功')
        router.push('/') // 跳转首页

      } catch (error) {
        console.error("登录失败", error)
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
  background-color: #2d3a4b;
}

.login-box {
  width: 400px; /* 稍微加宽一点 */
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0,0,0,0.1);
}

.title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.login-tabs {
  margin-bottom: 20px;
}

/* 调整 Tabs 字体大小 */
:deep(.el-tabs__item) {
  font-size: 16px;
}
</style>