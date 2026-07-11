package com.bdis.modules.assistant.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HerbAssistantBatchContextVO {

    private Long batchId;

    private String batchCode;

    private String batchName;

    private String batchStatus;

    private String speciesName;

    private Integer imageCount;

    private Integer identifiedCount;

    private Integer reviewedCount;

    private Integer needReviewCount;

    private String finalSpeciesName;

    private BigDecimal avgSimilarity;

    private String qualityLevel;

    private BigDecimal qualityScore;

    private String evaluationSummary;

    private List<HerbAssistantImageContextVO> images = new ArrayList<>();
}
