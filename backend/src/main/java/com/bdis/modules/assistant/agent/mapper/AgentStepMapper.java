package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentStepMapper {

  int insert(AgentStepEntity step);

  int insertIfAbsent(AgentStepEntity step);

  List<AgentStepEntity> selectByTaskId(@Param("agentTaskId") Long agentTaskId);

  AgentStepEntity selectByTaskIdAndType(
      @Param("agentTaskId") Long agentTaskId, @Param("stepType") String stepType);

  AgentStepEntity selectByTaskIdAndNo(
      @Param("agentTaskId") Long agentTaskId, @Param("stepNo") Integer stepNo);

  Integer selectMaxStepNo(@Param("agentTaskId") Long agentTaskId);

  int markRunning(@Param("id") Long id, @Param("startTime") LocalDateTime startTime);

  int markSucceeded(
      @Param("id") Long id,
      @Param("outputSummary") String outputSummary,
      @Param("outputJson") String outputJson,
      @Param("finishTime") LocalDateTime finishTime);

  int markFailed(
      @Param("id") Long id,
      @Param("errorCode") String errorCode,
      @Param("errorMessage") String errorMessage,
      @Param("finishTime") LocalDateTime finishTime);

  int markWaiting(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

  int completeWaiting(
      @Param("id") Long id,
      @Param("outputSummary") String outputSummary,
      @Param("outputJson") String outputJson,
      @Param("finishTime") LocalDateTime finishTime);

  int markSkipped(
      @Param("id") Long id,
      @Param("outputSummary") String outputSummary,
      @Param("finishTime") LocalDateTime finishTime);

  int cancelUnfinished(
      @Param("agentTaskId") Long agentTaskId, @Param("finishTime") LocalDateTime finishTime);
}
