package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceRequest {

    @Size(max = 64, message = "业绩编号不能超过 64 个字符")
    private String performanceNo;

    @NotBlank(message = "业绩标题不能为空")
    @Size(max = 200, message = "业绩标题不能超过 200 个字符")
    private String performanceTitle;

    @Size(max = 50, message = "业绩类型不能超过 50 个字符")
    private String performanceType;

    @Size(max = 50, message = "业绩等级不能超过 50 个字符")
    private String performanceLevel;

    private LocalDateTime occurredAt;

    private Long standardId;

    @Size(max = 50, message = "来源类型不能超过 50 个字符")
    private String sourceType;

    private Long sourceId;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
