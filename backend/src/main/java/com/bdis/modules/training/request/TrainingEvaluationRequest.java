package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainingEvaluationRequest {
    @NotBlank private String dimensionCode;
    @NotNull private java.math.BigDecimal score;
    private String comment;
}
