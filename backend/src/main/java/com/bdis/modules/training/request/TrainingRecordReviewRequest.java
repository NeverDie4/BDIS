package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrainingRecordReviewRequest {
    @NotBlank private String action;
    private String comment;
    private java.math.BigDecimal score;
}
