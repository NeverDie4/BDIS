package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResearchProjectSubmissionReviewRequest {
    @NotBlank private String action;
    @Size(max = 1000) private String comment;
    private java.math.BigDecimal score;
}
