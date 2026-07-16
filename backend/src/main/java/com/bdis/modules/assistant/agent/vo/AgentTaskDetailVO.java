package com.bdis.modules.assistant.agent.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AgentTaskDetailVO extends AgentTaskSummaryVO {

    private String resultSummary;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private List<AgentStepVO> steps;
    private List<AgentFindingVO> findings;
    private List<AgentActionVO> pendingActions;
}
