package com.bdis.modules.assistant.agent.dto;

import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredImageItem;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredMetricItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record AgentCollectionPlanUpdateRequest(
    @NotBlank @Size(max = 1000) String objective,
    LocalDateTime recommendedStartTime,
    LocalDateTime recommendedEndTime,
    @NotEmpty List<@Valid RequiredMetricItem> requiredMetrics,
    @NotEmpty List<@Valid RequiredImageItem> requiredImages,
    @NotEmpty List<@NotBlank String> completionCriteria,
    @NotBlank String rationale) {}
