package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.vo.AgentWaitStatusVO;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentWaitStatusQueryService {

  private final HerbDigitalTwinAgentTaskService agentTaskService;
  private final AgentWaitConditionMapper waitConditionMapper;
  private final HerbCollectionTaskService collectionTaskService;
  private final ObjectMapper objectMapper;

  public AgentWaitStatusQueryService(
      HerbDigitalTwinAgentTaskService agentTaskService,
      AgentWaitConditionMapper waitConditionMapper,
      HerbCollectionTaskService collectionTaskService,
      ObjectMapper objectMapper) {
    this.agentTaskService = agentTaskService;
    this.waitConditionMapper = waitConditionMapper;
    this.collectionTaskService = collectionTaskService;
    this.objectMapper = objectMapper;
  }

  public AgentWaitStatusVO get(Long agentTaskId) {
    agentTaskService.getDetail(agentTaskId);
    AgentWaitConditionEntity condition = waitConditionMapper.selectByAgentTaskId(agentTaskId);
    if (condition == null) {
      throw new BusinessException("该 Agent 任务尚未进入现场数据等待阶段");
    }
    HerbCollectionTaskVO followUpTask =
        collectionTaskService.getById(condition.getFollowUpTaskId());
    Snapshot snapshot = readSnapshot(condition.getCurrentSnapshotJson());
    return new AgentWaitStatusVO(
        followUpTask.getId(),
        followUpTask.getTaskName(),
        condition.getStatus(),
        condition.getDeadline(),
        snapshot == null ? List.of() : snapshot.completedRequirements(),
        snapshot == null ? List.of() : snapshot.missingRequirements(),
        snapshot == null ? 0 : snapshot.progressPercent(),
        condition.getLastCheckTime(),
        nextActionHint(condition.getStatus()));
  }

  private Snapshot readSnapshot(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, Snapshot.class);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("Agent 等待状态快照格式无效");
    }
  }

  private String nextActionHint(String status) {
    return switch (status) {
      case "SATISFIED" -> "现场数据已满足，Agent 正在准备重新分析。";
      case "EXPIRED" -> "请确认延长等待、重新生成方案或结束本次研究任务。";
      case "CANCELLED" -> "Agent 工作流已取消，真实采集数据仍保留。";
      default -> "请按复测任务要求继续补充缺失的现场数据。";
    };
  }
}
