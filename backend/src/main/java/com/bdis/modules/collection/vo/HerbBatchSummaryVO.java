package com.bdis.modules.collection.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class HerbBatchSummaryVO {

    private Long batchId;

    private String batchCode;

    private String batchName;

    private Integer imageCount;

    private Integer identifiedCount;

    private Integer reviewedCount;

    private Integer needReviewCount;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal mainSpeciesRatio;

    private BigDecimal avgSimilarity;

    private BigDecimal qualityScore;

    private String qualityLevel;

    private String batchStatus;

    private String evaluationSummary;

    private List<HerbBatchSpeciesStatVO> speciesStats;

    private List<HerbBatchIdentificationItemVO> items;
}
