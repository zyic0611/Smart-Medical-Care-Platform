<template>
  <div class="ward-container">
    <div class="header-banner">
      <div class="title-area">
        <el-radio-group v-model="currentBuilding" @change="handleBuildingChange" size="large">
          <el-radio-button label="A">A 栋</el-radio-button>
          <el-radio-button label="B">B 栋</el-radio-button>
          <el-radio-button label="C">C 栋</el-radio-button>
          <el-radio-button label="D">D 栋</el-radio-button>
          <el-radio-button label="E">E 栋</el-radio-button>
        </el-radio-group>
      </div>

      <div class="action-buttons">
        <el-input v-model="queryParams.bedNumber" placeholder="搜索: A-1-101-1" style="width: 200px; margin-right: 12px" clearable @clear="loadData" />
        <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon> 搜索</el-button>
        <!-- 保留新增按钮（如需移除可直接删除这行） -->
        <el-button type="success" @click="handleAdd" v-if="userRole === 'ADMIN'"><el-icon><Plus /></el-icon> 智能新增</el-button>
      </div>
    </div>

    <el-container class="main-layout">
      <el-aside width="110px" class="floor-sidebar">
        <div class="building-indicator">{{ currentBuilding }} 栋</div>
        <el-tabs v-model="currentFloor" tab-position="left" @tab-change="handleFloorChange">
          <el-tab-pane v-for="f in 5" :key="f" :label="f + ' 楼'" :name="String(f)"></el-tab-pane>
        </el-tabs>
      </el-aside>

      <el-main class="room-display-area" v-loading="loading">
        <div class="room-grid" v-if="Object.keys(groupedRooms).length > 0">
          <div v-for="(beds, roomNum) in groupedRooms" :key="roomNum" class="room-box">
            <div class="room-header">{{ roomNum }} 房间</div>
            <div class="bed-layout">
              <div v-for="bed in beds" :key="bed.id"
                   :class="['bed-slot', bed.status === 1 ? 'is-occupied' : 'is-vacant']">

                <el-tooltip :content="bed.status === 1 ? '已占用' : '空闲'">
                  <div class="bed-info">
                    <el-icon :size="22">
                      <HomeFilled v-if="bed.status === 1" />
                      <Shop v-else />
                    </el-icon>
                    <span class="bed-label">{{ bed.bedNumber.split('-')[3] }}号床</span>
                  </div>
                </el-tooltip>

                <!-- 【修改1：完全移除编辑/删除按钮区域】 -->
                <!-- 原bed-actions代码已删除，不再显示编辑/删除按钮 -->
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="该楼层暂无床位数据" />
      </el-main>
    </el-container>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="450px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="当前位置">
          <el-tag size="large" effect="plain">{{ currentBuilding }} 栋 - {{ currentFloor }} 楼</el-tag>
        </el-form-item>

        <el-form-item label="房间号" required>
          <el-select
              v-model="tempRoomNum"
              placeholder="请选择或输入"
              filterable
              allow-create
              style="width: 100%"
              @change="autoSuggestBedIndex"
          >
            <el-option
                v-for="room in existingRooms"
                :key="room"
                :label="room + ' 房间'"
                :value="room"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="床位序号" required>
          <el-input-number v-model="tempBedIndex" :min="1" :max="4" />
          <span style="margin-left: 10px; color: #999">号床 (最多4张)</span>
        </el-form-item>

        <el-form-item label="预览编号">
          <el-tag type="warning" effect="dark">
            {{ currentBuilding }}-{{ currentFloor }}-{{ tempRoomNum || '?' }}-{{ tempBedIndex }}
          </el-tag>
        </el-form-item>

        <!-- 【修改2：移除编辑相关的状态选择，只保留新增的预设状态】 -->
        <el-form-item label="预设状态">
          <el-tag type="success">🟢 空闲 (新床位默认可用)</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleConfirmSave">确 定 保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Search, Plus, HomeFilled, Shop } from '@element-plus/icons-vue' // 【修改3：移除Edit、Delete图标导入】
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBedPage, addBed, updateBed, deleteBed } from '@/api/bed'

// 基础变量
const loading = ref(false)
const dialogVisible = ref(false)
const userRole = ref('')
const formTitle = ref('')
const allBeds = ref([])
const form = ref({})

// 筛选状态
const currentBuilding = ref('A')
const currentFloor = ref('1')
const queryParams = reactive({ pageNum: 1, pageSize: 1000, bedNumber: '' })

// 智能拼接辅助变量
const tempRoomNum = ref('')
const tempBedIndex = ref(1)

// === 计算属性：按房间分组渲染 ===
const groupedRooms = computed(() => {
  const rooms = {}
  const filtered = allBeds.value.filter(bed => {
    const parts = bed.bedNumber.split('-')
    return parts[0] === currentBuilding.value && parts[1] === currentFloor.value
  })
  filtered.forEach(bed => {
    const parts = bed.bedNumber.split('-')
    if (parts.length >= 3) {
      const r = parts[2]; if (!rooms[r]) rooms[r] = []; rooms[r].push(bed)
    }
  })
  return Object.keys(rooms).sort().reduce((obj, key) => { obj[key] = rooms[key]; return obj; }, {});
})

// === 计算属性：当前层已有房间号列表 ===
const existingRooms = computed(() => {
  return [...new Set(Object.keys(groupedRooms.value))].sort()
})

// === 智能建议与校验：超过4张床给出提示 ===
const autoSuggestBedIndex = (val) => {
  if (groupedRooms.value[val]) {
    const currentBeds = groupedRooms.value[val];
    if (currentBeds.length >= 4) {
      ElMessage.warning('该房间床位已满(4张)，请确认是否继续添加')
    }
    const maxIdx = currentBeds.reduce((max, bed) => {
      const idx = parseInt(bed.bedNumber.split('-')[3])
      return idx > max ? idx : max
    }, 0)
    tempBedIndex.value = maxIdx >= 4 ? 4 : maxIdx + 1
  } else {
    tempBedIndex.value = 1
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  const res = await getBedPage(queryParams)
  allBeds.value = res.records || []
  loading.value = false
}

const handleBuildingChange = () => { currentFloor.value = '1'; loadData(); }
const handleFloorChange = () => { loadData(); }

// 新增
const handleAdd = () => {
  form.value = { id: null, status: 0 } // 显式设置 id 为空，状态为 0
  tempRoomNum.value = ''
  tempBedIndex.value = 1
  formTitle.value = '新增床位'
  dialogVisible.value = true
}

// 保存逻辑
const handleConfirmSave = async () => {
  if (!tempRoomNum.value) return ElMessage.warning('请输入房间号')
  if (tempBedIndex.value > 4) return ElMessage.error('单个房间最多支持4张床位')

  // 拼接
  form.value.bedNumber = `${currentBuilding.value}-${currentFloor.value}-${tempRoomNum.value}-${tempBedIndex.value}`

  // 【修改4：移除编辑分支，只保留新增逻辑】
  form.value.status = 0 // 强制新增状态为空闲
  await addBed(form.value)
  ElMessage.success('新增成功')

  dialogVisible.value = false
  loadData()
}

// 【修改5：完全移除handleEdit和handleDelete方法】

onMounted(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  userRole.value = user.role
  loadData()
})
</script>

<style scoped>
/* 保持原有样式，移除bed-actions相关样式（因为界面已无该元素） */
.ward-container { padding: 15px; background: #f5f7fa; min-height: 100vh; }
.header-banner { background: #fff; padding: 15px 25px; border-radius: 12px; margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.main-title { font-size: 18px; font-weight: bold; }
.floor-sidebar { background: #fff; border-radius: 12px; margin-right: 15px; padding: 20px 0; height: calc(100vh - 160px); }
.building-indicator { text-align: center; font-size: 18px; font-weight: bold; color: #409EFF; margin-bottom: 20px; }
.room-grid { display: flex; flex-wrap: wrap; gap: 15px; }
.room-box { width: 160px; background: #fff; border-radius: 10px; border: 1px solid #e4e7ed; overflow: hidden; }
.room-header { background: #f5f7fa; padding: 6px; text-align: center; font-size: 13px; font-weight: bold; border-bottom: 1px solid #e4e7ed; }
.bed-layout { padding: 10px; display: flex; justify-content: center; flex-wrap: wrap; gap: 10px; }
.bed-slot { position: relative; width: 55px; height: 65px; border-radius: 6px; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.is-vacant { background-color: #f0f9eb; color: #67c23a; border: 1px solid #c2e7b0; }
.is-occupied { background-color: #fef0f0; color: #f56c6c; border: 1px solid #fbc4c4; }
.bed-info { display: flex; flex-direction: column; align-items: center; }
.bed-label { font-size: 11px; margin-top: 4px; }
/* 【修改6：移除bed-actions相关样式】 */
</style>