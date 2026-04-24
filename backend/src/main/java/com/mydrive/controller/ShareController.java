package com.mydrive.controller;

import com.mydrive.common.Result;
import com.mydrive.dto.ShareAccessDTO;
import com.mydrive.dto.ShareDTO;
import com.mydrive.entity.File;
import com.mydrive.service.ShareService;
import com.mydrive.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/create")
    public Result<Map<String, Object>> createShare(@Valid @RequestBody ShareDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(shareService.createShare(userId, dto));
    }

    @PostMapping("/info")
    public Result<Map<String, Object>> getShareInfo(@Valid @RequestBody ShareAccessDTO dto) {
        return Result.success(shareService.getShareInfo(dto));
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listShares() {
        Long userId = UserContext.getUserId();
        return Result.success(shareService.listShares(userId));
    }

    @PostMapping("/cancel/{shareId}")
    public Result<Void> cancelShare(@PathVariable Long shareId) {
        Long userId = UserContext.getUserId();
        shareService.cancelShare(userId, shareId);
        return Result.success();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadShare(
            @RequestParam String shareCode,
            @RequestParam(required = false) String password) throws IOException {
        com.mydrive.entity.Share share = shareService.getShareForDownload(shareCode, password);
        File file = shareService.getShareFile(share.getId());

        if (file.getIsFolder() == 1) {
            throw new IllegalArgumentException("不能下载文件夹");
        }

        Path filePath = shareService.getFilePath(file.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在");
        }

        Resource resource = new FileSystemResource(filePath.toFile());

        String encodedFileName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentLength(file.getFileSize())
                .body(resource);
    }

    @GetMapping("/preview")
    public ResponseEntity<Resource> previewShare(
            @RequestParam String shareCode,
            @RequestParam(required = false) String password) throws IOException {
        com.mydrive.entity.Share share = shareService.getShareForDownload(shareCode, password);
        File file = shareService.getShareFile(share.getId());

        if (file.getIsFolder() == 1) {
            throw new IllegalArgumentException("不能预览文件夹");
        }

        Path filePath = shareService.getFilePath(file.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在");
        }

        Resource resource = new FileSystemResource(filePath.toFile());

        String contentType = getPreviewContentType(file.getFileType());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentLength(file.getFileSize())
                .body(resource);
    }

    private String getPreviewContentType(String fileType) {
        if (fileType == null) {
            return "application/octet-stream";
        }
        return switch (fileType) {
            case "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp" -> fileType;
            case "text/plain", "text/html", "text/css" -> "text/plain; charset=utf-8";
            case "application/javascript", "application/json" -> "text/plain; charset=utf-8";
            default -> "application/octet-stream";
        };
    }
}
