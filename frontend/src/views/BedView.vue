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

        <el-button
            type="danger"
            plain
            :disabled="selectedBedIds.length === 0"
            @click="handleBatchDelete"
            v-if="userRole === 'ADMIN'">
          <el-icon><Delete /></el-icon> 批量删除 ({{ selectedBedIds.length }})
        </el-button>

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
            <div class="bedEntity-layout">
              <div v-for="bedEntity in beds" :key="bedEntity.id"
                   :class="[
                     'bedEntity-slot',
                     bedEntity.status === 1 ? 'is-occupied' : 'is-vacant',
                     isSelected(bedEntity.id) ? 'is-selected' : ''
                   ]"
                   @click="toggleSelection(bedEntity)">

                <div v-if="isSelected(bedEntity.id)" class="selected-badge">
                  <el-icon><Check /></el-icon>
                </div>

                <el-tooltip :content="bedEntity.status === 1 ? '已占用 (不可删除)' : '点击选中/取消'">
                  <div class="bedEntity-info">
                    <el-icon :size="22">
                      <HomeFilled v-if="bedEntity.status === 1" />
                      <Shop v-else />
                    </el-icon>
                    <span class="bedEntity-label">{{ bedEntity.bedNumber.split('-')[3] }}号床</span>
                  </div>
                </el-tooltip>
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
          <el-select v-model="tempRoomNum" placeholder="请选择或输入" filterable allow-create style="width: 100%" @change="autoSuggestBedIndex">
            <el-option v-for="room in existingRooms" :key="room" :label="room + ' 房间'" :value="room" />
          </el-select>
        </el-form-item>
        <el-form-item label="床位序号" required>
          <el-input-number v-model="tempBedIndex" :min="1" :max="4" />
          <span style="margin-left: 10px; color: #999">号床 (最多4张)</span>
        </el-form-item>
        <el-form-item label="预览编号">
          <el-tag type="warning" effect="dark">{{ currentBuilding }}-{{ currentFloor }}-{{ tempRoomNum || '?' }}-{{ tempBedIndex }}</el-tag>
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
import { Search, Plus, HomeFilled, Shop, Delete, Check } from '@element-plus/icons-vue' // 引入 Check 和 Delete 图标
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBedPage, addBed, deleteBed } from '@/api/bed' // 确保引入 deleteBed

// 基础变量
const loading = ref(false)
const dialogVisible = ref(false)
const userRole = ref('')
const formTitle = ref('')
const allBeds = ref([])
const form = ref({})
// 【修改3：新增选中ID数组】
const selectedBedIds = ref([])

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
  const filtered = allBeds.value.filter(bedEntity => {
    const parts = bedEntity.bedNumber.split('-')
    return parts[0] === currentBuilding.value && parts[1] === currentFloor.value
  })
  filtered.forEach(bedEntity => {
    const parts = bedEntity.bedNumber.split('-')
    if (parts.length >= 3) {
      const r = parts[2]; if (!rooms[r]) rooms[r] = []; rooms[r].push(bedEntity)
    }
  })
  return Object.keys(rooms).sort().reduce((obj, key) => { obj[key] = rooms[key]; return obj; }, {});
})

const existingRooms = computed(() => {
  return [...new Set(Object.keys(groupedRooms.value))].sort()
})

const autoSuggestBedIndex = (val) => {
  if (groupedRooms.value[val]) {
    const currentBeds = groupedRooms.value[val];
    const maxIdx = currentBeds.reduce((max, bedEntity) => {
      const idx = parseInt(bedEntity.bedNumber.split('-')[3])
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
  // 切换楼层或刷新后，清空选中状态，防止误删
  selectedBedIds.value = []
  loading.value = false
}

const handleBuildingChange = () => { currentFloor.value = '1'; loadData(); }
const handleFloorChange = () => { loadData(); }

// === 【修改4：多选逻辑】 ===
const isSelected = (id) => {
  return selectedBedIds.value.includes(id)
}

const toggleSelection = (bedEntity) => {
  // 只有管理员能操作
  if (userRole.value !== 'ADMIN') return;

  // 占用状态建议不给选，或者选了让后端报错。这里为了体验，可以在前端简单拦截提示
  if (bedEntity.status === 1) {
    // 可选策略：允许选中，让后端报错；或者直接不让选。
    // 这里我们允许选中，以便用户尝试删除时看到后端返回的具体“占用无法删除”的错误
  }

  const id = bedEntity.id
  const index = selectedBedIds.value.indexOf(id)
  if (index > -1) {
    selectedBedIds.value.splice(index, 1) // 取消选中
  } else {
    selectedBedIds.value.push(id) // 选中
  }
}

// === 【修改5：批量删除逻辑】 ===
const handleBatchDelete = () => {
  if (selectedBedIds.value.length === 0) return

  ElMessageBox.confirm(
      `确认要删除选中的 ${selectedBedIds.value.length} 个床位吗？(如床位正在使用将无法删除)`,
      '删除警告',
      { confirmButtonText: '狠心删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      // 核心：将数组转为逗号分隔的字符串 "1,2,3"
      const idsStr = selectedBedIds.value.join(',')
      await deleteBed(idsStr)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      // 错误由拦截器统一处理，如果后端抛出“床位正在使用”，这里会自动显示
    }
  })
}

// 新增
const handleAdd = () => {
  form.value = { id: null, status: 0 }
  tempRoomNum.value = ''
  tempBedIndex.value = 1
  formTitle.value = '新增床位'
  dialogVisible.value = true
}

// 保存逻辑
const handleConfirmSave = async () => {
  if (!tempRoomNum.value) return ElMessage.warning('请输入房间号')
  if (tempBedIndex.value > 4) return ElMessage.error('单个房间最多支持4张床位')
  form.value.bedNumber = `${currentBuilding.value}-${currentFloor.value}-${tempRoomNum.value}-${tempBedIndex.value}`
  form.value.status = 0
  await addBed(form.value)
  ElMessage.success('新增成功')
  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  userRole.value = user.role
  loadData()
})
</script>

<style scoped>
.ward-container { padding: 15px; background: #f5f7fa; min-height: 100vh; }
.header-banner { background: #fff; padding: 15px 25px; border-radius: 12px; margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.floor-sidebar { background: #fff; border-radius: 12px; margin-right: 15px; padding: 20px 0; height: calc(100vh - 160px); }
.building-indicator { text-align: center; font-size: 18px; font-weight: bold; color: #409EFF; margin-bottom: 20px; }
.room-grid { display: flex; flex-wrap: wrap; gap: 15px; }
.room-box { width: 160px; background: #fff; border-radius: 10px; border: 1px solid #e4e7ed; overflow: hidden; }
.room-header { background: #f5f7fa; padding: 6px; text-align: center; font-size: 13px; font-weight: bold; border-bottom: 1px solid #e4e7ed; }
.bedEntity-layout { padding: 10px; display: flex; justify-content: center; flex-wrap: wrap; gap: 10px; }

/* === 修改后的床位样式 === */
.bedEntity-slot {
  position: relative;
  width: 55px;
  height: 65px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent; /* 预留边框位置 */
}

/* 空闲状态 */
.is-vacant { background-color: #f0f9eb; color: #67c23a; border-color: #c2e7b0; }
/* 占用状态 */
.is-occupied { background-color: #fef0f0; color: #f56c6c; border-color: #fbc4c4; opacity: 0.8; }

/* === 核心：选中态样式 === */
.is-selected {
  border: 2px solid #409EFF !important; /* 强制变蓝 */
  background-color: #ecf5ff !important;
  color: #409EFF !important;
  transform: scale(1.05); /* 微微放大 */
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 选中时的右上角对钩 */
.selected-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background-color: #409EFF;
  color: white;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  z-index: 10;
}

.bedEntity-info { display: flex; flex-direction: column; align-items: center; }
.bedEntity-label { font-size: 11px; margin-top: 4px; }
</style>