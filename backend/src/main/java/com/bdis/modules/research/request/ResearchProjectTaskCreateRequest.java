package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ResearchProjectTaskCreateRequest {
    @NotBlank
    @Size(max = 64)
    private String taskNo;

    @NotBlank
    @Size(max = 200)
    private String taskName;

    private String description;
    private Long responsibleUserId;
    private Long baseId;
    private Long sourceRecordId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime deadlineAt;
    private List<Long> courseIds;
    private List<Long> speciesIds;
}
