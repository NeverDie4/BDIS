package com.bdis.modules.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("eval_task")
public class EvaluationTaskEntity extends BaseEntity {

    private String taskNo;

    private String taskName;

    private String taskType;

    private String targetType;

    private Long targetId;

    private Long ownerId;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String taskStatus;
}
