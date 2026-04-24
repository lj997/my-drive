package com.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShareAccessDTO {
    @NotBlank(message = "分享码不能为空")
    private String shareCode;
    private String password;
}
