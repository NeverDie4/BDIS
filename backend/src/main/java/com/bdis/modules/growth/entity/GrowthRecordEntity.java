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

    private Long batchId;

    private Long taskId;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private Long distributionId;

    private Long collectorId;

    private String collectorNameSnapshot;

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

    private String traceCode;

    private String traceQrcodeUrl;

    private String tracePublicUrl;

    private Integer publicVisible;

    private LocalDateTime traceGeneratedTime;

    private Long traceGeneratedBy;

    private String traceGeneratedByName;
}
