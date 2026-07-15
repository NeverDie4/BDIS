package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("edu_training_record_item")
public class TrainingRecordItemEntity extends BaseEntity {
    private Long trainingRecordId;
    private Long planItemId;
    private BigDecimal progress;
    private Integer completed;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long submittedFileId;
}
