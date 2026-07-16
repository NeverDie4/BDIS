package com.bdis.modules.assistant.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentActionRejectRequest(@NotBlank @Size(max = 500) String reason) {}
