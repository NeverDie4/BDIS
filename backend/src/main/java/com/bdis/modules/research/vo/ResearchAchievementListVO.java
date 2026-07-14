package com.bdis.modules.research.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchAchievementListVO {
    private Long id;
    private String achievementNo;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private String achievementName;
    private String achievementType;
    private String achievementStage;
    private String achievementStatus;
    private Long fileId;
    private String fileName;
    private LocalDateTime publishedAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
