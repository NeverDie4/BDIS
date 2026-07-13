package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResearchProjectLeaderChangeRequest {
    @NotNull(message = "newLeaderId is required")
    private Long newLeaderId;
    @Size(max = 50, message = "oldLeaderRole must not exceed 50 characters")
    private String oldLeaderRole;
    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason must not exceed 500 characters")
    private String reason;
    @NotNull(message = "version is required")
    private Integer version;
}
