package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_training_record")
public class TrainingRecordEntity extends BasicEntity {

    private Long userId;

    private Long courseId;

    private Long planId;

    private BigDecimal progress;

    private String trainingStatus;

    private BigDecimal score;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
