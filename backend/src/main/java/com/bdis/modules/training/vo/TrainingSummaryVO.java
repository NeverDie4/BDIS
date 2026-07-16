package com.bdis.modules.training.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TrainingSummaryVO {
    private Long planId;
    private String planNo;
    private String planName;
    private String publishStatus;
    private Long totalParticipantCount;
    private Long completedCount;
    private Long failedCount;
    private Long makeupCount;
    private Long notStartedCount;
    private Long learningCount;
    private Long pendingAttendanceCount;
    private Long presentCount;
    private Long lateCount;
    private Long absentCount;
    private Long leaveCount;
    private BigDecimal averageProgress;
    private Long scoredParticipantCount;
    private BigDecimal averageScore;
    private Long feedbackCount;
    private BigDecimal averageRating;
}
