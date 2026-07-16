package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("assistant_agent_collection_requirement")
public class AgentCollectionRequirementEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long collectionPlanId;
  private Long collectionTaskId;
  private String requirementType;
  private String requirementCode;
  private String requirementName;
  private Integer required;
  private Integer minCount;
  private String unit;
  private String guidance;
  private String reason;
  private Integer sortOrder;
  private LocalDateTime createTime;
}
