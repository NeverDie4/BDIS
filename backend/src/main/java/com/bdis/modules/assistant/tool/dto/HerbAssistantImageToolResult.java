package com.bdis.modules.assistant.tool.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbAssistantImageToolResult {

    private boolean success;

    private String message;

    private Long imageId;

    private String imageCode;

    private String imageType;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Integer needReview;

    private String reviewStatus;

    private String suggestion;
}
