package com.bdis.modules.spectrum.vo;

import lombok.Data;

@Data
public class FeatureBatchExtractItemVO {

    private Long atlasId;

    private String atlasCode;

    private Long imageId;

    private String imageCode;

    private String imageName;

    private Long speciesId;

    private String status;

    private String message;
}
