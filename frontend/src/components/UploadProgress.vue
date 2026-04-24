<template>
  <el-dialog
    v-model="visible"
    title="文件上传"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="upload-area" @click="triggerUpload">
      <el-icon size="48" color="#409eff"><Upload /></el-icon>
      <p class="upload-text">点击选择文件或拖拽文件到此处</p>
      <p class="upload-tip">支持多文件上传</p>
    </div>
    <input
      ref="fileInputRef"
      type="file"
      multiple
      style="display: none"
      @change="handleFileSelect"
    />

    <div v-if="uploadList.length > 0" class="upload-list">
      <div
        v-for="item in uploadList"
        :key="item.uid"
        class="upload-item"
      >
        <div class="item-info">
          <el-icon><Document /></el-icon>
          <span class="item-name">{{ item.name }}</span>
          <span class="item-status" :class="item.status">
            {{ getStatusText(item) }}
          </span>
        </div>
        <el-progress
          v-if="item.status === 'uploading'"
          :percentage="item.percentage"
          :stroke-width="8"
          :text-inside="true"
        />
        <span v-if="item.status === 'success'" class="item-size">
          {{ formatSize(item.size) }}
        </span>
        <span v-if="item.status === 'error'" class="item-error">
          {{ item.error || '上传失败' }}
        </span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <span v-if="uploadList.length > 0" class="upload-stats">
          已选择 {{ uploadList.length }} 个文件
        </span>
        <span>
          <el-button @click="clearAll" v-if="uploadList.length > 0">清空</el-button>
          <el-button @click="visible = false">关闭</el-button>
          <el-button
            type="primary"
            :loading="anyUploading"
            :disabled="uploadList.length === 0 || anyUploading"
            @click="startUpload"
          >
            开始上传
          </el-button>
        </span>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

const props = defineProps({
  parentId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['success'])

const visible = ref(false)
const fileInputRef = ref(null)
const uploadList = ref([])
let uidCounter = 0

const anyUploading = computed(() => {
  return uploadList.value.some(item => item.status === 'uploading')
})

const open = () => {
  visible.value = true
}

const triggerUpload = () => {
  fileInputRef.value.click()
}

const handleFileSelect = (e) => {
  const files = Array.from(e.target.files)
  addFiles(files)
  fileInputRef.value.value = ''
}

const addFiles = (files) => {
  for (const file of files) {
    const exist = uploadList.value.find(item => item.name === file.name && item.size === file.size)
    if (!exist) {
      uploadList.value.push({
        uid: uidCounter++,
        file,
        name: file.name,
        size: file.size,
        status: 'pending',
        percentage: 0,
        error: null
      })
    }
  }
}

const getStatusText = (item) => {
  switch (item.status) {
    case 'pending': return '等待上传'
    case 'uploading': return '上传中...'
    case 'success': return '上传成功'
    case 'error': return '上传失败'
    default: return ''
  }
}

const formatSize = (size) => {
  if (!size) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(size) / Math.log(k))
  return (size / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const startUpload = async () => {
  const pendingFiles = uploadList.value.filter(item => item.status === 'pending')
  if (pendingFiles.length === 0) return

  for (const item of pendingFiles) {
    await uploadSingleFile(item)
  }

  const successCount = uploadList.value.filter(item => item.status === 'success').length
  const errorCount = uploadList.value.filter(item => item.status === 'error').length

  if (successCount > 0) {
    emit('success')
    ElMessage.success(`成功上传 ${successCount} 个文件`)
  }
  if (errorCount > 0) {
    ElMessage.warning(`${errorCount} 个文件上传失败`)
  }
}

const uploadSingleFile = async (item) => {
  item.status = 'uploading'
  item.percentage = 0

  try {
    await uploadFile(
      item.file,
      props.parentId,
      (progressEvent) => {
        item.percentage = Math.round((progressEvent.loaded * 100) / progressEvent.total)
      }
    )
    item.status = 'success'
    item.percentage = 100
  } catch (error) {
    item.status = 'error'
    item.error = error.message || '上传失败'
  }
}

const clearAll = () => {
  uploadList.value = uploadList.value.filter(item => item.status === 'uploading')
}

defineExpose({ open })
</script>

<style scoped>
.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-area:hover {
  border-color: #409eff;
}

.upload-text {
  margin: 16px 0 8px;
  font-size: 16px;
  color: #606266;
}

.upload-tip {
  font-size: 14px;
  color: #909399;
}

.upload-list {
  margin-top: 20px;
  max-height: 300px;
  overflow-y: auto;
}

.upload-item {
  padding: 12px;
  border-radius: 4px;
  background: #f5f7fa;
  margin-bottom: 8px;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-status {
  font-size: 12px;
}

.item-status.uploading {
  color: #409eff;
}

.item-status.success {
  color: #67c23a;
}

.item-status.error {
  color: #f56c6c;
}

.item-size {
  font-size: 12px;
  color: #909399;
}

.item-error {
  font-size: 12px;
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-stats {
  font-size: 14px;
  color: #606266;
}
</style>
