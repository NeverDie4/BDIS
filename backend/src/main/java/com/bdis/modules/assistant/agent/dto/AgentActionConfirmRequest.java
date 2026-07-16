package com.bdis.modules.assistant.agent.dto;

import jakarta.validation.constraints.Size;

public record AgentActionConfirmRequest(
    @Size(max = 500) String comment, Boolean publishAfterCreate) {}
