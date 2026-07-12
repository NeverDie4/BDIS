package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceRequest {

    private String performanceNo;

    @NotBlank(message = "业绩标题不能为空")
    private String performanceTitle;

    private String performanceType;
    private Long standardId;
    private String sourceType;
    private Long sourceId;
    private String remark;
}
