package com.bdis.modules.research.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchAchievementDetailVO {
    private Long id;
    private String achievementNo;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private String achievementName;
    private String achievementType;
    private String achievementStage;
    private String achievementStatus;
    private String description;
    private Long fileId;
    private String fileNo;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private String fileFormat;
    private Long fileSize;
    private String fileUrl;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Integer version;
}
