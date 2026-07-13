package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchProjectCreateRequest {

    @NotBlank(message = "projectNo is required")
    @Size(max = 64, message = "projectNo must not exceed 64 characters")
    private String projectNo;

    @NotBlank(message = "projectName is required")
    @Size(max = 200, message = "projectName must not exceed 200 characters")
    private String projectName;

    @NotBlank(message = "projectType is required")
    @Size(max = 50, message = "projectType must not exceed 50 characters")
    private String projectType;

    @NotNull(message = "leaderId is required")
    private Long leaderId;

    private Long speciesId;
    private String description;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
