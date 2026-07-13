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

    private Long speciesId;

    private String speciesName;

    private Long distributionId;

    private Long collectorId;

    private String collectorName;

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

    private String dataSource;

    private String deviceType;

    private String externalSource;

    private String externalNo;

    private String reviewStatus;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime archivedAt;

    private LocalDateTime collectedAt;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<HerbImageVO> images = new ArrayList<>();
}
