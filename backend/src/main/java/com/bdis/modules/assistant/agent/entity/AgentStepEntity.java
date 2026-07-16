package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("assistant_agent_step")
public class AgentStepEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentTaskId;
    private Integer stepNo;
    private String stepType;
    private String stepName;
    private String description;
    private String status;
    private String inputSnapshot;
    private String outputSummary;
    private String outputJson;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
