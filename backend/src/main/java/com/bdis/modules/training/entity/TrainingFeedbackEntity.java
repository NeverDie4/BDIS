package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_training_feedback")
public class TrainingFeedbackEntity extends BasicEntity {

    private Long trainingRecordId;

    private Long userId;

    private BigDecimal rating;

    private String feedbackContent;

    private LocalDateTime submittedAt;
}
