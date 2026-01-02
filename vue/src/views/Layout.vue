<template>
  <div class="layout-container">
    <div class="aside">
      <div class="logo">智慧养老</div>

      <el-menu
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          :default-active="$route.path"
          router
          class="el-menu-vertical"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon> <span>系统首页</span>
        </el-menu-item>


        <el-menu-item index="/employee">
          <el-icon><User /></el-icon>
          <span>护工管理</span>
        </el-menu-item>

        <el-menu-item index="/elderly">
          <el-icon><User /></el-icon>
          <span>老人管理</span>
        </el-menu-item>

        <el-menu-item index="/bed">
          <el-icon><User /></el-icon>
          <span>床位管理</span>
        </el-menu-item>


        <el-menu-item index="/about">
          <el-icon><Setting /></el-icon>
          <span>关于系统</span>
        </el-menu-item>
      </el-menu>
    </div>

    <div class="main-content">
      <div class="header">
        <div style="flex: 1"></div>

        <div class="user-info">
          <el-dropdown>
          <span class="el-dropdown-link" style="display: flex; align-items: center; cursor: pointer">
            <el-avatar :size="30" :src="user.avatar" style="margin-right: 10px" />
            <span class="user-name">{{ user.name || user.username }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/person')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>


      <div class="content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { User, Setting } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router' // 引入路由工具
import { ElMessage } from 'element-plus'
import { ref, onMounted } from 'vue' // 引入 ref

const $route = useRoute() // 用来获取当前路径，高亮菜单
const router = useRouter() // 获取路由器实例 (跳转页面用)
// 定义一个变量存名字，默认叫管理员
const user = ref('管理员')


// 页面加载时，去兜里拿名字
onMounted(() => {
  const userStr = localStorage.getItem('user') //去兜里拿
  if (userStr) {
    try {
      // 尝试解析
      user.value = JSON.parse(userStr)
    } catch (e) {
      // 如果解析失败（说明数据坏了），就重置为空，并清理掉坏数据
      console.error("用户信息解析失败，已重置", e)
      localStorage.removeItem('user')
      user.value = {}
    }
  }
})


//退出登录的逻辑
const handleLogout = () => {
  // 1. 清除 Token
  localStorage.removeItem('token')
  localStorage.removeItem('user') // 清理用户名

  // 2. 提示
  ElMessage.success('退出成功')

  // 3. 跳转回登录页
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh; /* 全屏高度 */
}

.aside {
  width: 200px;
  background-color: #304156;
  color: white;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  background-color: #2b3649;
}

.el-menu-vertical {
  border-right: none; /* 去掉 Element 默认的白边 */
}

.main-content {
  flex: 1; /* 占满剩余空间 */
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5; /* 灰色背景 */
}

.header {
  height: 60px;
  background-color: white;
  display: flex;
  align-items: center;
  /*：两端对齐*/
  justify-content: space-between;

  padding-left: 20px;
  /*右边也留点空隙，不然按钮贴着屏幕边太丑了*/
  padding-right: 20px;

  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}


.user-info {
  display: flex;
  align-items: center; /* 让头像、文字、按钮垂直居中 */
  cursor: pointer;
}

.user-name {
  font-weight: bold;
  color: #333;
}


.content-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto; /* 内容多了可以滚动 */
}
</style>