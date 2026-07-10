package com.bdis.modules.spectrum.client;

import java.util.List;
import lombok.Data;

@Data
public class FeatureExtractionClientResponse {

    private Boolean success;

    private List<Double> featureVector;

    private Integer dimension;

    private String modelName;

    private String modelVersion;

    private String message;
}
