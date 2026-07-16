package com.bdis.modules.assistant.agent.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentActionVO {

    private Long id;
    private Long stepId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String actionName;
    private String actionDescription;
    private String riskLevel;
    private Boolean needConfirm;
    private String status;
    private LocalDateTime requestedTime;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private LocalDateTime rejectedTime;
    private LocalDateTime executedTime;
    private String resultSummary;
    private String errorMessage;
}
