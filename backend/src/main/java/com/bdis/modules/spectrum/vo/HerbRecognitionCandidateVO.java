package com.bdis.modules.spectrum.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbRecognitionCandidateVO {

    private Long atlasId;

    private Long speciesId;

    private String name;

    private BigDecimal confidence;

    private BigDecimal similarity;

    private Integer rank;
}
