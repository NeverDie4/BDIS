package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResearchProjectReviewRequest {
    @NotBlank private String action;
    private String comment;
}
