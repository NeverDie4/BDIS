package com.bdis.modules.research.vo;

import lombok.Data;

@Data
public class ResearchAchievementSummaryVO {
    private long total;
    private long draftCount;
    private long submittedCount;
    private long confirmedCount;
    private long initialCount;
    private long middleCount;
    private long finalCount;
}
