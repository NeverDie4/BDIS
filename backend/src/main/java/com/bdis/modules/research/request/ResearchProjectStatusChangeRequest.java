package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResearchProjectStatusChangeRequest {
    @NotBlank(message = "targetStatus is required")
    @Size(max = 50, message = "targetStatus must not exceed 50 characters")
    private String targetStatus;

    @Size(max = 500, message = "reason must not exceed 500 characters")
    private String reason;

    @NotNull(message = "version is required")
    private Integer version;
}
