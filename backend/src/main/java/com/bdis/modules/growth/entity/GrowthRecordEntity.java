package com.bdis.modules.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_growth_record")
public class GrowthRecordEntity extends BaseEntity {

    private Long speciesId;

    private Long distributionId;

    private Long collectorId;

    private String collectorNameSnapshot;

    private Long regionId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String growthStage;

    private String soilType;

    private BigDecimal soilPh;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private String weather;

    private BigDecimal sampleWeight;

    private String deviceType;

    private String dataSource;

    private String externalSource;

    private String externalNo;

    private String reviewStatus;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime archivedAt;

    private LocalDateTime collectedAt;
}
