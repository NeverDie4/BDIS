package com.bdis.modules.collection.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbBatchSpeciesStatVO {

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private Integer count;

    private BigDecimal ratio;
}
