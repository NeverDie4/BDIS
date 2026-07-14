package com.bdis.modules.training.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanMaterialVO {
    private Long bindingId;
    private Long planId;
    private Long materialId;
    private String materialNo;
    private String materialName;
    private String materialType;
    private Long fileId;
    private String fileName;
    private Integer isRequired;
    private Integer sortOrder;
    private Integer materialStatus;
    private Integer reuseCount;
    private LocalDateTime createdAt;
    private String remark;
}
