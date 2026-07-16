package com.bdis.modules.assistant.agent.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AgentFindingVO {

    private Long id;
    private Long stepId;
    private String findingType;
    private String severity;
    private String targetType;
    private Long targetId;
    private String title;
    private String description;
    private String suggestion;
    private String status;
    private LocalDateTime resolvedTime;
    private LocalDateTime createTime;
}
