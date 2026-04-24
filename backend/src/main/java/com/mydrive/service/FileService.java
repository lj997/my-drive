package com.mydrive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mydrive.dto.CreateFolderDTO;
import com.mydrive.dto.MoveDTO;
import com.mydrive.dto.RenameDTO;
import com.mydrive.entity.File;
import com.mydrive.entity.User;
import com.mydrive.mapper.FileMapper;
import com.mydrive.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final UserMapper userMapper;

    @Value("${file.storage.path}")
    private String storagePath;

    public List<Map<String, Object>> listFiles(Long userId, Long parentId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId != null ? parentId : 0L)
                .eq(File::getDeleted, 0)
                .orderByDesc(File::getIsFolder)
                .orderByDesc(File::getCreateTime);

        List<File> files = fileMapper.selectList(wrapper);
        return files.stream().map(this::buildFileInfo).toList();
    }

    public List<Map<String, Object>> listRecycleBin(Long userId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getDeleted, 1)
                .orderByDesc(File::getDeleteTime);

        List<File> files = fileMapper.selectList(wrapper);
        return files.stream().map(this::buildFileInfo).toList();
    }

    @Transactional
    public Map<String, Object> createFolder(Long userId, CreateFolderDTO dto) {
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;

        checkNameExists(userId, parentId, dto.getFolderName(), null);

        File folder = new File();
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setFileName(dto.getFolderName());
        folder.setIsFolder(1);
        folder.setFileType("folder");
        folder.setFileSize(0L);

        fileMapper.insert(folder);
        return buildFileInfo(folder);
    }

    @Transactional
    public Map<String, Object> uploadFile(Long userId, Long parentId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        Long pId = parentId != null ? parentId : 0L;
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileType = getFileType(extension);
        long fileSize = file.getSize();

        checkNameExists(userId, pId, originalFilename, null);

        User user = userMapper.selectById(userId);
        if (user.getStorageUsed() + fileSize > user.getStorageLimit()) {
            throw new IllegalArgumentException("存储空间不足");
        }

        String relativePath = userId + "/" + UUID.randomUUID() + (extension != null ? "." + extension : "");
        Path fullPath = Paths.get(storagePath, relativePath);
        Files.createDirectories(fullPath.getParent());
        file.transferTo(fullPath.toFile());

        File fileEntity = new File();
        fileEntity.setUserId(userId);
        fileEntity.setParentId(pId);
        fileEntity.setFileName(originalFilename);
        fileEntity.setFilePath(relativePath);
        fileEntity.setFileSize(fileSize);
        fileEntity.setFileType(fileType);
        fileEntity.setExtension(extension);
        fileEntity.setIsFolder(0);

        fileMapper.insert(fileEntity);

        user.setStorageUsed(user.getStorageUsed() + fileSize);
        userMapper.updateById(user);

        return buildFileInfo(fileEntity);
    }

    @Transactional
    public void renameFile(Long userId, RenameDTO dto) {
        File file = getFileAndCheckOwner(userId, dto.getFileId());

        checkNameExists(userId, file.getParentId(), dto.getNewName(), file.getId());

        String extension = getExtension(dto.getNewName());
        file.setFileName(dto.getNewName());
        file.setExtension(extension);

        fileMapper.updateById(file);
    }

    @Transactional
    public void moveFiles(Long userId, MoveDTO dto) {
        Long targetParentId = dto.getTargetParentId() != null ? dto.getTargetParentId() : 0L;

        if (targetParentId != 0L) {
            File targetFolder = getFileAndCheckOwner(userId, targetParentId);
            if (targetFolder.getIsFolder() != 1) {
                throw new IllegalArgumentException("目标不是文件夹");
            }
        }

        for (Long fileId : dto.getFileIds()) {
            File file = getFileAndCheckOwner(userId, fileId);
            checkNameExists(userId, targetParentId, file.getFileName(), file.getId());
            file.setParentId(targetParentId);
            fileMapper.updateById(file);
        }
    }

    @Transactional
    public void moveToRecycleBin(Long userId, List<Long> fileIds) {
        for (Long fileId : fileIds) {
            File file = getFileAndCheckOwner(userId, fileId);
            file.setDeleted(1);
            file.setDeleteTime(LocalDateTime.now());
            fileMapper.updateById(file);

            if (file.getIsFolder() == 1) {
                deleteChildrenToRecycleBin(userId, fileId);
            }
        }
    }

    private void deleteChildrenToRecycleBin(Long userId, Long parentId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId)
                .eq(File::getDeleted, 0);

        List<File> children = fileMapper.selectList(wrapper);
        for (File child : children) {
            child.setDeleted(1);
            child.setDeleteTime(LocalDateTime.now());
            fileMapper.updateById(child);

            if (child.getIsFolder() == 1) {
                deleteChildrenToRecycleBin(userId, child.getId());
            }
        }
    }

    @Transactional
    public void restoreFromRecycleBin(Long userId, List<Long> fileIds) {
        for (Long fileId : fileIds) {
            LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(File::getId, fileId)
                    .eq(File::getUserId, userId)
                    .eq(File::getDeleted, 1);

            File file = fileMapper.selectOne(wrapper);
            if (file == null) {
                throw new IllegalArgumentException("文件不存在或不在回收站中");
            }

            file.setDeleted(0);
            file.setDeleteTime(null);
            fileMapper.updateById(file);

            if (file.getIsFolder() == 1) {
                restoreChildren(userId, fileId);
            }
        }
    }

    private void restoreChildren(Long userId, Long parentId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId)
                .eq(File::getDeleted, 1);

        List<File> children = fileMapper.selectList(wrapper);
        for (File child : children) {
            child.setDeleted(0);
            child.setDeleteTime(null);
            fileMapper.updateById(child);

            if (child.getIsFolder() == 1) {
                restoreChildren(userId, child.getId());
            }
        }
    }

    @Transactional
    public void permanentDelete(Long userId, List<Long> fileIds) {
        for (Long fileId : fileIds) {
            LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(File::getId, fileId)
                    .eq(File::getUserId, userId)
                    .eq(File::getDeleted, 1);

            File file = fileMapper.selectOne(wrapper);
            if (file == null) {
                throw new IllegalArgumentException("文件不存在或不在回收站中");
            }

            if (file.getIsFolder() == 1) {
                deleteFolderPermanently(userId, fileId);
            } else {
                if (file.getFilePath() != null) {
                    Path path = Paths.get(storagePath, file.getFilePath());
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException("删除物理文件失败", e);
                    }
                }

                User user = userMapper.selectById(userId);
                if (file.getFileSize() != null && file.getFileSize() > 0) {
                    user.setStorageUsed(Math.max(0, user.getStorageUsed() - file.getFileSize()));
                    userMapper.updateById(user);
                }

                fileMapper.deleteById(fileId);
            }
        }
    }

    private void deleteFolderPermanently(Long userId, Long folderId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, folderId)
                .eq(File::getDeleted, 1);

        List<File> children = fileMapper.selectList(wrapper);
        for (File child : children) {
            if (child.getIsFolder() == 1) {
                deleteFolderPermanently(userId, child.getId());
            } else {
                if (child.getFilePath() != null) {
                    Path path = Paths.get(storagePath, child.getFilePath());
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException("删除物理文件失败", e);
                    }
                }

                User user = userMapper.selectById(userId);
                if (child.getFileSize() != null && child.getFileSize() > 0) {
                    user.setStorageUsed(Math.max(0, user.getStorageUsed() - child.getFileSize()));
                    userMapper.updateById(user);
                }

                fileMapper.deleteById(child.getId());
            }
        }
        fileMapper.deleteById(folderId);
    }

    public File getFileForDownload(Long userId, Long fileId) {
        File file = getFileAndCheckOwner(userId, fileId);
        if (file.getIsFolder() == 1) {
            throw new IllegalArgumentException("不能下载文件夹");
        }
        return file;
    }

    public File getFileForPreview(Long userId, Long fileId) {
        File file = getFileAndCheckOwner(userId, fileId);
        if (file.getIsFolder() == 1) {
            throw new IllegalArgumentException("不能预览文件夹");
        }
        return file;
    }

    public Path getFilePath(String relativePath) {
        return Paths.get(storagePath, relativePath);
    }

    private File getFileAndCheckOwner(Long userId, Long fileId) {
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        if (!file.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限访问此文件");
        }
        return file;
    }

    private void checkNameExists(Long userId, Long parentId, String fileName, Long excludeId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId)
                .eq(File::getFileName, fileName)
                .eq(File::getDeleted, 0);

        if (excludeId != null) {
            wrapper.ne(File::getId, excludeId);
        }

        if (fileMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("名称已存在: " + fileName);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getFileType(String extension) {
        if (extension == null) {
            return "application/octet-stream";
        }
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "pdf" -> "application/pdf";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            case "ppt", "pptx" -> "application/vnd.ms-powerpoint";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "mp3" -> "audio/mpeg";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }

    private Map<String, Object> buildFileInfo(File file) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", file.getId());
        info.put("userId", file.getUserId());
        info.put("parentId", file.getParentId());
        info.put("fileName", file.getFileName());
        info.put("fileSize", file.getFileSize());
        info.put("fileType", file.getFileType());
        info.put("extension", file.getExtension());
        info.put("isFolder", Integer.valueOf(1).equals(file.getIsFolder()));
        info.put("createTime", file.getCreateTime());
        info.put("updateTime", file.getUpdateTime());
        info.put("deleted", Integer.valueOf(1).equals(file.getDeleted()));
        info.put("deleteTime", file.getDeleteTime());
        return info;
    }

    public List<Map<String, Object>> getPathBreadcrumb(Long userId, Long fileId) {
        List<Map<String, Object>> breadcrumb = new ArrayList<>();
        
        if (fileId == null || fileId == 0) {
            Map<String, Object> root = new HashMap<>();
            root.put("id", 0L);
            root.put("fileName", "根目录");
            breadcrumb.add(root);
            return breadcrumb;
        }

        File file = getFileAndCheckOwner(userId, fileId);
        List<Map<String, Object>> temp = new ArrayList<>();
        
        while (file != null) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", file.getId());
            item.put("fileName", file.getFileName());
            temp.add(item);
            
            if (file.getParentId() == 0) {
                Map<String, Object> root = new HashMap<>();
                root.put("id", 0L);
                root.put("fileName", "根目录");
                temp.add(root);
                break;
            }
            
            file = fileMapper.selectById(file.getParentId());
        }
        
        for (int i = temp.size() - 1; i >= 0; i--) {
            breadcrumb.add(temp.get(i));
        }
        
        return breadcrumb;
    }
}
