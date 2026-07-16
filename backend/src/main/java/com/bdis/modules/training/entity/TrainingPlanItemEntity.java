package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_training_plan_item")
public class TrainingPlanItemEntity extends BaseEntity {
    private Long planId;
    private String itemType;
    private String itemTitle;
    private String description;
    private Long courseId;
    private Long projectId;
    private Long baseId;
    private Long speciesId;
    private Long fileId;
    private Integer isRequired;
    private BigDecimal completionWeight;
    private Integer sortOrder;
}
