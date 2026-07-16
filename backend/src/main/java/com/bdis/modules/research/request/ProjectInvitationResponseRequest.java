package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectInvitationResponseRequest {
    @NotBlank private String response;
}
