package com.bdis.modules.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "herb.assistant")
public class HerbAssistantProperties {

    private boolean enabled = true;

    private boolean mockEnabled = true;

    private String model;

    private String baseUrl;

    private String apiKey;

    private Integer timeoutSeconds = 60;
}
