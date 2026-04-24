package com.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RenameDTO {
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    @NotBlank(message = "新名称不能为空")
    private String newName;
}
