<template>
  <div class="recycle-container">
    <div class="page-header">
      <h3>回收站</h3>
      <div class="toolbar">
        <el-button
          type="primary"
          :disabled="selectedFiles.length === 0"
          @click="handleRestore"
        >
          <el-icon><Refresh /></el-icon>
          恢复
        </el-button>
        <el-button
          type="danger"
          :disabled="selectedFiles.length === 0"
          @click="handlePermanentDelete"
        >
          <el-icon><Delete /></el-icon>
          彻底删除
        </el-button>
      </div>
    </div>

    <div class="recycle-content">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
      </div>

      <div v-else-if="files.length === 0" class="empty-container">
        <el-empty description="回收站为空" />
      </div>

      <div v-else class="file-list">
        <div class="list-header">
          <div class="col-checkbox">
            <el-checkbox
              v-model="isAllSelected"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
            />
          </div>
          <div class="col-name">名称</div>
          <div class="col-size">大小</div>
          <div class="col-time">删除时间</div>
          <div class="col-action">操作</div>
        </div>

        <div
          v-for="file in files"
          :key="file.id"
          class="list-item"
          :class="{ selected: selectedFiles.includes(file.id) }"
          @click="handleFileClick(file, $event)"
        >
          <div class="col-checkbox" @click.stop>
            <el-checkbox
              :model-value="selectedFiles.includes(file.id)"
              @change="(val) => handleFileSelectChange(file, val)"
            />
          </div>
          <div class="col-name">
            <el-icon class="file-icon" :size="24">
              <component :is="getFileIcon(file)" />
            </el-icon>
            <span class="file-name">{{ file.fileName }}</span>
          </div>
          <div class="col-size">{{ formatSize(file.fileSize) }}</div>
          <div class="col-time">{{ formatTime(file.deleteTime) }}</div>
          <div class="col-action">
            <el-button link type="primary" size="small" @click="handleRestoreFile(file)">
              恢复
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteFile(file)">
              彻底删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRecycleBin,
  restoreFromRecycleBin,
  permanentDelete as apiPermanentDelete
} from '@/api/file'

const loading = ref(false)
const files = ref([])
const selectedFiles = ref([])

const isAllSelected = computed(() => {
  return files.value.length > 0 && selectedFiles.value.length === files.value.length
})

const isIndeterminate = computed(() => {
  return selectedFiles.value.length > 0 && selectedFiles.value.length < files.value.length
})

const getFileIcon = (file) => {
  if (file.isFolder) return 'Folder'
  const ext = file.extension?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return 'Picture'
  if (['txt', 'html', 'css', 'js', 'json'].includes(ext)) return 'Document'
  return 'Document'
}

const formatSize = (size) => {
  if (!size || size === 0) return '-'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(size) / Math.log(k))
  return (size / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadFiles = async () => {
  loading.value = true
  try {
    const res = await listRecycleBin()
    files.value = res.data
    selectedFiles.value = []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleFileClick = (file, event) => {
  if (event.ctrlKey || event.metaKey) {
    const index = selectedFiles.value.indexOf(file.id)
    if (index > -1) {
      selectedFiles.value.splice(index, 1)
    } else {
      selectedFiles.value.push(file.id)
    }
  } else {
    selectedFiles.value = [file.id]
  }
}

const handleFileSelectChange = (file, checked) => {
  if (checked) {
    if (!selectedFiles.value.includes(file.id)) {
      selectedFiles.value.push(file.id)
    }
  } else {
    const index = selectedFiles.value.indexOf(file.id)
    if (index > -1) {
      selectedFiles.value.splice(index, 1)
    }
  }
}

const handleSelectAll = (checked) => {
  if (checked) {
    selectedFiles.value = files.value.map(f => f.id)
  } else {
    selectedFiles.value = []
  }
}

const handleRestore = async () => {
  if (selectedFiles.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要恢复选中的 ${selectedFiles.value.length} 个项目吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await restoreFromRecycleBin(selectedFiles.value)
    ElMessage.success('恢复成功')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleRestoreFile = async (file) => {
  try {
    await ElMessageBox.confirm(`确定要恢复 "${file.fileName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await restoreFromRecycleBin([file.id])
    ElMessage.success('恢复成功')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handlePermanentDelete = async () => {
  if (selectedFiles.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除选中的 ${selectedFiles.value.length} 个项目吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await apiPermanentDelete(selectedFiles.value)
    ElMessage.success('已彻底删除')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleDeleteFile = async (file) => {
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除 "${file.fileName}" 吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await apiPermanentDelete([file.id])
    ElMessage.success('已彻底删除')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.recycle-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h3 {
  font-size: 20px;
  color: #303133;
}

.toolbar {
  display: flex;
  gap: 12px;
}

.recycle-content {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  overflow: auto;
}

.loading-container,
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.file-list {
  width: 100%;
}

.list-header {
  display: flex;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.list-item {
  display: flex;
  padding: 12px 16px;
  border-radius: 4px;
  cursor: pointer;
  align-items: center;
  transition: background 0.2s;
}

.list-item:hover {
  background: #f5f7fa;
}

.list-item.selected {
  background: #ecf5ff;
}

.col-checkbox {
  width: 40px;
  flex-shrink: 0;
}

.col-name {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-icon {
  flex-shrink: 0;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-size {
  width: 120px;
  flex-shrink: 0;
  color: #909399;
  font-size: 14px;
}

.col-time {
  width: 180px;
  flex-shrink: 0;
  color: #909399;
  font-size: 14px;
}

.col-action {
  width: 140px;
  flex-shrink: 0;
}
</style>
