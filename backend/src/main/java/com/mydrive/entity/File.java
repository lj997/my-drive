package com.mydrive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("files")
public class File {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long parentId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String extension;
    private Integer isFolder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    private LocalDateTime deleteTime;
}
