<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <el-icon size="32"><FolderOpened /></el-icon>
        <span>我的网盘</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="layout-menu"
        router
        background-color="transparent"
        text-color="#333"
        active-text-color="#409eff"
      >
        <el-menu-item index="/files">
          <el-icon><Folder /></el-icon>
          <span>我的文件</span>
        </el-menu-item>
        <el-menu-item index="/shares">
          <el-icon><Share /></el-icon>
          <span>我的分享</span>
        </el-menu-item>
        <el-menu-item index="/recycle">
          <el-icon><Delete /></el-icon>
          <span>回收站</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="storage-info">
            已用 {{ formatStorage(userStore.userInfo?.storageUsed) }} /
            {{ formatStorage(userStore.userInfo?.storageLimit) }}
          </span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>

    <file-preview-dialog
      v-model:visible="previewVisible"
      :file="previewFile"
    />
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import FilePreviewDialog from '@/components/FilePreviewDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const previewVisible = ref(false)
const previewFile = ref(null)

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})

const formatStorage = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

const openPreview = (file) => {
  previewFile.value = file
  previewVisible.value = true
}

provide('openPreview', openPreview)
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  border-bottom: 1px solid #e4e7ed;
}

.layout-menu {
  border-right: none;
  margin-top: 20px;
}

.layout-menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 12px;
  border-radius: 8px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background: #ecf5ff;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.storage-info {
  font-size: 14px;
  color: #666;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #333;
}

.layout-main {
  background: #f5f7fa;
  padding: 24px;
  overflow: auto;
}
</style>
