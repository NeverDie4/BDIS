package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.dto.AgentReanalysisExplanation;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AgentReanalysisExplanationServiceTest {

  @Mock private ObjectProvider<ChatClient> chatClientProvider;
  @Mock private ChatClient chatClient;

  @Test
  void modelFailureFallsBackToRuleExplanationWithCausalityLimit() {
    HerbAssistantProperties properties = new HerbAssistantProperties();
    properties.setEnabled(true);
    properties.setMockEnabled(false);
    when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
    when(chatClient.prompt()).thenThrow(new IllegalStateException("model unavailable"));
    AgentReanalysisExplanation fallback =
        new AgentReanalysisExplanation(
            "完整度提升", List.of("新增根部图片"), List.of("同期变化不能证明因果关系"), "继续按规则处理");
    AgentReanalysisExplanationService service =
        new AgentReanalysisExplanationService(chatClientProvider, properties, new ObjectMapper());
    AgentReanalysisSnapshot snapshot =
        new AgentReanalysisSnapshot(List.of(), List.of(), 0, "NOT_READY");

    AgentReanalysisExplanation result =
        service.explain(snapshot, snapshot, "NEED_MORE_FIELD_DATA", fallback);

    assertThat(result).isSameAs(fallback);
    assertThat(result.uncertainties()).contains("同期变化不能证明因果关系");
  }
}
