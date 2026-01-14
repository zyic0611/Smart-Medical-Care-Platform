<template>
  <div class="workstation-container">
    <div class="header">
      <div class="logo">
        <div class="logo-icon-bg">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="logo-text">
          <span class="main-title">YOLOv11</span>
          <span class="sub-title">智能医疗影像辅助诊断系统</span>
        </div>
      </div>
      <el-button
          type="primary"
          plain
          round
          @click="resetWorkstation"
          v-if="currentImageUrl || taskId"
          class="reset-btn"
      >
        <el-icon class="el-icon--left"><RefreshLeft /></el-icon> 重置 / 上传新影像
      </el-button>
    </div>

    <div class="content-wrapper">
      <div class="elder-list-section" v-if="!currentImageUrl && !taskId">
        <transition name="el-fade-in-linear">
          <el-card shadow="hover" class="custom-card">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-icon class="header-icon"><User /></el-icon>
                  <span>待诊断患者列表</span>
                </div>
                <el-tag type="info" effect="plain" round size="small">共 {{ elderPage.total }} 人</el-tag>
              </div>
            </template>

            <div class="elder-list-content">
              <el-table
                  :data="elderPage.records"
                  style="width: 100%"
                  v-loading="elderLoading"
                  :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
                  highlight-current-row
              >
                <el-table-column prop="id" label="ID" width="80" align="center">
                  <template #default="scope">
                    <span class="id-tag">#{{ scope.row.id }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="姓名" width="100" align="center" font-weight="bold"/>
                <el-table-column prop="gender" label="性别" width="80" align="center">
                  <template #default="scope">
                    <el-tag :type="scope.row.gender === '男' ? '' : 'danger'" size="small" effect="light">
                      {{ scope.row.gender }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="age" label="年龄" width="80" align="center" />
                <el-table-column prop="healthStatus" label="健康状态" align="center">
                  <template #default="scope">
                    <el-tag v-if="scope.row.healthStatus === '健康'" type="success" size="small" effect="dark">健康</el-tag>
                    <el-tag v-else-if="scope.row.healthStatus === '异常'" type="warning" size="small" effect="dark">异常</el-tag>
                    <el-tag v-else type="info" size="small">{{ scope.row.healthStatus }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createTime" label="入住时间" min-width="160" align="center" />
                <el-table-column label="操作" width="120" align="center" fixed="right">
                  <template #default="scope">
                    <el-button type="primary" text bg size="small" disabled>查看影像</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-container">
                <el-pagination
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    :current-page="elderPage.current"
                    :page-sizes="[5, 10, 20]"
                    :page-size="elderPage.size"
                    layout="total, sizes, prev, pager, next"
                    :total="elderPage.total"
                    background
                >
                </el-pagination>
              </div>
            </div>
          </el-card>
        </transition>
      </div>

      <div class="main-content" :class="{ 'is-centered': !currentImageUrl && !taskId }">
        <div v-if="!currentImageUrl && !taskId">
          <transition name="el-zoom-in-center">
            <div class="upload-zone">
              <el-upload
                  class="upload-dragger-custom"
                  drag
                  action="#"
                  :http-request="customUpload"
                  :show-file-list="false"
                  :before-upload="beforeUpload"
              >
                <div class="upload-inner">
                  <div class="icon-wrapper">
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  </div>
                  <div class="upload-text">
                    <h3>点击或拖拽上传影像</h3>
                    <p>支持 DICOM / JPG / PNG 格式 (Max 50MB)</p>
                  </div>
                </div>
              </el-upload>
            </div>
          </transition>
        </div>
        <div v-else class="diagnosis-layout">
          <div class="image-viewer" v-loading="loading" element-loading-background="rgba(0, 0, 0, 0.8)" :element-loading-text="'AI 正在逐层扫描影像...'">
            <el-image
                :src="showAnnotated ? aiProcessedUrl : currentImageUrl || `https://via.placeholder.com/800x800/2c3e50/ffffff?text=TaskID:${taskId}`"
                fit="contain"
                class="main-image"
                :preview-src-list="[aiProcessedUrl || currentImageUrl]"
                v-if="currentImageUrl || aiProcessedUrl"
            />
            <div class="dicom-placeholder" v-else>
              <div class="placeholder-icon">
                <el-icon><Picture /></el-icon>
              </div>
              <p class="placeholder-title">DICOM 序列已就绪</p>
              <p class="placeholder-subtitle">TaskID: {{ taskId }}</p>
            </div>

            <div class="image-controls glass-effect">
              <el-radio-group v-model="showAnnotated" :disabled="!aiProcessedUrl" size="small" fill="#409eff">
                <el-radio-button :value="false">原始影像</el-radio-button>
                <el-radio-button :value="true">AI 标注视图</el-radio-button>
              </el-radio-group>
              <el-divider direction="vertical" />
              <el-tag v-if="aiProcessedUrl" type="success" effect="dark" round class="status-tag">
                <el-icon><CircleCheckFilled /></el-icon> 分析完成
              </el-tag>
              <el-tag v-else type="info" effect="dark" round class="status-tag">
                等待分析
              </el-tag>
            </div>
          </div>

          <div class="report-panel">
            <div class="panel-header">
              <h3>诊断报告</h3>
              <el-button type="primary" size="default" @click="startDiagnosis" v-if="!aiProcessedUrl && !loading" :icon="Aim">
                开始 AI 分析
              </el-button>
            </div>

            <el-scrollbar class="panel-body">
              <div class="report-section">
                <h4 class="section-title"><el-icon><Aim /></el-icon> 智能检测结果</h4>

                <div v-if="aiFindings.length > 0" class="finding-list">
                  <div v-for="(item, index) in aiFindings" :key="index" class="finding-card">
                    <div class="card-row">
                      <span class="disease-name">{{ item.label }}</span>
                      <el-tag size="small" :type="getConfType(item.confidence)" effect="light">
                        {{ formatConfidence(item.confidence) }} 置信度
                      </el-tag>
                    </div>
                    <el-progress
                        :percentage="parseFloat(formatConfidence(item.confidence))"
                        :status="getConfType(item.confidence) === 'danger' ? 'exception' : ''"
                        :stroke-width="6"
                        :show-text="false"
                        class="custom-progress"
                    />
                  </div>
                </div>

                <el-empty v-else-if="!loading && !aiProcessedUrl" description="请点击上方按钮开始分析" :image-size="80" />
                <div v-else-if="loading" class="analyzing-placeholder">
                  <div class="pulse-ring"></div>
                  <p>YOLOv11 正在计算...（TaskID: {{ taskId }}）</p>
                </div>
                <el-alert v-else title="未发现明显异常" type="success" :closable="false" show-icon center effect="dark"/>
              </div>

              <div class="report-section">
                <h4 class="section-title"><el-icon><Edit /></el-icon> 医生复核意见</h4>
                <el-input
                    v-model="doctorNote"
                    type="textarea"
                    :rows="8"
                    placeholder="请输入最终诊断结论..."
                    resize="none"
                    class="doctor-input"
                />
              </div>
            </el-scrollbar>

            <div class="panel-footer">
              <el-button type="primary" size="large" style="width: 100%" @click="saveReport" :disabled="!aiProcessedUrl" class="save-btn">
                生成并打印报告
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UploadFilled, Monitor, Aim, Edit, RefreshLeft, Picture, User, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage, ElLoading } from 'element-plus'
import { uploadDiagnosisImage, getDiagnosisResult } from '@/api/diagnosis.js'
import { getElderlyWithImgPage } from '@/api/elderly.js'

const elderLoading = ref(false)
const elderPage = ref({
  current: 1,
  size: 10,
  total: 0,
  records: []
})

const loading = ref(false)
const currentImageUrl = ref('')
const aiProcessedUrl = ref('')
const showAnnotated = ref(false)
const aiFindings = ref([])
const doctorNote = ref('')
const taskId = ref('')
const diagnosing = ref(false)
const diagnosisResult = ref(null)

const loadElderList = async () => {
  elderLoading.value = true
  try {
    const res = await getElderlyWithImgPage({
      pageNum: elderPage.value.current,
      pageSize: elderPage.value.size
    })
    elderPage.value = {
      current: res.current || 1,
      size: res.size || 10,
      total: res.total || 0,
      records: res.records || []
    }
  } catch (error) {
    console.error('加载老人列表失败:', error)
    elderPage.value.records = []
    elderPage.value.total = 0
  } finally {
    elderLoading.value = false
  }
}

const handleSizeChange = (newSize) => {
  elderPage.value.size = newSize
  elderPage.value.current = 1
  loadElderList()
}

const handleCurrentChange = (newPage) => {
  elderPage.value.current = newPage
  loadElderList()
}

const beforeUpload = (file) => {
  const fileName = file.name.toLowerCase()
  const isAllowed = fileName.endsWith('.dcm') ||
      fileName.endsWith('.jpg') ||
      fileName.endsWith('.png') ||
      file.type.startsWith('image/')

  if (!isAllowed) {
    ElMessage.error('仅支持 DICOM(.dcm) / JPG / PNG 格式')
    return false
  }

  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }

  return true
}

const customUpload = async (options) => {
  const { file } = options
  let loadingInstance = null

  try {
    loadingInstance = ElLoading.service({
      text: '正在加密上传影像数据...',
      background: 'rgba(255, 255, 255, 0.8)',
    })

    const res = await uploadDiagnosisImage(file)
    taskId.value = res
    if (file.type.startsWith('image/')) {
      currentImageUrl.value = URL.createObjectURL(file)
    }
    ElMessage.success(`影像上传成功（TaskID: ${taskId.value}），请开始分析`)

  } catch (error) {
    console.error('上传错误:', error)
    ElMessage.error(error.message || '网络连接失败')
  } finally {
    if (loadingInstance) loadingInstance.close()
  }
}

const startDiagnosis = async () => {
  if (!taskId.value) {
    ElMessage.warning('请先上传文件！')
    return
  }
  loading.value = true
  diagnosing.value = true
  diagnosisResult.value = {status: 'processing'}

  const pollInterval = setInterval(async () => {
    try {
      // 1. 发起请求
      const res = await getDiagnosisResult(taskId.value)

      // 🔍 调试日志 (看到 processing 说明一切正常)
      console.log('轮询状态:', res.msg, '数据:', res.data)

      // 🌟🌟🌟 核心修正点 🌟🌟🌟
      // 不需要判断 res.data !== undefined 了，因为拦截器已经帮我们处理好了
      // 直接用 res 作为响应体
      const responseBody = res

      // 2. 防御性检查 (防止网络层真的很空)
      if (!responseBody) {
        console.warn('网络响应完全为空，跳过')
        return
      }

      // 3. 状态判断：正在处理中
      // 逻辑：只要 msg 是 processing 或者 data 是 null，就代表还在算
      if (responseBody.msg === 'processing' || responseBody.data === null) {
        console.log(`[${new Date().toLocaleTimeString()}] 任务仍在计算中...`)
        return // ✅ 直接返回，等待下一次轮询，不再报错
      }

      // 4. 状态判断：成功
      if (responseBody.code === 200 && responseBody.data) {
        clearInterval(pollInterval) // 停止轮询
        diagnosing.value = false
        loading.value = false

        const resultData = responseBody.data
        diagnosisResult.value = resultData

        // 赋值页面显示
        aiFindings.value = resultData.findings || []
        aiProcessedUrl.value = resultData.annotated_image_url || ''

        showAnnotated.value = true
        ElMessage.success('AI诊断完成！')

      } else {
        // 5. 业务错误
        throw new Error(responseBody.msg || '未知业务错误')
      }

    } catch (error) {
      console.error('轮询出错:', error)
      // 遇到严重错误才停止，404 或 processing 不停止
      if (error.response && error.response.status !== 404) {
        // 根据需要决定是否 clearInterval
      }
    }
  }, 2000)
}

const formatConfidence = (val) => {
  if (!val) return '0%'
  if (typeof val === 'number') {
    return (val * 100).toFixed(0) + '%'
  }
  if (typeof val === 'string') {
    if (val.includes('%')) {
      return parseFloat(val).toFixed(0) + '%'
    }
    return (parseFloat(val) * 100).toFixed(0) + '%'
  }
  return '0%'
}

const getConfType = (conf) => {
  let numVal = conf
  if (typeof conf === 'string') {
    numVal = parseFloat(conf.replace('%', ''))
    if (conf.includes('%')) numVal = numVal / 100
  }

  if (numVal > 0.8) return 'danger'
  if (numVal > 0.5) return 'warning'
  return 'primary'
}

const resetWorkstation = () => {
  currentImageUrl.value = ''
  aiProcessedUrl.value = ''
  aiFindings.value = []
  doctorNote.value = ''
  showAnnotated.value = false
  taskId.value = ''
  diagnosing.value = false
  diagnosisResult.value = null
  if (currentImageUrl.value) {
    URL.revokeObjectURL(currentImageUrl.value)
  }
  loadElderList()
}

const saveReport = () => {
  ElMessage.success(`报告已生成（TaskID: ${taskId.value}）并发送至打印机`)
}

onMounted(() => {
  loadElderList()
})
</script>

<style scoped>
:root {
  --primary-color: #0052D9;
  --bg-color: #f2f3f5;
  --card-bg: #ffffff;
}

.workstation-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-color);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
}

.header {
  height: 64px;
  background-color: #fff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  z-index: 10;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon-bg {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #0052D9 0%, #366EF4 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  box-shadow: 0 2px 8px rgba(0, 82, 217, 0.3);
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.main-title {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: 0.5px;
}

.sub-title {
  font-size: 12px;
  color: #8c9096;
}

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 24px 24px 24px;
  gap: 16px;
}

.elder-list-section {
  flex-shrink: 0;
  animation: slideInDown 0.4s ease-out;
}

.custom-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f2f5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.header-icon {
  color: #0052D9;
}

.elder-list-content {
  max-height: 280px;
  overflow: auto;
}

.id-tag {
  color: #909399;
  font-family: monospace;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  border-radius: 12px;
}

.main-content.is-centered {
  justify-content: center;
  align-items: center;
}

.upload-zone {
  width: 100%;
  max-width: 640px;
  animation: zoomIn 0.3s ease-out;
}

:deep(.el-upload-dragger) {
  border: 2px dashed #dcdfe6;
  border-radius: 16px;
  background-color: #fafafa;
  transition: all 0.3s;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-upload-dragger:hover) {
  border-color: #0052D9;
  background-color: #f0f5ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 82, 217, 0.1);
}

.upload-inner {
  text-align: center;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.upload-icon {
  font-size: 40px;
  color: #0052D9;
}

.upload-text h3 {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 8px;
}

.upload-text p {
  color: #909399;
  font-size: 14px;
}

.diagnosis-layout {
  width: 100%;
  height: 100%;
  display: flex;
  gap: 16px;
  background: transparent;
  overflow: hidden;
}

.image-viewer {
  flex: 2;
  background-color: #000;
  border-radius: 12px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.main-image {
  width: 100%;
  height: 100%;
}

.dicom-placeholder {
  color: #fff;
  text-align: center;
  opacity: 0.8;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.placeholder-title {
  font-size: 20px;
  font-weight: 500;
}

.placeholder-subtitle {
  font-size: 14px;
  opacity: 0.6;
  margin-top: 4px;
  font-family: monospace;
}

.image-controls {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 20px;
  border-radius: 30px;
  display: flex;
  gap: 16px;
  align-items: center;
  z-index: 10;
}

.glass-effect {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.report-panel {
  flex: 1;
  min-width: 380px;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.panel-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 700;
}

.panel-body {
  flex: 1;
  padding: 24px;
}

.report-section {
  margin-bottom: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2c3e50;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  border-left: 4px solid #0052D9;
  padding-left: 10px;
}

.finding-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.finding-card {
  background: #f8f9fb;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #ebedf0;
  transition: all 0.2s;
}

.finding-card:hover {
  background: #fff;
  border-color: #c0c4cc;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.disease-name {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.analyzing-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  color: #909399;
}

:deep(.doctor-input textarea) {
  background-color: #f9f9f9;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  transition: all 0.3s;
}

:deep(.doctor-input textarea:focus) {
  background-color: #fff;
  border-color: #0052D9;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

.panel-footer {
  padding: 20px 24px;
  border-top: 1px solid #f0f2f5;
  background-color: #fff;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.save-btn {
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 82, 217, 0.2);
}

.save-btn:not(.is-disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 82, 217, 0.3);
}

@keyframes slideInDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes zoomIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

:deep(.el-radio-button__label) {
  padding: 0 16px;
}
</style>