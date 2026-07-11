package com.bdis.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_batch")
public class HerbBatchEntity extends BaseEntity {

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

    private LocalDateTime harvestTime;

    private LocalDate productionDate;

    private String batchStatus;

    private Integer imageCount;

    private Integer identifiedCount;

    private Integer reviewedCount;

    private Integer needReviewCount;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal avgSimilarity;

    private String qualityLevel;

    private BigDecimal qualityScore;

    private String evaluationSummary;

    private String traceCode;
}
