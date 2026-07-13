package com.bdis.modules.assistant.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbAssistantRecognitionContextVO {

    private String speciesName;

    private BigDecimal confidence;

    private String reason;

    private String suggestion;
}
