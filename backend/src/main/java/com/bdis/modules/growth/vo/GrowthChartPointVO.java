package com.bdis.modules.growth.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrowthChartPointVO {

    private Long recordId;
    private Long taskId;
    private Long batchId;
    private String batchName;
    private LocalDateTime collectTime;
    private String metric;
    private BigDecimal value;
    private BigDecimal plantHeight;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal soilMoisture;
    private BigDecimal soilPh;
    private BigDecimal light;
    private String growthStage;
    private String collectorName;
    private String auditStatus;
    private String summary;
}
