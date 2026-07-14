package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_training_plan")
public class TrainingPlanEntity extends BaseEntity {

    private String planNo;

    private String planName;

    private String planType;

    private Long ownerId;

    private Long courseId;

    private Long trainerId;

    private String description;

    private String location;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String publishStatus;

    private LocalDateTime publishedAt;

    private Long publishedBy;
}
