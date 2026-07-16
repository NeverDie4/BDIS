package com.bdis.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_indicator")
public class EvaluationIndicatorEntity extends BaseEntity {

    private String indicatorNo;

    private String indicatorName;

    private String indicatorType;

    private Long parentId;

    private BigDecimal weight;

    private BigDecimal maxScore;

    private String scoreDesc;

    private Integer sortOrder;
}
