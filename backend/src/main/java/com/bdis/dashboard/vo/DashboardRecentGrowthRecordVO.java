package com.bdis.dashboard.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DashboardRecentGrowthRecordVO {

    private Long recordId;
    private String herbName;
    private String baseName;
    private String collectorName;
    private String reviewStatus;
    private LocalDateTime collectedAt;
}
