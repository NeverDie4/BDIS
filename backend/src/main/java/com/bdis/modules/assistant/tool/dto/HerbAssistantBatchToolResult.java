package com.bdis.modules.assistant.tool.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbAssistantBatchToolResult {

    private boolean success;

    private String message;

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

    private String evaluationSummary;
}
