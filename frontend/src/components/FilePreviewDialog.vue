<template>
  <el-dialog
    v-model="visible"
    :title="file?.fileName"
    width="800px"
    :close-on-click-modal="false"
  >
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="isImage" class="image-preview">
      <img :src="previewUrl" :alt="file?.fileName" @load="onImageLoad" />
    </div>

    <div v-else-if="isText" class="text-preview">
      <iframe
        ref="iframeRef"
        :src="previewUrl"
        @load="onIframeLoad"
        style="width: 100%; height: 500px; border: none;"
      ></iframe>
    </div>

    <div v-else class="no-preview">
      <el-icon size="64" color="#909399"><Document /></el-icon>
      <p>此文件类型暂不支持预览</p>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="handleDownload" v-if="file">
        <el-icon><Download /></el-icon>
        下载
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getPreviewUrl, getDownloadUrl } from '@/api/file'

const props = defineProps({
  file: {
    type: Object,
    default: null
  }
})

const visible = defineModel('visible', {
  type: Boolean,
  default: false
})

const loading = ref(false)
const iframeRef = ref(null)

const previewUrl = computed(() => {
  if (props.file) {
    return getPreviewUrl(props.file.id)
  }
  return ''
})

const isImage = computed(() => {
  if (!props.file) return false
  const ext = props.file.extension?.toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)
})

const isText = computed(() => {
  if (!props.file) return false
  const ext = props.file.extension?.toLowerCase()
  return ['txt', 'html', 'htm', 'css', 'js', 'json'].includes(ext)
})

const onImageLoad = () => {
  loading.value = false
}

const onIframeLoad = () => {
  loading.value = false
}

const handleDownload = () => {
  if (!props.file) return
  const url = getDownloadUrl(props.file.id)
  const link = document.createElement('a')
  link.href = url
  link.download = props.file.fileName
  link.click()
}

watch(visible, (val) => {
  if (val && props.file) {
    loading.value = true
    setTimeout(() => {
      if (loading.value) {
        loading.value = false
      }
    }, 3000)
  }
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.loading-container p {
  color: #909399;
}

.image-preview {
  text-align: center;
  max-height: 500px;
  overflow: auto;
}

.image-preview img {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
}

.text-preview {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.no-preview p {
  color: #909399;
  font-size: 16px;
}
</style>
