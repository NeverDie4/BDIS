package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.dto.AgentReanalysisExplanation;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentReanalysisExplanationService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AgentReanalysisExplanationService.class);
  private static final List<String> FORBIDDEN =
      List.of("已确认由", "确定由", "必然导致", "已确诊", "审核通过", "自动批准");
  private static final String SYSTEM_PROMPT =
      """
      你是本草数字孪生科研 Agent 的补采变化说明模块。
      只能基于输入的补采前后事实生成变化总结、可以确认的事实、尚不能确认的结论和下一步建议。
      不得决定审核是否通过，不得编造数值，不得输出确定因果、疾病诊断或业务写操作。
      必须明确同期变化不能证明因果。按 AgentReanalysisExplanation 结构返回 JSON。
      """;

  private final ObjectProvider<ChatClient> chatClientProvider;
  private final HerbAssistantProperties properties;
  private final ObjectMapper objectMapper;

  public AgentReanalysisExplanationService(
      @Qualifier("herbAssistantChatClient") ObjectProvider<ChatClient> chatClientProvider,
      HerbAssistantProperties properties,
      ObjectMapper objectMapper) {
    this.chatClientProvider = chatClientProvider;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public AgentReanalysisExplanation explain(
      AgentReanalysisSnapshot baseline,
      AgentReanalysisSnapshot current,
      String outcome,
      AgentReanalysisExplanation fallback) {
    if (!properties.isEnabled() || properties.isMockEnabled()) {
      return fallback;
    }
    ChatClient client = chatClientProvider.getIfAvailable();
    if (client == null) {
      return fallback;
    }
    try {
      AgentReanalysisExplanation candidate =
          client
              .prompt()
              .system(SYSTEM_PROMPT)
              .user(prompt(baseline, current, outcome))
              .call()
              .entity(AgentReanalysisExplanation.class);
      return valid(candidate) ? candidate : fallback;
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Agent reanalysis explanation degraded to rules, outcome={}, error={}",
          outcome,
          exception.getClass().getSimpleName());
      return fallback;
    }
  }

  private String prompt(
      AgentReanalysisSnapshot baseline, AgentReanalysisSnapshot current, String outcome) {
    try {
      return objectMapper.writeValueAsString(
          java.util.Map.of("baseline", baseline, "current", current, "ruleOutcome", outcome));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Agent reanalysis prompt serialization failed", exception);
    }
  }

  private boolean valid(AgentReanalysisExplanation value) {
    if (value == null
        || !StringUtils.hasText(value.changeSummary())
        || !StringUtils.hasText(value.nextSuggestion())
        || value.uncertainties() == null
        || value.uncertainties().isEmpty()) {
      return false;
    }
    String text = objectText(value);
    return FORBIDDEN.stream().noneMatch(text::contains)
        && (text.contains("不能") || text.contains("不足"));
  }

  private String objectText(AgentReanalysisExplanation value) {
    return value.changeSummary()
        + String.join("", value.confirmedFacts() == null ? List.of() : value.confirmedFacts())
        + String.join("", value.uncertainties())
        + value.nextSuggestion();
  }
}
