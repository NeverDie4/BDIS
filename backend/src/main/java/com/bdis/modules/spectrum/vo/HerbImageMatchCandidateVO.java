package com.bdis.modules.spectrum.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbImageMatchCandidateVO {

    private Integer rank;

    private Long atlasId;

    private String atlasCode;

    private String atlasImageUrl;

    private Long speciesId;

    private String speciesName;

    private BigDecimal similarity;

    private String matchResult;

    private Boolean doubaoAgreed;
}
