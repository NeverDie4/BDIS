package com.bdis.modules.spectrum.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HerbImageMatchVO {

    private Long imageId;

    private String imageCode;

    private Long bestSpeciesId;

    private String bestSpeciesName;

    private Long bestAtlasId;

    private String bestAtlasImageUrl;

    private BigDecimal bestSimilarity;

    private String matchResult;

    private Boolean needReview;

    private String suggestion;

    private List<HerbImageMatchCandidateVO> candidates = new ArrayList<>();
}
