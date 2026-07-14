package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_training_material")
public class TrainingMaterialEntity extends BaseEntity {

    private String materialNo;

    private String materialName;

    private String materialType;

    private String description;

    private Long fileId;

    private String sourceType;

    private Long sourceResourceId;

    private Long uploaderId;

    private LocalDateTime uploadedAt;

    private Integer reuseCount;
}
