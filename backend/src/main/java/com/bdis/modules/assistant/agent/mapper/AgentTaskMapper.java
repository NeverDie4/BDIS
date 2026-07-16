package com.bdis.modules.assistant.agent.mapper;

import com.bdis.modules.assistant.agent.dto.AgentTaskQueryRequest;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.collection.support.CollectionAccessScope;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgentTaskMapper {

    int insert(AgentTaskEntity task);

    int updateTaskNo(
            @Param("id") Long id,
            @Param("oldTaskNo") String oldTaskNo,
            @Param("taskNo") String taskNo,
            @Param("updateTime") LocalDateTime updateTime);

    AgentTaskEntity selectById(@Param("id") Long id);

    AgentTaskEntity selectActiveDuplicate(
            @Param("userId") Long userId,
            @Param("collectionTaskId") Long collectionTaskId,
            @Param("goalType") String goalType);

    List<AgentTaskEntity> selectPlanRecoveryCandidates(@Param("limit") int limit);

    long countPage(
            @Param("query") AgentTaskQueryRequest query,
            @Param("currentUserId") Long currentUserId,
            @Param("admin") boolean admin,
            @Param("reviewer") boolean reviewer,
            @Param("scope") CollectionAccessScope scope);

    List<AgentTaskEntity> selectPage(
            @Param("query") AgentTaskQueryRequest query,
            @Param("currentUserId") Long currentUserId,
            @Param("admin") boolean admin,
            @Param("reviewer") boolean reviewer,
            @Param("scope") CollectionAccessScope scope,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    int updateState(
            @Param("id") Long id,
            @Param("expectedStatus") String expectedStatus,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("targetStatus") String targetStatus,
            @Param("resultSummary") String resultSummary,
            @Param("startTime") LocalDateTime startTime,
            @Param("finishTime") LocalDateTime finishTime,
            @Param("cancelTime") LocalDateTime cancelTime,
            @Param("updateTime") LocalDateTime updateTime);

    int updateProgress(
            @Param("id") Long id,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("progressPercent") Integer progressPercent,
            @Param("currentPhase") String currentPhase,
            @Param("updateTime") LocalDateTime updateTime);
}
