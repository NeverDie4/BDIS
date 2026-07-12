package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceMaterialRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    private String fileUsage;

    private Integer sortOrder;

    private String remark;
}
