package com.bdis.modules.evaluation.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationConfirmationRequest {

    private Long confirmedBy;

    private BigDecimal totalScore;

    private String resultLevel;

    private String resultDesc;

    private String remark;
}
