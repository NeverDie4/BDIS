package com.bdis.modules.training.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.time.LocalDateTime;
import lombok.Data;
@Data @TableName("edu_training_record_evaluation")
public class TrainingEvaluationEntity extends BasicEntity { private Long trainingRecordId; private String dimensionCode; private java.math.BigDecimal score; private String comment; private Long evaluatorId; private LocalDateTime evaluatedAt; }
