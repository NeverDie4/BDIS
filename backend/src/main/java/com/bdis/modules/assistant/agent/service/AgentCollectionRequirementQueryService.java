package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import com.bdis.modules.assistant.agent.entity.AgentCollectionRequirementEntity;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionPlanMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionRequirementMapper;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.mobile.vo.MobileAgentRequirementsVO;
import com.bdis.modules.mobile.vo.MobileAgentRequirementsVO.RequirementItem;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentCollectionRequirementQueryService {

  private final AgentCollectionRequirementMapper requirementMapper;
  private final AgentCollectionPlanMapper planMapper;
  private final HerbCollectionTaskService collectionTaskService;

  public AgentCollectionRequirementQueryService(
      AgentCollectionRequirementMapper requirementMapper,
      AgentCollectionPlanMapper planMapper,
      HerbCollectionTaskService collectionTaskService) {
    this.requirementMapper = requirementMapper;
    this.planMapper = planMapper;
    this.collectionTaskService = collectionTaskService;
  }

  public MobileAgentRequirementsVO getForMobile(Long collectionTaskId) {
    collectionTaskService.getById(collectionTaskId);
    List<AgentCollectionRequirementEntity> requirements =
        requirementMapper.selectByTaskId(collectionTaskId);
    if (requirements.isEmpty()) {
      return new MobileAgentRequirementsVO(false, null, List.of(), List.of(), List.of());
    }
    AgentCollectionPlanEntity plan =
        planMapper.selectById(requirements.get(0).getCollectionPlanId());
    List<RequirementItem> metrics =
        requirements.stream()
            .filter(item -> "METRIC".equals(item.getRequirementType()))
            .map(this::toItem)
            .toList();
    List<RequirementItem> images =
        requirements.stream()
            .filter(item -> "IMAGE".equals(item.getRequirementType()))
            .map(this::toItem)
            .toList();
    List<String> criteria =
        requirements.stream()
            .filter(item -> "NOTE".equals(item.getRequirementType()))
            .filter(item -> item.getRequirementCode().startsWith("completion_"))
            .map(AgentCollectionRequirementEntity::getGuidance)
            .toList();
    return new MobileAgentRequirementsVO(
        true, plan == null ? null : plan.getObjective(), metrics, images, criteria);
  }

  private RequirementItem toItem(AgentCollectionRequirementEntity item) {
    return new RequirementItem(
        item.getRequirementCode(),
        item.getRequirementName(),
        item.getRequired() != null && item.getRequired() == 1,
        item.getMinCount(),
        item.getUnit(),
        item.getGuidance(),
        item.getReason());
  }
}
