package com.bdis.modules.assistant.tool.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbAssistantTaskToolResult {

    private boolean success;

    private String message;

    private Long taskId;

    private String taskCode;

    private String taskName;

    private String taskStatus;

    private String speciesName;

    private String baseName;

    private String collectPlace;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private String collectorName;

    private Long batchCount;
}
