package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("assistant_agent_tool_call_log")
public class AgentToolCallLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentTaskId;
    private Long stepId;
    private Long userId;
    private String sessionId;
    private String toolName;
    private String requestSummary;
    private String inputHash;
    private String outputSummary;
    private String outputJson;
    private Integer success;
    private String errorCode;
    private String errorMessage;
    private Long timeCostMs;
    private LocalDateTime createTime;
}
