package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rel_training_plan_material")
public class TrainingPlanMaterialEntity extends CreateAuditEntity {

    private Long planId;

    private Long materialId;

    private Integer isRequired;

    private Integer sortOrder;
}
