<template>
  <div class="container">
    <el-card>
      <div id="pie-chart" style="width: 100%; height: 400px;"></div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
// 引入 ECharts
import * as echarts from 'echarts'
import request from '@/api/request'

onMounted(() => {
  initCharts()
})

const initCharts = async () => {
  // 1. 去后端拿数据
  const res = await request.get('/echarts/data')
  // res.pie 就是我们在后端组装的那个 List

  // 2. 初始化图表
  // 必须保证有一个 id="pie-chart" 的 div 已经存在
  const myChart = echarts.init(document.getElementById('pie-chart'))

  // 3. 配置图表 (这一坨配置是 ECharts 官方标准写法)
  const option = {
    title: {
      text: '员工性别比例统计',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '性别',
        type: 'pie', // 饼图
        radius: '50%',
        data: res.pie, // ⬅️ 关键！把后端的数据填在这里
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  // 4. 渲染
  myChart.setOption(option)
}
</script>

<style scoped>
.container {
  padding: 20px;
}
</style>