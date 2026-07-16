package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentBusinessLinkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentBusinessLinkMapper {
  int insert(AgentBusinessLinkEntity link);

  AgentBusinessLinkEntity selectByAction(
      @Param("agentActionId") Long agentActionId, @Param("relationType") String relationType);

  List<AgentBusinessLinkEntity> selectByAgentTaskId(
      @Param("agentTaskId") Long agentTaskId, @Param("relationType") String relationType);
}
