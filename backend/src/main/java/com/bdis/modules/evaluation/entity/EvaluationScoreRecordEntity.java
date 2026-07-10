package com.bdis.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_score_record")
public class EvaluationScoreRecordEntity extends BaseEntity {

    private Long taskId;

    private Long indicatorId;

    private Long evaluatorId;

    private BigDecimal score;

    private String scoreComment;

    private LocalDateTime scoredAt;
}
