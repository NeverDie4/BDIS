package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResearchProjectSubmissionCreateRequest {
    private Long taskId;
    private Long recordId;
    @NotBlank @Size(max = 50) private String submissionType;
    @NotBlank @Size(max = 200) private String submissionTitle;
    private String content;
    @Positive private Long fileId;
}
