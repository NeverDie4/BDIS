package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.agent.dto.AgentActionConfirmRequest;
import com.bdis.modules.assistant.agent.dto.AgentActionRejectRequest;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.vo.AgentActionExecutionVO;
import org.springframework.stereotype.Service;

@Service
public class AgentActionExecutionDispatcher {

  private final AgentActionMapper actionMapper;
  private final AgentActionConfirmationService collectionTaskConfirmationService;
  private final AgentDigitalArchivePublicationService archivePublicationService;

  public AgentActionExecutionDispatcher(
      AgentActionMapper actionMapper,
      AgentActionConfirmationService collectionTaskConfirmationService,
      AgentDigitalArchivePublicationService archivePublicationService) {
    this.actionMapper = actionMapper;
    this.collectionTaskConfirmationService = collectionTaskConfirmationService;
    this.archivePublicationService = archivePublicationService;
  }

  public AgentActionExecutionVO confirm(Long actionId, AgentActionConfirmRequest request) {
    return AgentDigitalArchiveService.ENABLE_PUBLIC_TRACE.equals(actionType(actionId))
        ? archivePublicationService.confirm(actionId)
        : collectionTaskConfirmationService.confirm(actionId, request);
  }

  public AgentActionExecutionVO reject(Long actionId, AgentActionRejectRequest request) {
    return AgentDigitalArchiveService.ENABLE_PUBLIC_TRACE.equals(actionType(actionId))
        ? archivePublicationService.reject(actionId, request)
        : collectionTaskConfirmationService.reject(actionId, request);
  }

  private String actionType(Long actionId) {
    AgentActionEntity action = actionMapper.selectById(actionId);
    if (action == null) throw new BusinessException("待确认动作不存在");
    return action.getActionType();
  }
}
