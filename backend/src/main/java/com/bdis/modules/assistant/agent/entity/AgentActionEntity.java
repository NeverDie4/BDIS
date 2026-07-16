package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("assistant_agent_action")
public class AgentActionEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long agentTaskId;
  private Long stepId;
  private String actionType;
  private String targetType;
  private Long targetId;
  private String actionName;
  private String actionDescription;
  private String payloadJson;
  private String riskLevel;
  private Integer needConfirm;
  private String status;
  private LocalDateTime requestedTime;
  private Long confirmedBy;
  private LocalDateTime confirmedTime;
  private LocalDateTime rejectedTime;
  private LocalDateTime executedTime;
  private String resultSummary;
  private String errorMessage;
  private Integer version;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
