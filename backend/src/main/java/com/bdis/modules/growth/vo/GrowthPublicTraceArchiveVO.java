package com.bdis.modules.growth.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GrowthPublicTraceArchiveVO {

    private Long recordId;
    private String traceCode;
    private String speciesName;
    private String herbName;
    private Long taskId;
    private String taskName;
    private Long batchId;
    private String batchName;
    private String baseName;
    private String collectPlace;
    private LocalDateTime collectTime;
    private String collectorName;
    private String growthStage;
    private String auditStatus;

    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal light;
    private BigDecimal soilMoisture;
    private BigDecimal soilPh;
    private String soilType;

    private BigDecimal plantHeight;
    private BigDecimal stemDiameter;
    private String leafColor;
    private String floweringStatus;
    private String growthEvaluation;
    private BigDecimal sampleWeight;

    private String latestAuditResult;
    private String latestAuditComment;
    private LocalDateTime latestAuditTime;
    private String reviewerName;

    private List<GrowthPublicTraceImageVO> images = new ArrayList<>();
    private List<GrowthAuditHistoryVO> auditHistory = new ArrayList<>();
    private List<GrowthTraceEventVO> traceTimeline = new ArrayList<>();
}
