package com.bdis.modules.training.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingMaterialDetailVO extends TrainingMaterialListVO {
    private String originalFilename;
    private String fileFormat;
    private Long fileSize;
    private String fileUrl;
    private String thumbnailUrl;
    private String storageType;
    private String remark;
    private Integer version;
    private Long planCount;
}
