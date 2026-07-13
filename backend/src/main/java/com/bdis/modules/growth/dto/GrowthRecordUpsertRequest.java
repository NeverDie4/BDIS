package com.bdis.modules.growth.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthRecordUpsertRequest {
    private Long batchId;
    private Long speciesId;

    private Long distributionId;
    private Long regionId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String growthStage;
    private BigDecimal plantHeight;
    private String soilType;
    private BigDecimal soilPh;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal soilMoisture;
    private BigDecimal light;
    private BigDecimal stemDiameter;
    private String leafColor;
    private String floweringStatus;
    private String growthEvaluation;
    private String weather;

    @DecimalMin(value = "0.0", message = "采集重量不能小于 0")
    private BigDecimal sampleWeight;

    private String deviceType;
    private String dataSource;
    private LocalDateTime collectedAt;
    private String remark;
}
