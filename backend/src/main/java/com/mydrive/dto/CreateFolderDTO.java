package com.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFolderDTO {
    private Long parentId;
    @NotBlank(message = "文件夹名称不能为空")
    private String folderName;
}
