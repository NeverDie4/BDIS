package com.bdis.modules.assistant.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HerbAssistantImageExplainContextVO {

    private Long imageId;

    private String imageCode;

    private String imageType;

    private String growthStage;

    private String healthStatus;

    private String collectPlace;

    private LocalDateTime collectTime;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Integer needReview;

    private String reviewStatus;

    private String suggestion;

    private List<HerbAssistantMatchContextVO> matches = new ArrayList<>();

    private HerbAssistantRecognitionContextVO recognition;
}
