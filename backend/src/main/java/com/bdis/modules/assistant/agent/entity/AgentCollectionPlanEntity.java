package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("assistant_agent_collection_plan")
public class AgentCollectionPlanEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String planNo;
  private Long agentTaskId;
  private Long sourceStepId;
  private Integer researchRound;
  private Long collectionTaskId;
  private Long speciesId;
  private Long baseId;
  private String objective;
  private String recommendedTimeType;
  private Integer recommendedAfterDays;
  private LocalDateTime recommendedStartTime;
  private LocalDateTime recommendedEndTime;
  private String requiredMetricsJson;
  private String requiredImagesJson;
  private String optionalItemsJson;
  private String completionCriteriaJson;
  private String sourceFindingsJson;
  private String rationale;
  private String uncertainty;
  private String priority;
  private String planSource;
  private String promptVersion;
  private String modelName;
  private String status;

  @Version private Integer version;

  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  private LocalDateTime confirmTime;
  private LocalDateTime expireTime;
}
