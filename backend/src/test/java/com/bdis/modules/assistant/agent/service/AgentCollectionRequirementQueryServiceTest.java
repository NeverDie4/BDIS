package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import com.bdis.modules.assistant.agent.entity.AgentCollectionRequirementEntity;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionPlanMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionRequirementMapper;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.mobile.vo.MobileAgentRequirementsVO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AgentCollectionRequirementQueryServiceTest {

    @Mock private AgentCollectionRequirementMapper requirementMapper;
    @Mock private AgentCollectionPlanMapper planMapper;
    @Mock private HerbCollectionTaskService collectionTaskService;

    @Test
    void authorizedCollectorReceivesAgentChecklistWithoutInternalPrompt() {
        AgentCollectionRequirementQueryService service = service();
        AgentCollectionPlanEntity plan = new AgentCollectionPlanEntity();
        plan.setId(100L);
        plan.setObjective("验证土壤湿度变化是否持续");
        when(requirementMapper.selectByTaskId(200L))
                .thenReturn(
                        List.of(
                                requirement("METRIC", "soilMoisture", "土壤湿度", "真实测量", 1),
                                requirement("IMAGE", "root", "根部", "自然光拍摄", 2),
                                requirement("NOTE", "completion_1", "完成条件", "完成必填项", 3)));
        when(planMapper.selectById(100L)).thenReturn(plan);

        MobileAgentRequirementsVO result = service.getForMobile(200L);

        assertThat(result.agentGenerated()).isTrue();
        assertThat(result.requiredMetrics()).extracting("code").containsExactly("soilMoisture");
        assertThat(result.requiredImages()).extracting("code").containsExactly("root");
        assertThat(result.completionCriteria()).containsExactly("完成必填项");
        assertThat(result.toString()).doesNotContain("prompt", "modelName");
    }

    @Test
    void taskAccessDenialPreventsChecklistQuery() {
        AgentCollectionRequirementQueryService service = service();
        when(collectionTaskService.getById(200L)).thenThrow(new ForbiddenException("无权查看任务"));

        assertThatThrownBy(() -> service.getForMobile(200L)).isInstanceOf(ForbiddenException.class);
    }

    private AgentCollectionRequirementQueryService service() {
        return new AgentCollectionRequirementQueryService(
                requirementMapper, planMapper, collectionTaskService);
    }

    private AgentCollectionRequirementEntity requirement(
            String type, String code, String name, String guidance, int order) {
        AgentCollectionRequirementEntity item = new AgentCollectionRequirementEntity();
        item.setCollectionPlanId(100L);
        item.setCollectionTaskId(200L);
        item.setRequirementType(type);
        item.setRequirementCode(code);
        item.setRequirementName(name);
        item.setRequired(1);
        item.setMinCount("IMAGE".equals(type) ? 1 : null);
        item.setGuidance(guidance);
        item.setSortOrder(order);
        return item;
    }
}
