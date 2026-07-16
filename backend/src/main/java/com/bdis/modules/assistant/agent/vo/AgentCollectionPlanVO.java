package com.bdis.modules.assistant.agent.vo;

import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import java.time.LocalDateTime;

public record AgentCollectionPlanVO(
    Long id,
    String planNo,
    Long agentTaskId,
    Integer researchRound,
    Long collectionTaskId,
    String status,
    String statusLabel,
    String planSource,
    FollowUpCollectionPlan plan,
    LocalDateTime createTime,
    LocalDateTime updateTime) {}
