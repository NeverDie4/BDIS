package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrainingReportSubmitRequest {
    private Long fileId;
    @NotBlank private String content;
}
