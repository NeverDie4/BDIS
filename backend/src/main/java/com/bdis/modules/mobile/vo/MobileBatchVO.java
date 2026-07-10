package com.bdis.modules.mobile.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MobileBatchVO {

    private Long batchId;

    private String batchCode;

    private String batchName;

    private Long taskId;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String originPlace;

    private LocalDateTime collectStartTime;

    private LocalDateTime collectEndTime;

    private String batchStatus;

    private Integer imageCount;

    private Integer identifiedCount;

    private Integer reviewedCount;

    private Integer needReviewCount;

    private String finalSpeciesName;

    private String qualityLevel;

    private BigDecimal qualityScore;

    private String evaluationSummary;

    private LocalDateTime createTime;
}
