package com.mydrive.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class MoveDTO {
    @NotNull(message = "文件ID列表不能为空")
    private List<Long> fileIds;
    private Long targetParentId;
}
