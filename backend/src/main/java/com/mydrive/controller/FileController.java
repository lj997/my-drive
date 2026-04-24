package com.mydrive.controller;

import com.mydrive.common.Result;
import com.mydrive.dto.CreateFolderDTO;
import com.mydrive.dto.MoveDTO;
import com.mydrive.dto.RenameDTO;
import com.mydrive.entity.File;
import com.mydrive.service.FileService;
import com.mydrive.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listFiles(
            @RequestParam(required = false) Long parentId) {
        Long userId = UserContext.getUserId();
        return Result.success(fileService.listFiles(userId, parentId));
    }

    @GetMapping("/breadcrumb")
    public Result<List<Map<String, Object>>> getBreadcrumb(
            @RequestParam(required = false) Long fileId) {
        Long userId = UserContext.getUserId();
        return Result.success(fileService.getPathBreadcrumb(userId, fileId));
    }

    @PostMapping("/folder")
    public Result<Map<String, Object>> createFolder(@Valid @RequestBody CreateFolderDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(fileService.createFolder(userId, dto));
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(
            @RequestParam(required = false) Long parentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = UserContext.getUserId();
        return Result.success(fileService.uploadFile(userId, parentId, file));
    }

    @PostMapping("/rename")
    public Result<Void> renameFile(@Valid @RequestBody RenameDTO dto) {
        Long userId = UserContext.getUserId();
        fileService.renameFile(userId, dto);
        return Result.success();
    }

    @PostMapping("/move")
    public Result<Void> moveFiles(@Valid @RequestBody MoveDTO dto) {
        Long userId = UserContext.getUserId();
        fileService.moveFiles(userId, dto);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> moveToRecycleBin(@RequestBody List<Long> fileIds) {
        Long userId = UserContext.getUserId();
        fileService.moveToRecycleBin(userId, fileIds);
        return Result.success();
    }

    @GetMapping("/recycle")
    public Result<List<Map<String, Object>>> listRecycleBin() {
        Long userId = UserContext.getUserId();
        return Result.success(fileService.listRecycleBin(userId));
    }

    @PostMapping("/recycle/restore")
    public Result<Void> restoreFromRecycleBin(@RequestBody List<Long> fileIds) {
        Long userId = UserContext.getUserId();
        fileService.restoreFromRecycleBin(userId, fileIds);
        return Result.success();
    }

    @PostMapping("/recycle/delete")
    public Result<Void> permanentDelete(@RequestBody List<Long> fileIds) {
        Long userId = UserContext.getUserId();
        fileService.permanentDelete(userId, fileIds);
        return Result.success();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam Long fileId) throws IOException {
        Long userId = UserContext.getUserId();
        File file = fileService.getFileForDownload(userId, fileId);

        Path filePath = fileService.getFilePath(file.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在");
        }

        InputStream inputStream = Files.newInputStream(filePath);
        Resource resource = new InputStreamResource(inputStream);

        String encodedFileName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentLength(file.getFileSize())
                .body(resource);
    }

    @GetMapping("/preview")
    public ResponseEntity<Resource> previewFile(@RequestParam Long fileId) throws IOException {
        Long userId = UserContext.getUserId();
        File file = fileService.getFileForPreview(userId, fileId);

        Path filePath = fileService.getFilePath(file.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在");
        }

        InputStream inputStream = Files.newInputStream(filePath);
        Resource resource = new InputStreamResource(inputStream);

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
