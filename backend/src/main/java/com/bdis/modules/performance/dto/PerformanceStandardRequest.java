package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceStandardRequest {

    private String standardNo;

    @NotBlank(message = "标准名称不能为空")
    private String standardName;

    @NotBlank(message = "业绩类型不能为空")
    private String performanceType;

    private String standardDesc;

    private String scoreRule;

    private Integer sortOrder;

    private String remark;
}
