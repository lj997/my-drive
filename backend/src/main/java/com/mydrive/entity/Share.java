package com.mydrive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shares")
public class Share {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long userId;
    private String shareCode;
    private String password;
    private LocalDateTime expireTime;
    private Integer viewCount = 0;
    private Integer downloadCount = 0;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted = 0;
}
