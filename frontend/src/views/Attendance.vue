<template>
  <div class="attendance-container">
    <div class="header-banner">
      <div class="user-info">
        <el-avatar :size="50" :src="userInfo.avatar" />
        <div class="text-group">
          <span class="welcome-text">您好，{{ userInfo.nickname }}</span>
          <span class="role-tag">{{ userInfo.role === 'DOCTOR' ? '医生' : '护士' }}</span>
        </div>
      </div>
      <div class="timer-area">
        <div class="current-date">{{ currentTime.date }}</div>
        <div class="current-time">{{ currentTime.time }}</div>
      </div>
    </div>

    <el-container class="main-layout">
      <el-aside width="400px" class="punch-panel">
        <el-card shadow="never" class="punch-card">
          <template #header>
            <div class="card-header">
              <span>工作台</span>
              <el-tag type="success" effect="plain">今日已入场</el-tag>
            </div>
          </template>

          <div class="punch-button-wrapper">
            <div
                :class="['punch-circle', isPunchedOut ? 'is-disabled' : '']"
                @click="handlePunch"
                v-loading="punchLoading"
            >
              <el-icon :size="40"><Pointer /></el-icon>
              <span class="punch-text">{{ punchStatusText }}</span>
              <span class="punch-time">{{ currentTime.time }}</span>
            </div>
          </div>

          <div class="location-info">
            <el-icon><Location /></el-icon>
            <span>智慧养老院 - 数字化考勤终端</span>
          </div>
        </el-card>
      </el-aside>

      <el-main class="history-area">
        <div class="history-header">
          <span class="title">最近 15 次打卡记录</span>
          <el-button link type="primary" @click="loadHistory">刷新记录</el-button>
        </div>

        <el-table :data="historyList" v-loading="loading" border stripe class="history-table">
          <el-table-column prop="workDate" label="日期" width="120" />
          <el-table-column label="上班时间" width="180">
            <template #default="scope">
              <el-tag v-if="scope.row.checkInTime" type="info" effect="plain">
                {{ formatTime(scope.row.checkInTime) }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="下班时间" width="180">
            <template #default="scope">
              <el-tag v-if="scope.row.checkOutTime" type="success" effect="plain">
                {{ formatTime(scope.row.checkOutTime) }}
              </el-tag>
              <el-tag v-else type="warning">待签退</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="workHours" label="工时(h)" align="center" />
          <el-table-column label="状态" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isLate ? 'danger' : 'success'">
                {{ scope.row.isLate ? '迟到' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted,computed } from 'vue'
import { Pointer, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { punch, getRecentRecords } from '@/api/attendance'

const userInfo = ref({})
const loading = ref(false)
const punchLoading = ref(false)
const historyList = ref([])
const currentTime = reactive({ date: '', time: '' })

// 定时器，更新界面时间
let timer = null
const updateTime = () => {
  const now = new Date()
  currentTime.date = now.toLocaleDateString()
  currentTime.time = now.toTimeString().substring(0, 8)
}

// 加载历史记录
const loadHistory = async () => {
  loading.value = true
  try {
    // 这里的 res 拿到的就是后端 Result 里的 data（即 IPage 对象）
    const res = await request.get('/api/attendance/recent')

    // 🌟 核心修改：直接访问 res.records
    if (res && res.records) {
      historyList.value = res.records
    } else {
      historyList.value = []
      console.warn('后端返回了分页对象，但 records 字段为空')
    }
  } catch (error) {
    console.error('加载考勤历史异常:', error)
  } finally {
    loading.value = false
  }
}

// 执行打卡
const handlePunch = async () => {
  punchLoading.value = true
  try {
    const res = await punch()
    ElMessage.success(res.data || '打卡成功') // 这里的 res.data 是后端返回的 String
    loadHistory()
  } catch (e) {
    // 拦截器通常已经报过错，这里可以保持静默或处理特定逻辑
  } finally {
    punchLoading.value = false
  }
}

// 辅助格式化
const formatTime = (timeStr) => {
  if (!timeStr) return '-'

  // 1. 如果包含空格，说明是 "yyyy-MM-dd HH:mm:ss"
  if (typeof timeStr === 'string' && timeStr.includes(' ')) {
    return timeStr.split(' ')[1]
  }

  // 2. 如果包含 T，说明是 ISO 格式 "yyyy-MM-ddTHH:mm:ss"
  if (typeof timeStr === 'string' && timeStr.includes('T')) {
    return timeStr.split('T')[1].substring(0, 8)
  }

  // 3. 如果是个数组 [2026, 1, 15, 8, 30] (某些老版本 SpringBoot 默认行为)
  if (Array.isArray(timeStr)) {
    const h = String(timeStr[3]).padStart(2, '0')
    const m = String(timeStr[4]).padStart(2, '0')
    const s = String(timeStr[5] || 0).padStart(2, '0')
    return `${h}:${m}:${s}`
  }

  return timeStr
}

const punchStatusText = computed(() => {
  // 1. 获取今天的日期字符串 (需与后端 workDate 格式匹配，通常是 yyyy-MM-dd)
  const today = new Date().toISOString().split('T')[0]

  // 2. 查找今天是否有记录
  const todayRecord = historyList.value.find(item => item.workDate === today)

  if (!todayRecord) {
    return "上班打卡"
  } else if (!todayRecord.checkOutTime) {
    return "下班打卡"
  } else {
    return "已签退"
  }
})

// 增加一个计算属性：如果今天已经打完两次卡，禁用按钮
const isPunchedOut = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  const todayRecord = historyList.value.find(item => item.workDate === today)
  return !!(todayRecord && todayRecord.checkOutTime)
})

onMounted(() => {
  userInfo.value = JSON.parse(localStorage.getItem('user') || '{}')
  updateTime()
  timer = setInterval(updateTime, 1000)
  loadHistory()
})

onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.attendance-container { padding: 15px; background: #f5f7fa; min-height: 100vh; }

.header-banner {
  background: linear-gradient(135deg, #409EFF 0%, #67c23a 100%);
  padding: 20px 30px;
  border-radius: 12px;
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}

.user-info { display: flex; align-items: center; gap: 15px; }
.welcome-text { font-size: 20px; font-weight: bold; display: block; }
.role-tag { font-size: 12px; background: rgba(255,255,255,0.2); padding: 2px 8px; border-radius: 4px; }

.timer-area { text-align: right; }
.current-date { font-size: 16px; opacity: 0.9; }
.current-time { font-size: 32px; font-weight: bold; font-family: 'Courier New', Courier, monospace; }

.punch-panel { margin-right: 15px; }
.punch-card { height: calc(100vh - 180px); display: flex; flex-direction: column; }

.punch-button-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

/* 圆形打卡按钮样式 */
.punch-circle {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: #fff;
  border: 8px solid #f0f9eb;
  box-shadow: 0 10px 30px rgba(103, 194, 58, 0.2);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  color: #67c23a;
}

.punch-circle:hover { transform: scale(1.05); box-shadow: 0 15px 40px rgba(103, 194, 58, 0.3); }
.punch-circle:active { transform: scale(0.95); }

.punch-text { font-size: 24px; font-weight: bold; margin: 10px 0; }
.punch-time { font-size: 14px; color: #909399; }

.location-info { text-align: center; color: #909399; font-size: 13px; margin-top: 20px; }

.history-area { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.history-header .title { font-size: 16px; font-weight: bold; color: #303133; }
</style>