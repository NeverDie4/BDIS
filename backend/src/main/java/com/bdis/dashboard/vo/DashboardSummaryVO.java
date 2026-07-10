package com.bdis.dashboard.vo;

import lombok.Data;

@Data
public class DashboardSummaryVO {

    private long herbCount;
    private long baseCount;
    private long mapPointCount;
    private long growthRecordCount;
    private long pendingGrowthReviewCount;
    private long pendingDeclarationReviewCount;
    private long pendingPerformanceReviewCount;
    private long totalPendingTaskCount;
    private long courseCount;
    private long fileCount;
    private long soapFailedCount;
}
