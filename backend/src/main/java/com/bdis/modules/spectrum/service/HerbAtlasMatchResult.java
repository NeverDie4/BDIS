package com.bdis.modules.spectrum.service;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbAtlasMatchResult {

    private Long atlasId;

    private Long speciesId;

    private String speciesName;

    private BigDecimal similarity;

    private Integer rank;
}
