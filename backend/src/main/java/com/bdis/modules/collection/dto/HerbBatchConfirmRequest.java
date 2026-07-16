package com.bdis.modules.collection.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbBatchConfirmRequest {

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private String qualityLevel;

    private BigDecimal qualityScore;

    private String evaluationSummary;

    private String remark;
}
