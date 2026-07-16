package com.bdis.modules.growth.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HerbDigitalLifePublicStageVO {

    private Integer sequence;

    private String batchCode;

    private String batchName;

    private String growthStage;

    private LocalDateTime collectedAt;

    private String collectorName;

    private String baseName;

    private String locationName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String auditStatus;

    private LocalDateTime reviewedAt;

    private String dataStatus;

    private HerbDigitalLifeMetricsVO metrics;

    private List<HerbDigitalLifePublicImageVO> images = new ArrayList<>();

    private HerbDigitalLifeRecognitionVO recognition;

    private String aiNarration;

    private String narrationSource;

    private LocalDateTime narrationGeneratedTime;
}
