package com.mydrive.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareDTO {
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    private String password;
    private Integer expireDays;
}
