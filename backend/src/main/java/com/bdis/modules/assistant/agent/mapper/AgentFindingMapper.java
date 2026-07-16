package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentFindingEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentFindingMapper {

  int insert(AgentFindingEntity finding);

  int upsert(AgentFindingEntity finding);

  List<AgentFindingEntity> selectByTaskId(@Param("agentTaskId") Long agentTaskId);

  int resolve(
      @Param("id") Long id,
      @Param("evidenceJson") String evidenceJson,
      @Param("resolvedTime") LocalDateTime resolvedTime);

  int updateOpenEvidence(
      @Param("id") Long id,
      @Param("evidenceJson") String evidenceJson,
      @Param("updateTime") LocalDateTime updateTime);
}
