package com.bdis.modules.spectrum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "herb.feature")
public class HerbFeatureProperties {

    private boolean enabled = true;

    private String serviceUrl = "http://localhost:8002/extract-feature";

    private Integer timeoutSeconds = 30;

    private boolean mockEnabled = false;

    private String modelName = "resnet50";

    private String modelVersion = "resnet50-imagenet-v1";

    private Integer mockDimension = 512;

    private boolean recoveryEnabled = true;
}
