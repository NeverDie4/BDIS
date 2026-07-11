package com.bdis.modules.assistant.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbAssistantMatchContextVO {

    private Integer rank;

    private String speciesName;

    private BigDecimal similarity;

    private String atlasName;
}
