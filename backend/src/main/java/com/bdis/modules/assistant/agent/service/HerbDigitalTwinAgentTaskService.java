package com.bdis.modules.assistant.agent.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentTaskCancelRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskCreateRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskQueryRequest;
import com.bdis.modules.assistant.agent.vo.AgentActionVO;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.bdis.modules.assistant.agent.vo.AgentStepVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;

import java.util.List;

public interface HerbDigitalTwinAgentTaskService {

    AgentTaskSummaryVO create(AgentTaskCreateRequest request);

    AgentTaskDetailVO getDetail(Long agentTaskId);

    PageResult<AgentTaskSummaryVO> page(AgentTaskQueryRequest request);

    List<AgentStepVO> listSteps(Long agentTaskId);

    List<AgentFindingVO> listFindings(Long agentTaskId);

    List<AgentActionVO> listPendingActions(Long agentTaskId);

    AgentTaskDetailVO cancel(Long agentTaskId, AgentTaskCancelRequest request);

    AgentTaskSummaryVO updateProgress(
            Long agentTaskId, Integer progressPercent, String currentPhase);

    AgentTaskSummaryVO transition(Long agentTaskId, AgentTaskStatus targetStatus);
}
