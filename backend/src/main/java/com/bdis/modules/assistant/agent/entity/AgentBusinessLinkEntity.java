package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("assistant_agent_business_link")
public class AgentBusinessLinkEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long agentTaskId;
  private Long agentActionId;
  private Long collectionPlanId;
  private String relationType;
  private String businessType;
  private Long businessId;
  private String businessNo;
  private LocalDateTime createTime;
}
