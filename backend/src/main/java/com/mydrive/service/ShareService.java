package com.mydrive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mydrive.dto.ShareAccessDTO;
import com.mydrive.dto.ShareDTO;
import com.mydrive.entity.File;
import com.mydrive.entity.Share;
import com.mydrive.mapper.FileMapper;
import com.mydrive.mapper.ShareMapper;
import com.mydrive.util.Md5Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareMapper shareMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;

    @Transactional
    public Map<String, Object> createShare(Long userId, ShareDTO dto) {
        File file = getFileAndCheckOwner(userId, dto.getFileId());

        Share share = new Share();
        share.setFileId(dto.getFileId());
        share.setUserId(userId);
        share.setShareCode(generateShareCode());
        share.setViewCount(0);
        share.setDownloadCount(0);

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            share.setPassword(Md5Util.encrypt(dto.getPassword()));
        }

        if (dto.getExpireDays() != null && dto.getExpireDays() > 0) {
            share.setExpireTime(LocalDateTime.now().plusDays(dto.getExpireDays()));
        }

        shareMapper.insert(share);

        Map<String, Object> result = new HashMap<>();
        result.put("shareCode", share.getShareCode());
        result.put("shareUrl", "/api/share/" + share.getShareCode());
        result.put("expireTime", share.getExpireTime());
        result.put("hasPassword", share.getPassword() != null);

        return result;
    }

    public Map<String, Object> getShareInfo(ShareAccessDTO dto) {
        Share share = validateShare(dto.getShareCode());

        if (share.getPassword() != null) {
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("needPassword", true);
                result.put("message", "需要访问密码");
                return result;
            }
            if (!Md5Util.verify(dto.getPassword(), share.getPassword())) {
                throw new IllegalArgumentException("密码错误");
            }
        }

        share.setViewCount(share.getViewCount() + 1);
        shareMapper.updateById(share);

        File file = fileMapper.selectById(share.getFileId());
        if (file == null || file.getDeleted() == 1) {
            throw new IllegalArgumentException("分享的文件已被删除");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("needPassword", false);
        result.put("shareCode", share.getShareCode());
        result.put("fileName", file.getFileName());
        result.put("fileSize", file.getFileSize());
        result.put("fileType", file.getFileType());
        result.put("isFolder", file.getIsFolder() == 1);
        result.put("viewCount", share.getViewCount());
        result.put("createTime", share.getCreateTime());
        result.put("expireTime", share.getExpireTime());

        return result;
    }

    public Share getShareForDownload(String shareCode, String password) {
        Share share = validateShare(shareCode);

        if (share.getPassword() != null) {
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("需要访问密码");
            }
            if (!Md5Util.verify(password, share.getPassword())) {
                throw new IllegalArgumentException("密码错误");
            }
        }

        share.setDownloadCount(share.getDownloadCount() + 1);
        shareMapper.updateById(share);

        return share;
    }

    public File getShareFile(Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new IllegalArgumentException("分享不存在");
        }
        File file = fileMapper.selectById(share.getFileId());
        if (file == null || file.getDeleted() == 1) {
            throw new IllegalArgumentException("分享的文件已被删除");
        }
        return file;
    }

    public Path getFilePath(String relativePath) {
        return fileService.getFilePath(relativePath);
    }

    private Share validateShare(String shareCode) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getShareCode, shareCode)
                .eq(Share::getDeleted, 0);

        Share share = shareMapper.selectOne(wrapper);
        if (share == null) {
            throw new IllegalArgumentException("分享链接无效或已过期");
        }

        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("分享链接已过期");
        }

        return share;
    }

    private File getFileAndCheckOwner(Long userId, Long fileId) {
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        if (!file.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限分享此文件");
        }
        if (file.getDeleted() == 1) {
            throw new IllegalArgumentException("文件在回收站中，无法分享");
        }
        return file;
    }

    private String generateShareCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void cancelShare(Long userId, Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new IllegalArgumentException("分享不存在");
        }
        if (!share.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限取消此分享");
        }
        share.setDeleted(1);
        shareMapper.updateById(share);
    }

    public java.util.List<Map<String, Object>> listShares(Long userId) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getUserId, userId)
                .eq(Share::getDeleted, 0)
                .orderByDesc(Share::getCreateTime);

        java.util.List<Share> shares = shareMapper.selectList(wrapper);
        return shares.stream().map(share -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", share.getId());
            info.put("shareCode", share.getShareCode());
            info.put("fileId", share.getFileId());
            info.put("viewCount", share.getViewCount());
            info.put("downloadCount", share.getDownloadCount());
            info.put("hasPassword", share.getPassword() != null);
            info.put("createTime", share.getCreateTime());
            info.put("expireTime", share.getExpireTime());

            File file = fileMapper.selectById(share.getFileId());
            if (file != null) {
                info.put("fileName", file.getFileName());
                info.put("isFolder", file.getIsFolder() == 1);
            }

            return info;
        }).toList();
    }
}
