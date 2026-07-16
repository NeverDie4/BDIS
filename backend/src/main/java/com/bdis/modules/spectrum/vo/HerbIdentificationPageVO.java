package com.bdis.modules.spectrum.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbIdentificationPageVO {

    private Long id;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private Long recognitionId;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Boolean needReview;

    private String reviewStatus;

    private String suggestion;

    private String reviewComment;

    private Long reviewerId;

    private String reviewerName;

    private LocalDateTime reviewTime;

    private String rawSummary;

    private LocalDateTime identifyTime;
}
