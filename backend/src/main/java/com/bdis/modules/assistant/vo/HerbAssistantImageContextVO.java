package com.bdis.modules.assistant.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbAssistantImageContextVO {

    private Long imageId;

    private String imageRole;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private Integer needReview;

    private String reviewStatus;

    private String resultSource;

    private String matchResult;

    private String suggestion;
}
