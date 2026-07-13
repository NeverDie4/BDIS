package com.bdis.modules.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationStandardRequest {

    @NotBlank(message = "指标名称不能为空")
    private String indicatorName;

    private String indicatorNo;

    private String indicatorType;

    private Long parentId;

    @DecimalMin(value = "0.00", message = "权重不能小于 0")
    @DecimalMax(value = "100.00", message = "权重不能大于 100")
    private BigDecimal weight;

    @DecimalMin(value = "0.00", message = "满分不能小于 0")
    private BigDecimal maxScore;

    private String scoreDesc;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
