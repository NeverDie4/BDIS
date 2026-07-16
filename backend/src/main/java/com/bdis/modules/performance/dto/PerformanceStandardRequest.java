package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceStandardRequest {

    @Size(max = 64, message = "标准编号不能超过 64 个字符")
    private String standardNo;

    @NotBlank(message = "标准名称不能为空")
    @Size(max = 150, message = "标准名称不能超过 150 个字符")
    private String standardName;

    @NotBlank(message = "业绩类型不能为空")
    @Size(max = 50, message = "业绩类型不能超过 50 个字符")
    private String performanceType;

    private String standardDesc;

    private String scoreRule;

    private String levelRule;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private Boolean materialRequired;

    @Min(value = 0, message = "最少材料数量不能小于 0")
    private Integer minMaterialCount;

    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sortOrder;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
