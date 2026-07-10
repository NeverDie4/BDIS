package com.bdis.modules.spectrum.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FeatureExtractResultVO {

    private String targetType;

    private Long targetId;

    private Long atlasId;

    private Long imageId;

    private Long speciesId;

    private String featureCode;

    private String featureVector;

    private Integer featureDimension;

    private String featureModel;

    private String featureVersion;

    private String extractStatus;

    private LocalDateTime extractTime;

    private String message;

    private String errorMessage;
}
