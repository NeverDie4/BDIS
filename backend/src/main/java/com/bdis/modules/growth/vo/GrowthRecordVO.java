package com.bdis.modules.growth.vo;

import com.bdis.modules.herb.vo.HerbImageVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrowthRecordVO {

    private Long id;

    private Long batchId;

    private String batchName;

    private Long taskId;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String collectPlace;

    private Long distributionId;

    private Long collectorId;

    private String collectorName;

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

    private String dataSource;

    private String deviceType;

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

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<HerbImageVO> images = new ArrayList<>();
}
