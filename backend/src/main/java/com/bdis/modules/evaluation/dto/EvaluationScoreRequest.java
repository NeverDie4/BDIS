package com.bdis.modules.evaluation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationScoreRequest {

    @NotNull(message = "评价任务 ID 不能为空")
    private Long taskId;

    @NotNull(message = "评价指标 ID 不能为空")
    private Long indicatorId;

    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "0.00", message = "评分不能小于 0")
    private BigDecimal score;

    private String scoreComment;

    private String remark;
}
