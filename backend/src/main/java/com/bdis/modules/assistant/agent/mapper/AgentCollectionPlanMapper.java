package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

public interface AgentCollectionPlanMapper {

  int insert(AgentCollectionPlanEntity plan);

  AgentCollectionPlanEntity selectById(@Param("id") Long id);

  AgentCollectionPlanEntity selectLatestActive(@Param("agentTaskId") Long agentTaskId);

  Integer selectMaxRound(@Param("agentTaskId") Long agentTaskId);

  int cancelActive(
      @Param("agentTaskId") Long agentTaskId, @Param("updateTime") LocalDateTime updateTime);

  int updateEditable(
      @Param("plan") AgentCollectionPlanEntity plan,
      @Param("expectedVersion") Integer expectedVersion);

  int updateStatus(
      @Param("id") Long id,
      @Param("expectedStatus") String expectedStatus,
      @Param("targetStatus") String targetStatus,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("updateTime") LocalDateTime updateTime);
}
