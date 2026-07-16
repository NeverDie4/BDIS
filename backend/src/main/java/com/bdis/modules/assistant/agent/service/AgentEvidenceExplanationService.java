package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceExplanation;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentEvidenceExplanationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AgentEvidenceExplanationService.class);
    private static final Pattern NUMBER = Pattern.compile("(?<![\\p{L}\\d])[-+]?\\d+(?:\\.\\d+)?");
    private static final Set<String> CONFIDENCE_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final List<String> FORBIDDEN_CERTAINTY =
            List.of("已确认由", "已确诊", "一定会减产", "药材已经失效", "必然导致", "确定由");
    private static final String SYSTEM_PROMPT =
            """
            你是本草数字孪生科研 Agent 的证据说明模块。
            只能根据输入 JSON 中的事实组织说明，不得编造数值，不得输出疾病诊断、确定因果、审核结论或业务写操作。
            时间上同时出现只能描述为统计关联，并明确证据不足以证明因果关系。
            按 AgentEvidenceExplanation 结构返回 JSON，confidenceLevel 只能是 LOW、MEDIUM、HIGH。
            """;

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final HerbAssistantProperties properties;
    private final ObjectMapper objectMapper;

    public AgentEvidenceExplanationService(
            @Qualifier("herbAssistantChatClient") ObjectProvider<ChatClient> chatClientProvider,
            HerbAssistantProperties properties,
            ObjectMapper objectMapper) {
        this.chatClientProvider = chatClientProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AgentEvidenceExplanation explain(AgentEvidenceAnalysisVO analysis) {
        AgentEvidenceExplanation fallback = analysis.explanation();
        if (!properties.isEnabled() || properties.isMockEnabled()) {
            return fallback;
        }
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            return fallback;
        }
        try {
            AgentEvidenceExplanation candidate =
                    chatClient
                            .prompt()
                            .system(SYSTEM_PROMPT)
                            .user(buildPrompt(analysis))
                            .call()
                            .entity(AgentEvidenceExplanation.class);
            return validate(analysis, candidate);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Agent evidence explanation degraded to rules, taskId={}, error={}",
                    analysis.agentTaskId(),
                    exception.getClass().getSimpleName());
            return fallback;
        }
    }

    AgentEvidenceExplanation validate(
            AgentEvidenceAnalysisVO analysis, AgentEvidenceExplanation candidate) {
        AgentEvidenceExplanation fallback = analysis.explanation();
        if (candidate == null || !CONFIDENCE_LEVELS.contains(candidate.confidenceLevel())) {
            return fallback;
        }
        Set<String> knownNumbers = knownNumbers(analysis);
        String conclusion =
                validSentence(candidate.conclusion(), knownNumbers)
                        ? candidate.conclusion()
                        : fallback.conclusion();
        List<String> confirmedFacts = filter(candidate.confirmedFacts(), knownNumbers);
        List<String> possibleAssociations = filter(candidate.possibleAssociations(), knownNumbers);
        List<String> uncertainties = filter(candidate.uncertainties(), knownNumbers);
        List<String> evidenceGaps = filter(candidate.evidenceGaps(), knownNumbers);
        List<String> suggestions = filter(candidate.followUpSuggestions(), knownNumbers);
        return new AgentEvidenceExplanation(
                conclusion,
                confirmedFacts.isEmpty() ? fallback.confirmedFacts() : confirmedFacts,
                possibleAssociations,
                uncertainties.isEmpty() ? fallback.uncertainties() : uncertainties,
                evidenceGaps.isEmpty() ? fallback.evidenceGaps() : evidenceGaps,
                suggestions.isEmpty() ? fallback.followUpSuggestions() : suggestions,
                candidate.confidenceLevel());
    }

    private String buildPrompt(AgentEvidenceAnalysisVO analysis) {
        try {
            return "请基于以下已脱敏结构化证据生成说明：\n" + objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Agent evidence input serialization failed", exception);
        }
    }

    private Set<String> knownNumbers(AgentEvidenceAnalysisVO analysis) {
        String json;
        try {
            json = objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException exception) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        Matcher matcher = NUMBER.matcher(json);
        while (matcher.find()) {
            values.add(normalizeNumber(matcher.group()));
        }
        return Set.copyOf(values);
    }

    private List<String> filter(List<String> sentences, Set<String> knownNumbers) {
        if (sentences == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String sentence : sentences) {
            if (validSentence(sentence, knownNumbers)) {
                result.add(sentence.trim());
            }
        }
        return List.copyOf(result);
    }

    private boolean validSentence(String sentence, Set<String> knownNumbers) {
        if (!StringUtils.hasText(sentence)
                || FORBIDDEN_CERTAINTY.stream().anyMatch(sentence::contains)) {
            return false;
        }
        Matcher matcher = NUMBER.matcher(sentence);
        while (matcher.find()) {
            if (!knownNumbers.contains(normalizeNumber(matcher.group()))) {
                return false;
            }
        }
        return true;
    }

    private String normalizeNumber(String number) {
        try {
            return new BigDecimal(number).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            return number;
        }
    }
}
