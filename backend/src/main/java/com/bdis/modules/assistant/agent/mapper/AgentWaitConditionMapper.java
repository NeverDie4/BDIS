package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentWaitConditionMapper {

  int insert(AgentWaitConditionEntity condition);

  AgentWaitConditionEntity selectById(@Param("id") Long id);

  AgentWaitConditionEntity selectByAgentTaskId(@Param("agentTaskId") Long agentTaskId);

  List<AgentWaitConditionEntity> selectActiveByFollowUpTaskId(
      @Param("followUpTaskId") Long followUpTaskId);

  List<AgentWaitConditionEntity> selectRecoveryCandidates(@Param("limit") int limit);

  int updateCheck(
      @Param("id") Long id,
      @Param("expectedVersion") Integer expectedVersion,
      @Param("status") String status,
      @Param("snapshotJson") String snapshotJson,
      @Param("checkTime") LocalDateTime checkTime,
      @Param("satisfiedTime") LocalDateTime satisfiedTime);

  int incrementTerminalCheckCount(
      @Param("id") Long id, @Param("checkTime") LocalDateTime checkTime);

  int cancelByAgentTaskId(
      @Param("agentTaskId") Long agentTaskId, @Param("updateTime") LocalDateTime updateTime);
}
