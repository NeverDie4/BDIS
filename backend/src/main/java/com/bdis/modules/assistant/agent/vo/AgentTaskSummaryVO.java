package com.bdis.modules.assistant.agent.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentTaskSummaryVO {

    private Long id;
    private String taskNo;
    private String goalType;
    private String goalText;
    private String status;
    private String statusLabel;
    private String currentPhase;
    private Integer progressPercent;
    private AgentTargetVO target;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
