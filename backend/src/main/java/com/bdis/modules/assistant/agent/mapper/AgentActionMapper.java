package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentActionMapper {

  int insert(AgentActionEntity action);

  AgentActionEntity selectById(@Param("id") Long id);

  List<AgentActionEntity> selectByTaskId(@Param("agentTaskId") Long agentTaskId);

  int cancelUnexecuted(
      @Param("agentTaskId") Long agentTaskId, @Param("updateTime") LocalDateTime updateTime);

  int cancelPendingCollectionPlanAction(
      @Param("agentTaskId") Long agentTaskId, @Param("updateTime") LocalDateTime updateTime);

  int confirm(
      @Param("id") Long id,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("confirmedBy") Long confirmedBy,
      @Param("confirmedTime") LocalDateTime confirmedTime);

  int markExecuting(
      @Param("id") Long id,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("updateTime") LocalDateTime updateTime);

  int markSucceeded(
      @Param("id") Long id,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("resultSummary") String resultSummary,
      @Param("executedTime") LocalDateTime executedTime);

  int markFailed(
      @Param("id") Long id,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("errorMessage") String errorMessage,
      @Param("updateTime") LocalDateTime updateTime);

  int reject(
      @Param("id") Long id,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("resultSummary") String resultSummary,
      @Param("rejectedTime") LocalDateTime rejectedTime);
}
