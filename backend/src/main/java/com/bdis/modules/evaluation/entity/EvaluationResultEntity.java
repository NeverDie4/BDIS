package com.bdis.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_result")
public class EvaluationResultEntity extends BaseEntity {

    private Long taskId;

    private BigDecimal totalScore;

    private String resultLevel;

    private String resultDesc;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;
}
