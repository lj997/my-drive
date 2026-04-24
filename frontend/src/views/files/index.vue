<template>
  <div class="files-container">
    <div class="files-header">
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item
            v-for="item in breadcrumb"
            :key="item.id"
          >
            <span
              class="breadcrumb-item"
              :class="{ active: item.id === currentParentId }"
              @click="navigateTo(item.id)"
            >
              {{ item.fileName }}
            </span>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="toolbar">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :multiple="true"
          :limit="100"
          :on-change="handleFileSelect"
          :before-upload="() => false"
          drag
          class="upload-drag"
          style="display: none"
        >
          <div class="el-upload-dragger">
            <i class="el-icon-upload"></i>
            <div class="el-upload-text">将文件拖到此处，或点击上传</div>
          </div>
        </el-upload>
        <el-button type="primary" @click="handleUploadClick">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
        <el-button @click="showCreateFolder = true">
          <el-icon><FolderAdd /></el-icon>
          新建文件夹
        </el-button>
        <el-button
          type="danger"
          :disabled="selectedFiles.length === 0"
          @click="handleDelete"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="list">列表</el-radio-button>
          <el-radio-button value="grid">网格</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="files-content">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
      </div>
      
      <div v-else-if="files.length === 0" class="empty-container">
        <el-empty description="文件夹为空" />
      </div>
      
      <template v-else>
        <div v-if="viewMode === 'list'" class="file-list">
          <div class="file-list-header">
            <div class="col-checkbox">
              <el-checkbox
                v-model="isAllSelected"
                :indeterminate="isIndeterminate"
                @change="handleSelectAll"
              />
            </div>
            <div class="col-name">名称</div>
            <div class="col-size">大小</div>
            <div class="col-time">修改时间</div>
            <div class="col-action">操作</div>
          </div>
          
          <div
            v-for="file in files"
            :key="file.id"
            class="file-item"
            :class="{ selected: selectedFiles.includes(file.id) }"
            @click="handleFileClick(file, $event)"
            @dblclick="handleFileDblClick(file)"
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
            <div class="col-time">{{ formatTime(file.updateTime) }}</div>
            <div class="col-action" @click.stop>
              <el-dropdown trigger="click">
                <el-button link type="primary" size="small">
                  更多 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handlePreview(file)" v-if="canPreview(file)">
                      <el-icon><View /></el-icon> 预览
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleDownload(file)" v-if="!file.isFolder">
                      <el-icon><Download /></el-icon> 下载
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleRename(file)">
                      <el-icon><Edit /></el-icon> 重命名
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleMove(file)">
                      <el-icon><Promotion /></el-icon> 移动
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleShare(file)">
                      <el-icon><Share /></el-icon> 分享
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDeleteFile(file)">
                      <el-icon><Delete /></el-icon> 删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
        
        <div v-else class="file-grid">
          <div
            v-for="file in files"
            :key="file.id"
            class="grid-item"
            :class="{ selected: selectedFiles.includes(file.id) }"
            @click="handleFileClick(file, $event)"
            @dblclick="handleFileDblClick(file)"
          >
            <div class="grid-checkbox" @click.stop>
              <el-checkbox
                :model-value="selectedFiles.includes(file.id)"
                @change="(val) => handleFileSelectChange(file, val)"
              />
            </div>
            <div class="grid-icon">
              <el-icon :size="48">
                <component :is="getFileIcon(file)" />
              </el-icon>
            </div>
            <div class="grid-name" :title="file.fileName">
              {{ file.fileName }}
            </div>
          </div>
        </div>
      </template>
    </div>

    <el-dialog
      v-model="showCreateFolder"
      title="新建文件夹"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="文件夹名">
          <el-input v-model="folderName" placeholder="请输入文件夹名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateFolder = false">取消</el-button>
        <el-button type="primary" @click="createFolder" :loading="creating">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showRename"
      title="重命名"
      width="400px"
    >
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="newName" placeholder="请输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRename = false">取消</el-button>
        <el-button type="primary" @click="renameFile" :loading="renaming">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showShare"
      title="创建分享链接"
      width="450px"
    >
      <el-form label-width="100px">
        <el-form-item label="文件名称">
          <el-input :value="shareFile?.fileName" disabled />
        </el-form-item>
        <el-form-item label="访问密码">
          <el-input
            v-model="shareForm.password"
            placeholder="留空则无密码"
            show-password
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="shareForm.expireDays" style="width: 100%">
            <el-option label="永久有效" :value="null" />
            <el-option label="1天" :value="1" />
            <el-option label="7天" :value="7" />
            <el-option label="30天" :value="30" />
          </el-select>
        </el-form-item>
      </el-form>
      <template v-if="shareResult" #footer>
        <div class="share-result">
          <div class="share-link">
            <span>分享链接：</span>
            <el-input :value="shareResult.shareLink" style="width: 300px" />
            <el-button type="primary" @click="copyLink">复制</el-button>
          </div>
          <div v-if="shareResult.hasPassword" class="share-tip">
            提示：此分享需要访问密码
          </div>
        </div>
        <el-button @click="showShare = false; shareResult = null">关闭</el-button>
      </template>
      <template v-else #footer>
        <el-button @click="showShare = false">取消</el-button>
        <el-button type="primary" @click="createShare" :loading="sharing">创建分享</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showMove"
      title="选择目标文件夹"
      width="500px"
    >
      <div class="folder-tree">
        <div
          class="tree-item"
          :class="{ selected: targetFolderId === 0 }"
          @click="targetFolderId = 0"
        >
          <el-icon><FolderOpened /></el-icon>
          <span>根目录</span>
        </div>
        <div
          v-for="folder in folderList"
          :key="folder.id"
          class="tree-item"
          :class="{ selected: targetFolderId === folder.id, disabled: movingFileIds?.includes(folder.id) }"
          @click="!movingFileIds?.includes(folder.id) && (targetFolderId = folder.id)"
        >
          <el-icon><Folder /></el-icon>
          <span>{{ folder.fileName }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showMove = false">取消</el-button>
        <el-button type="primary" @click="moveFiles" :loading="moving">确定</el-button>
      </template>
    </el-dialog>

    <upload-progress
      ref="uploadProgressRef"
      :parent-id="currentParentId"
      @success="loadFiles"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listFiles,
  getBreadcrumb,
  createFolder as apiCreateFolder,
  renameFile as apiRenameFile,
  moveFiles as apiMoveFiles,
  moveToRecycleBin,
  getDownloadUrl,
} from '@/api/file'
import { createShare as apiCreateShare } from '@/api/share'
import UploadProgress from '@/components/UploadProgress.vue'

const openPreview = inject('openPreview')

const loading = ref(false)
const files = ref([])
const breadcrumb = ref([])
const currentParentId = ref(0)
const viewMode = ref('list')
const selectedFiles = ref([])
const folderList = ref([])

const showCreateFolder = ref(false)
const folderName = ref('')
const creating = ref(false)

const showRename = ref(false)
const renameFileItem = ref(null)
const newName = ref('')
const renaming = ref(false)

const showShare = ref(false)
const shareFile = ref(null)
const shareForm = ref({
  password: '',
  expireDays: null
})
const sharing = ref(false)
const shareResult = ref(null)

const showMove = ref(false)
const targetFolderId = ref(0)
const movingFileIds = ref([])
const moving = ref(false)

const uploadProgressRef = ref(null)

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
  if (['zip', 'rar'].includes(ext)) return 'Box'
  if (['mp3'].includes(ext)) return 'Headset'
  if (['mp4'].includes(ext)) return 'VideoCamera'
  return 'Document'
}

const canPreview = (file) => {
  if (file.isFolder) return false
  const ext = file.extension?.toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'txt', 'html', 'css', 'js', 'json'].includes(ext)
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
    const [filesRes, breadcrumbRes] = await Promise.all([
      listFiles(currentParentId.value),
      getBreadcrumb(currentParentId.value)
    ])
    files.value = filesRes.data
    breadcrumb.value = breadcrumbRes.data
    folderList.value = files.value.filter(f => f.isFolder)
    selectedFiles.value = []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const navigateTo = (folderId) => {
  currentParentId.value = folderId
  loadFiles()
}

const handleFileDblClick = (file) => {
  if (file.isFolder) {
    navigateTo(file.id)
  } else if (canPreview(file)) {
    handlePreview(file)
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

const handleUploadClick = () => {
  uploadProgressRef.value.open()
}

const handleFileSelect = (file) => {
  console.log('File selected:', file.name)
}

const createFolder = async () => {
  if (!folderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  creating.value = true
  try {
    await apiCreateFolder({
      parentId: currentParentId.value,
      folderName: folderName.value.trim()
    })
    ElMessage.success('创建成功')
    showCreateFolder.value = false
    folderName.value = ''
    loadFiles()
  } catch (error) {
    console.error(error)
  } finally {
    creating.value = false
  }
}

const handlePreview = (file) => {
  openPreview(file)
}

const handleDownload = (file) => {
  const url = getDownloadUrl(file.id)
  const link = document.createElement('a')
  link.href = url
  link.download = file.fileName
  link.click()
}

const handleRename = (file) => {
  renameFileItem.value = file
  newName.value = file.fileName
  showRename.value = true
}

const renameFile = async () => {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }
  renaming.value = true
  try {
    await apiRenameFile({
      fileId: renameFileItem.value.id,
      newName: newName.value.trim()
    })
    ElMessage.success('重命名成功')
    showRename.value = false
    loadFiles()
  } catch (error) {
    console.error(error)
  } finally {
    renaming.value = false
  }
}

const handleMove = (file) => {
  movingFileIds.value = [file.id]
  targetFolderId.value = 0
  showMove.value = true
}

const moveFiles = async () => {
  moving.value = true
  try {
    await apiMoveFiles({
      fileIds: movingFileIds.value,
      targetParentId: targetFolderId.value
    })
    ElMessage.success('移动成功')
    showMove.value = false
    loadFiles()
  } catch (error) {
    console.error(error)
  } finally {
    moving.value = false
  }
}

const handleShare = (file) => {
  shareFile.value = file
  shareForm.value = { password: '', expireDays: null }
  shareResult.value = null
  showShare.value = true
}

const createShare = async () => {
  sharing.value = true
  try {
    const res = await apiCreateShare({
      fileId: shareFile.value.id,
      password: shareForm.value.password || null,
      expireDays: shareForm.value.expireDays
    })
    shareResult.value = res.data
    shareResult.value.shareLink = `${window.location.origin}/s/${res.data.shareCode}`
    ElMessage.success('分享链接已创建')
  } catch (error) {
    console.error(error)
  } finally {
    sharing.value = false
  }
}

const copyLink = () => {
  navigator.clipboard.writeText(shareResult.value.shareLink).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

const handleDeleteFile = async (file) => {
  try {
    await ElMessageBox.confirm(`确定要删除 "${file.fileName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await moveToRecycleBin([file.id])
    ElMessage.success('已移到回收站')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel' && error?.message !== 'cancel') {
      console.error(error)
    }
  }
}

const handleDelete = async () => {
  if (selectedFiles.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedFiles.value.length} 个项目吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await moveToRecycleBin(selectedFiles.value)
    ElMessage.success('已移到回收站')
    selectedFiles.value = []
    loadFiles()
  } catch (error) {
    if (error !== 'cancel' && error?.message !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.files-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.files-header {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.breadcrumb {
  margin-bottom: 16px;
}

.breadcrumb-item {
  cursor: pointer;
  color: #409eff;
}

.breadcrumb-item.active {
  color: #606266;
  cursor: default;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.files-content {
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

.file-list-header {
  display: flex;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.file-item {
  display: flex;
  padding: 12px 16px;
  border-radius: 4px;
  cursor: pointer;
  align-items: center;
  transition: background 0.2s;
}

.file-item:hover {
  background: #f5f7fa;
}

.file-item.selected {
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
  width: 80px;
  flex-shrink: 0;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.grid-item {
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: background 0.2s;
  position: relative;
}

.grid-item:hover {
  background: #f5f7fa;
}

.grid-item.selected {
  background: #ecf5ff;
}

.grid-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
}

.grid-icon {
  margin-bottom: 8px;
  color: #409eff;
}

.grid-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

.folder-tree {
  max-height: 400px;
  overflow-y: auto;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.tree-item:hover {
  background: #f5f7fa;
}

.tree-item.selected {
  background: #ecf5ff;
  color: #409eff;
}

.tree-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.share-result {
  margin-bottom: 16px;
}

.share-link {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.share-tip {
  color: #e6a23c;
  font-size: 14px;
}
</style>
