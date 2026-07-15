package com.bdis.modules.growth.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class HerbDigitalLifeRecognitionVO {

    private String speciesName;

    private BigDecimal confidence;

    private BigDecimal similarity;

    private Boolean needReview;

    private String recognitionSource;

    private String conclusion;
}
