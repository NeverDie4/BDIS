package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceExplanation;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

class AgentEvidenceExplanationServiceTest {

    @Test
    void unknownNumbersAndCertainCausalStatementsAreDiscarded() {
        AgentEvidenceAnalysisVO analysis = analysis();
        AgentEvidenceExplanation candidate =
                new AgentEvidenceExplanation(
                        "已确认由缺水导致株高下降 99%。",
                        List.of("本次包含 2 个阶段。", "虚构数值为 99。"),
                        List.of("已确认由缺水导致变化。"),
                        List.of("现有证据不足以证明因果关系。"),
                        List.of(),
                        List.of("建议补充第 2 阶段证据。"),
                        "MEDIUM");
        AgentEvidenceExplanationService service = service(false, null);

        AgentEvidenceExplanation result = service.validate(analysis, candidate);

        assertThat(result.conclusion()).isEqualTo(analysis.explanation().conclusion());
        assertThat(result.confirmedFacts()).containsExactly("本次包含 2 个阶段。");
        assertThat(result.possibleAssociations()).isEmpty();
        assertThat(result.toString()).doesNotContain("99", "已确认由");
    }

    @Test
    void modelFailureFallsBackToRuleExplanation() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt()
                        .system(anyString())
                        .user(anyString())
                        .call()
                        .entity(AgentEvidenceExplanation.class))
                .thenThrow(new IllegalStateException("model unavailable"));
        AgentEvidenceExplanationService service = service(true, client);

        AgentEvidenceExplanation result = service.explain(analysis());

        assertThat(result).isEqualTo(analysis().explanation());
    }

    @SuppressWarnings("unchecked")
    private AgentEvidenceExplanationService service(boolean enabledModel, ChatClient client) {
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        HerbAssistantProperties properties = new HerbAssistantProperties();
        properties.setEnabled(enabledModel);
        properties.setMockEnabled(false);
        return new AgentEvidenceExplanationService(
                provider, properties, new ObjectMapper().findAndRegisterModules());
    }

    private AgentEvidenceAnalysisVO analysis() {
        AgentEvidenceExplanation fallback =
                new AgentEvidenceExplanation(
                        "规则分析完成，不能形成确定因果结论。",
                        List.of("本次包含 2 个阶段。"),
                        List.of(),
                        List.of("现有证据不足以证明因果关系。"),
                        List.of(),
                        List.of("建议补充第 2 阶段证据。"),
                        "LOW");
        return new AgentEvidenceAnalysisVO(
                1L,
                12L,
                2,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                fallback,
                LocalDateTime.of(2026, 7, 16, 10, 0));
    }
}
