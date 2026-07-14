package com.bdis.modules.research.query;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchAchievementQuery {
    private String keyword;
    private Long projectId;
    private String achievementType;
    private String achievementStage;
    private String achievementStatus;
    private LocalDateTime publishedFrom;
    private LocalDateTime publishedTo;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
