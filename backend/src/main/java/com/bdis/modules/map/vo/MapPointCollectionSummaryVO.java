package com.bdis.modules.map.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MapPointCollectionSummaryVO {

    private Long pointId;
    private int recordCount;
    private LocalDateTime latestCollectedAt;
    private String latestGrowthStage;
    private List<MonthlySnapshot> monthlySnapshots;

    @Data
    public static class MonthlySnapshot {
        private String month;
        private LocalDateTime collectedAt;
        private String growthStage;
    }
}
