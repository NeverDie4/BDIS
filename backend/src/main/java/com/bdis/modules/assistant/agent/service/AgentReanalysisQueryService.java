package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.agent.entity.AgentAnalysisRoundEntity;
import com.bdis.modules.assistant.agent.mapper.AgentAnalysisRoundMapper;
import com.bdis.modules.assistant.agent.vo.AgentAnalysisRoundVO;
import com.bdis.modules.assistant.agent.vo.AgentReanalysisComparisonVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentReanalysisQueryService {

  private final HerbDigitalTwinAgentTaskService agentTaskService;
  private final AgentAnalysisRoundMapper roundMapper;
  private final ObjectMapper objectMapper;

  public AgentReanalysisQueryService(
      HerbDigitalTwinAgentTaskService agentTaskService,
      AgentAnalysisRoundMapper roundMapper,
      ObjectMapper objectMapper) {
    this.agentTaskService = agentTaskService;
    this.roundMapper = roundMapper;
    this.objectMapper = objectMapper;
  }

  public AgentReanalysisComparisonVO latest(Long agentTaskId) {
    agentTaskService.getDetail(agentTaskId);
    AgentAnalysisRoundEntity round = roundMapper.selectLatestByTaskId(agentTaskId);
    if (round == null || !"SUCCEEDED".equals(round.getStatus())) {
      throw new BusinessException("该 Agent 任务尚无可查询的重新分析结果");
    }
    try {
      return objectMapper.readValue(round.getChangeSummary(), AgentReanalysisComparisonVO.class);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("重新分析结果格式无效");
    }
  }

  public List<AgentAnalysisRoundVO> rounds(Long agentTaskId) {
    agentTaskService.getDetail(agentTaskId);
    return roundMapper.selectByTaskId(agentTaskId).stream().map(this::toVO).toList();
  }

  private AgentAnalysisRoundVO toVO(AgentAnalysisRoundEntity entity) {
    return new AgentAnalysisRoundVO(
        entity.getId(),
        entity.getRoundNo(),
        entity.getRoundType(),
        entity.getSourceCollectionTaskId(),
        entity.getFollowUpCollectionTaskId(),
        entity.getBaselineHash(),
        entity.getCurrentHash(),
        entity.getConclusion(),
        entity.getOutcome(),
        entity.getStatus(),
        entity.getStartTime(),
        entity.getFinishTime());
  }
}
