package com.bdis.modules.spectrum.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "herb.identification")
public class HerbIdentificationProperties {

    private BigDecimal highThreshold = new BigDecimal("0.85");

    private BigDecimal middleThreshold = new BigDecimal("0.60");

    private Integer topK = 5;

    private Boolean enableDoubaoReview = true;
}
