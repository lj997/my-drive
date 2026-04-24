<template>
  <div class="shares-container">
    <div class="page-header">
      <h3>我的分享</h3>
    </div>

    <div class="shares-content">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
      </div>

      <div v-else-if="shares.length === 0" class="empty-container">
        <el-empty description="暂无分享" />
      </div>

      <div v-else class="share-list">
        <div class="list-header">
          <div class="col-name">文件名称</div>
          <div class="col-type">类型</div>
          <div class="col-code">分享码</div>
          <div class="col-views">访问</div>
          <div class="col-downloads">下载</div>
          <div class="col-time">创建时间</div>
          <div class="col-expire">过期时间</div>
          <div class="col-action">操作</div>
        </div>

        <div
          v-for="share in shares"
          :key="share.id"
          class="list-item"
        >
          <div class="col-name">
            <el-icon class="file-icon" :size="24">
              <component :is="share.isFolder ? 'Folder' : 'Document'" />
            </el-icon>
            <span class="file-name">{{ share.fileName }}</span>
          </div>
          <div class="col-type">{{ share.isFolder ? '文件夹' : '文件' }}</div>
          <div class="col-code">
            <el-tag size="small">{{ share.shareCode }}</el-tag>
          </div>
          <div class="col-views">{{ share.viewCount }}</div>
          <div class="col-downloads">{{ share.downloadCount }}</div>
          <div class="col-time">{{ formatTime(share.createTime) }}</div>
          <div class="col-expire">
            <span v-if="share.expireTime" :class="isExpired(share.expireTime) ? 'expired' : ''">
              {{ formatTime(share.expireTime) }}
            </span>
            <span v-else class="permanent">永久</span>
          </div>
          <div class="col-action">
            <el-button link type="primary" size="small" @click="copyLink(share)">
              复制链接
            </el-button>
            <el-button link type="danger" size="small" @click="handleCancel(share)">
              取消分享
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listShares, cancelShare } from '@/api/share'

const loading = ref(false)
const shares = ref([])

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const isExpired = (expireTime) => {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}

const loadShares = async () => {
  loading.value = true
  try {
    const res = await listShares()
    shares.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const copyLink = (share) => {
  const link = `${window.location.origin}/s/${share.shareCode}`
  navigator.clipboard.writeText(link).then(() => {
    ElMessage.success('已复制分享链接')
  })
}

const handleCancel = async (share) => {
  try {
    await ElMessageBox.confirm(`确定要取消分享 "${share.fileName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelShare(share.id)
    ElMessage.success('已取消分享')
    loadShares()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadShares()
})
</script>

<style scoped>
.shares-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h3 {
  font-size: 20px;
  color: #303133;
}

.shares-content {
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

.share-list {
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
  align-items: center;
  transition: background 0.2s;
}

.list-item:hover {
  background: #f5f7fa;
}

.col-name {
  flex: 2;
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

.col-type {
  width: 80px;
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;
}

.col-code {
  width: 100px;
  flex-shrink: 0;
}

.col-views,
.col-downloads {
  width: 60px;
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;
}

.col-time,
.col-expire {
  width: 160px;
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;
}

.col-action {
  width: 140px;
  flex-shrink: 0;
}

.expired {
  color: #f56c6c;
}

.permanent {
  color: #67c23a;
}
</style>
