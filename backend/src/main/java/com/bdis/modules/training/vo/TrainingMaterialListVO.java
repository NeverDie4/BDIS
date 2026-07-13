package com.bdis.modules.training.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingMaterialListVO {
    private Long id;
    private String materialNo;
    private String materialName;
    private String materialType;
    private String description;
    private Long fileId;
    private String fileName;
    private String fileType;
    private String sourceType;
    private Long sourceResourceId;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime uploadedAt;
    private Integer reuseCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
