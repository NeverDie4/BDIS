package com.bdis.modules.assistant.agent.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentStepVO {

    private Long id;
    private Integer stepNo;
    private String stepType;
    private String stepName;
    private String description;
    private String status;
    private String statusLabel;
    private String outputSummary;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
}
