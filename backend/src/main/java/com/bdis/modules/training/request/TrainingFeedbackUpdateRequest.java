package com.bdis.modules.training.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TrainingFeedbackUpdateRequest {
    @NotNull @DecimalMin("1.00") @DecimalMax("5.00") private BigDecimal rating;
    private String feedbackContent;
    @Size(max = 500) private String remark;
}
