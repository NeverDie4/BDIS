package com.bdis.modules.course.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CourseLearningSummaryVO {
    private Long courseId;
    private Integer enrollmentCount;
    private Integer completedStepCount;
    private Integer submittedReportCount;
    private Integer ungradedReportCount;
    private BigDecimal averageScore;
    private Integer excellentCount;
    private Integer passCount;
    private Integer failCount;
}
