package com.bdis.modules.growth.vo;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

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
