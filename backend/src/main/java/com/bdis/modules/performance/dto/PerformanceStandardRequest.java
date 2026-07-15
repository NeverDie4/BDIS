package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
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

    private String levelRule;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private Boolean materialRequired;

    @Min(value = 0, message = "最少材料数量不能小于 0")
    private Integer minMaterialCount;

    private Integer sortOrder;

    private String remark;
}
