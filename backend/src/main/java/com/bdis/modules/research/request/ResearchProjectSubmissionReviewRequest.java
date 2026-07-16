package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResearchProjectSubmissionReviewRequest {
    @NotBlank private String action;

    @Size(max = 1000)
    private String comment;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private java.math.BigDecimal score;

    @NotNull @PositiveOrZero private Integer version;
}
