package com.bdis.modules.spectrum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "herb.feature")
public class HerbFeatureProperties {

    private boolean enabled = true;

    private String serviceUrl = "http://localhost:8001/extract-feature";

    private Integer timeoutSeconds = 180;

    private boolean mockEnabled = false;

    private String modelName = "resnet50";

    private String modelVersion = "v1.0";

    private Integer mockDimension = 512;
}
