package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.entity.AgentToolCallLogEntity;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AgentToolCallLogMapper {

    int insert(AgentToolCallLogEntity log);

    List<AgentToolCallLogEntity> selectByTaskId(@Param("agentTaskId") Long agentTaskId);
}
