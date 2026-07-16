package com.bdis.modules.spectrum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "herb.recognition")
public class HerbRecognitionProperties {

    private boolean enabled = true;

    private boolean mockEnabled = false;

    private String serviceUrl = "http://localhost:8001/recognize";

    private String featureServiceUrl = "http://localhost:8001/feature";

    private Integer timeoutSeconds = 180;
}
