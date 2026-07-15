package com.bdis.modules.training.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TrainingRecordItemProgressRequest {
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private java.math.BigDecimal progress;

    @NotNull private Boolean completed;
    @Positive private Long submittedFileId;
}
