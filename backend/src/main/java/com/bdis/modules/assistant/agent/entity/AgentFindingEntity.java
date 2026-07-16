package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("assistant_agent_finding")
public class AgentFindingEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentTaskId;
    private Long stepId;
    private String findingType;
    private String severity;
    private String targetType;
    private Long targetId;
    private String title;
    private String description;
    private String evidenceJson;
    private String suggestion;
    private String status;
    private LocalDateTime resolvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
