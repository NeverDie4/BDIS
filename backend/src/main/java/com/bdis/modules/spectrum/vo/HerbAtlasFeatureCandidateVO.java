package com.bdis.modules.spectrum.vo;

import lombok.Data;

@Data
public class HerbAtlasFeatureCandidateVO {

    private Long atlasId;

    private String atlasCode;

    private String atlasImageUrl;

    private Long speciesId;

    private String speciesName;

    private String featureVector;
}
