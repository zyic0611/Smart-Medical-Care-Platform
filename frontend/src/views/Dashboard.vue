<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="mb-4">
      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-blue">
          <template #header>
            <div class="card-header">
              <span>🛏️ 总床位数</span>
            </div>
          </template>
          <div class="card-value">{{ stats.totalBeds || 0 }}</div>
          <div class="card-desc">当前空闲：{{ stats.totalBeds - stats.occupiedBeds }}</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-green">
          <template #header>
            <div class="card-header">
              <span>👴 在院老人</span>
            </div>
          </template>
          <div class="card-value">{{ stats.elderlyCount || 0 }}</div>
          <div class="card-desc">健康占比高</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-orange">
          <template #header>
            <div class="card-header">
              <span>📊 今天的入住率</span>
            </div>
          </template>
          <div class="card-value">{{ stats.occupancyRate || 0 }}%</div>
          <el-progress :percentage="Number(stats.occupancyRate) || 0" :status="getRateStatus(stats.occupancyRate)" />
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-purple">
          <template #header>
            <div class="card-header">
              <span>👨‍⚕️ 在职护工</span>
            </div>
          </template>
          <div class="card-value">{{ stats.employeeCount || 0 }}</div>
          <div class="card-desc">全勤在岗</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">📈 近七天入院人数趋势</div>
          </template>
          <div id="trend-chart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">🩺 老人健康评级分布</div>
          </template>
          <div id="health-chart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">👥 护工性别比例</div>
          </template>
          <div id="emp-chart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

// 响应式数据，存储后端返回的统计结果
const stats = reactive({
  totalBeds: 0,
  occupiedBeds: 0,
  elderlyCount: 0,
  employeeCount: 0,
  occupancyRate: 0
})

onMounted(() => {
  loadDashboardData()
})

// 根据入住率返回进度条颜色
const getRateStatus = (rate) => {
  if (rate > 90) return 'exception' // 红色警告
  if (rate > 70) return 'warning'   // 黄色提醒
  return 'success'                  // 绿色健康
}

const loadDashboardData = async () => {
  // 1. 发起请求。此时的 res 已经是后台 Result 里的那个 Map 了
  const res = await request.get('/echarts/dashboard')

  console.log("接口返回原始数据:", res)

  // 2. 直接使用 res 进行赋值，不要再写 res.data 了
  // 注意：既然 request.js 已经剥离了层级，通常也不需要判断 res.code === '200'
  // 因为失败的情况通常已经在 request.js 拦截器里弹窗报错了
  if (res) {
    // 填充顶部卡片
    stats.totalBeds = res.totalBeds
    stats.occupiedBeds = res.occupiedBeds
    stats.elderlyCount = res.elderlyCount
    stats.employeeCount = res.employeeCount
    stats.occupancyRate = res.occupancyRate

    // 渲染图表
    nextTick(() => {
      initTrendChart(res.trendDates, res.trendValues)
      initHealthChart(res.healthPie)
      initEmpChart(res.empPie)
    })
  } else {
    console.error('获取数据失败：返回结果为空')
  }
}

// --- 初始化折线图 ---
const initTrendChart = (dates, values) => {
  const chart = echarts.init(document.getElementById('trend-chart'))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates },
    yAxis: { type: 'value' },
    series: [{
      name: '入院人数',
      type: 'line',
      smooth: true, // 平滑曲线
      data: values,
      areaStyle: { opacity: 0.3 }, // 填充颜色
      itemStyle: { color: '#409EFF' }
    }]
  })
}

// --- 初始化健康饼图 ---
const initHealthChart = (data) => {
  const chart = echarts.init(document.getElementById('health-chart'))
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%' },
    color: ['#67C23A', '#E6A23C', '#F56C6C'], // 对应 健康(绿)、一般(黄)、严重(红)
    series: [{
      name: '健康等级',
      type: 'pie',
      radius: ['40%', '70%'], // 环形图
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
      data: data
    }]
  })
}

// --- 初始化护工饼图 ---
const initEmpChart = (data) => {
  const chart = echarts.init(document.getElementById('emp-chart'))
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%' },
    series: [{
      name: '性别',
      type: 'pie',
      radius: '50%',
      data: data,
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }]
  })
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa; /* 浅灰底色，更有质感 */
  min-height: 100vh;
}
.mb-4 {
  margin-bottom: 20px;
}

/* 卡片通用样式 */
.data-card {
  color: #fff;
  transition: transform 0.3s;
}
.data-card:hover {
  transform: translateY(-5px); /* 鼠标悬浮上浮效果 */
}
.card-header {
  font-size: 16px;
  font-weight: bold;
}
.card-value {
  font-size: 28px;
  font-weight: bold;
  margin: 10px 0;
}
.card-desc {
  font-size: 12px;
  opacity: 0.8;
}

/* 卡片特定背景色 */
.bg-blue { background: linear-gradient(135deg, #36D1DC, #5B86E5); }
.bg-green { background: linear-gradient(135deg, #42e695, #3bb2b8); }
.bg-orange { background: linear-gradient(135deg, #FFB75E, #ED8F03); }
.bg-purple { background: linear-gradient(135deg, #E09FFF, #6772fa); }

/* 图表标题 */
.chart-header {
  font-weight: bold;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}
</style>