package com.bdis.modules.research.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProjectMaterialVO {
    private Long bindingId;
    private Long projectId;
    private Long fileId;
    private String fileNo;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private String fileFormat;
    private Long fileSize;
    private String fileUrl;
    private String thumbnailUrl;
    private String fileUsage;
    private String storageType;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime uploadedAt;
    private String remark;
    private LocalDateTime createdAt;
}
