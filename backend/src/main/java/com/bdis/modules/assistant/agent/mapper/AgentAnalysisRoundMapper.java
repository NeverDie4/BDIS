package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentAnalysisRoundEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentAnalysisRoundMapper {

  int insertIfAbsent(AgentAnalysisRoundEntity round);

  AgentAnalysisRoundEntity selectByTaskAndRound(
      @Param("agentTaskId") Long agentTaskId, @Param("roundNo") Integer roundNo);

  AgentAnalysisRoundEntity selectLatestByTaskId(@Param("agentTaskId") Long agentTaskId);

  List<AgentAnalysisRoundEntity> selectByTaskId(@Param("agentTaskId") Long agentTaskId);

  int markRunning(
      @Param("id") Long id,
      @Param("baselineSnapshot") String baselineSnapshot,
      @Param("baselineHash") String baselineHash,
      @Param("currentSnapshot") String currentSnapshot,
      @Param("currentHash") String currentHash,
      @Param("startTime") LocalDateTime startTime);

  int markSucceeded(
      @Param("id") Long id,
      @Param("currentSnapshot") String currentSnapshot,
      @Param("currentHash") String currentHash,
      @Param("changeSummary") String changeSummary,
      @Param("conclusion") String conclusion,
      @Param("outcome") String outcome,
      @Param("finishTime") LocalDateTime finishTime);

  int markFailed(
      @Param("id") Long id,
      @Param("conclusion") String conclusion,
      @Param("finishTime") LocalDateTime finishTime);
}
