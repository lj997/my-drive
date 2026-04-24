<template>
  <div class="share-access-container">
    <div class="share-box">
      <div class="share-header">
        <el-icon size="40" color="#409eff"><Share /></el-icon>
        <h2>文件分享</h2>
      </div>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <div v-else-if="error" class="error-container">
        <el-icon size="48" color="#f56c6c"><Warning /></el-icon>
        <p class="error-message">{{ error }}</p>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </div>

      <template v-else>
        <div v-if="needPassword" class="password-form">
          <p class="tip">此分享需要访问密码</p>
          <el-form :model="passwordForm" label-width="0">
            <el-form-item>
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="请输入访问密码"
                show-password
                @keyup.enter="handleAccess"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleAccess" block>
                确认访问
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-else class="share-info">
          <div class="file-info">
            <el-icon :size="64" :color="shareInfo.isFolder ? '#409eff' : '#67c23a'">
              <component :is="shareInfo.isFolder ? 'Folder' : 'Document'" />
            </el-icon>
            <div class="file-detail">
              <div class="file-name">{{ shareInfo.fileName }}</div>
              <div class="file-meta">
                <span v-if="!shareInfo.isFolder">大小：{{ formatSize(shareInfo.fileSize) }}</span>
                <span>创建时间：{{ formatTime(shareInfo.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="share-actions">
            <el-button
              v-if="canPreview"
              type="primary"
              @click="handlePreview"
            >
              <el-icon><View /></el-icon>
              预览
            </el-button>
            <el-button
              v-if="!shareInfo.isFolder"
              type="primary"
              @click="handleDownload"
            >
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </div>

          <div class="share-stats">
            <span>访问次数：{{ shareInfo.viewCount }}</span>
            <span v-if="shareInfo.expireTime">
              过期时间：{{ formatTime(shareInfo.expireTime) }}
            </span>
            <span v-else>有效期：永久有效</span>
          </div>
        </div>
      </template>
    </div>

    <file-preview-dialog
      v-model:visible="previewVisible"
      :file="previewFile"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShareInfo, getShareDownloadUrl, getSharePreviewUrl } from '@/api/share'
import FilePreviewDialog from '@/components/FilePreviewDialog.vue'

const route = useRoute()
const router = useRouter()

const shareCode = computed(() => route.params.shareCode)

const loading = ref(true)
const error = ref(null)
const needPassword = ref(false)
const shareInfo = ref(null)
const submitting = ref(false)

const passwordForm = ref({
  password: ''
})

const previewVisible = ref(false)
const previewFile = ref(null)

const canPreview = computed(() => {
  if (!shareInfo.value || shareInfo.value.isFolder) return false
  const ext = shareInfo.value.fileType
  const imageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/webp']
  const textTypes = ['text/plain', 'text/html', 'text/css', 'application/javascript', 'application/json']
  return imageTypes.includes(ext) || textTypes.includes(ext)
})

const formatSize = (size) => {
  if (!size || size === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(size) / Math.log(k))
  return (size / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const loadShareInfo = async (password = null) => {
  loading.value = true
  error.value = null
  try {
    const res = await getShareInfo({
      shareCode: shareCode.value,
      password: password
    })

    if (res.data.needPassword) {
      needPassword.value = true
    } else {
      needPassword.value = false
      shareInfo.value = res.data
    }
  } catch (err) {
    error.value = err.message || '分享链接无效或已过期'
  } finally {
    loading.value = false
  }
}

const handleAccess = async () => {
  if (!passwordForm.value.password) {
    ElMessage.warning('请输入访问密码')
    return
  }
  submitting.value = true
  try {
    await loadShareInfo(passwordForm.value.password)
  } finally {
    submitting.value = false
  }
}

const handleDownload = () => {
  const url = getShareDownloadUrl(shareCode.value, passwordForm.value.password)
  const link = document.createElement('a')
  link.href = url
  link.download = shareInfo.value.fileName
  link.click()
}

const handlePreview = () => {
  previewFile.value = {
    id: null,
    fileName: shareInfo.value.fileName,
    fileType: shareInfo.value.fileType,
    extension: getExtension(shareInfo.value.fileName),
    shareCode: shareCode.value,
    sharePassword: passwordForm.value.password
  }
  previewVisible.value = true
}

const getExtension = (filename) => {
  if (!filename || !filename.includes('.')) return null
  return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  loadShareInfo()
})
</script>

<style scoped>
.share-access-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.share-box {
  width: 100%;
  max-width: 500px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.share-header {
  text-align: center;
  margin-bottom: 30px;
}

.share-header h2 {
  margin-top: 10px;
  font-size: 24px;
  color: #303133;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 16px;
}

.loading-container p,
.error-message {
  color: #606266;
  font-size: 16px;
}

.error-message {
  color: #f56c6c;
}

.password-form {
  text-align: center;
}

.password-form .tip {
  margin-bottom: 20px;
  color: #606266;
  font-size: 15px;
}

.share-info {
  text-align: center;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 24px;
}

.file-detail {
  flex: 1;
  text-align: left;
}

.file-name {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  word-break: break-all;
}

.file-meta {
  font-size: 14px;
  color: #909399;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.share-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 24px;
}

.share-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #909399;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}
</style>
