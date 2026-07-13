package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchProjectUpdateRequest {

    @NotBlank(message = "projectName is required")
    @Size(max = 200, message = "projectName must not exceed 200 characters")
    private String projectName;

    @NotBlank(message = "projectType is required")
    @Size(max = 50, message = "projectType must not exceed 50 characters")
    private String projectType;

    private Long speciesId;
    private String description;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;

    private Integer version;
}
