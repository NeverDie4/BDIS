package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentCollectionRequirementEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentCollectionRequirementMapper {
  int insert(AgentCollectionRequirementEntity requirement);

  List<AgentCollectionRequirementEntity> selectByTaskId(
      @Param("collectionTaskId") Long collectionTaskId);
}
