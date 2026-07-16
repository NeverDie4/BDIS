package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceMaterialRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    @Size(max = 50, message = "文件用途不能超过 50 个字符")
    private String fileUsage;

    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sortOrder;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
