package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ResearchProjectTaskMemberRequest {
    @NotNull @Positive private Long userId;
    private String memberRole;
}
