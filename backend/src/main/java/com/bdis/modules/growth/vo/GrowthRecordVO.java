package com.bdis.modules.growth.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrowthRecordVO {

    private Long id;

    private Long speciesId;

    private Long distributionId;

    private String collectorName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String growthStage;

    private String soilType;

    private BigDecimal soilPh;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private String weather;

    private BigDecimal sampleWeight;

    private String dataSource;

    private String reviewStatus;

    private LocalDateTime collectedAt;

    private String remark;
}
