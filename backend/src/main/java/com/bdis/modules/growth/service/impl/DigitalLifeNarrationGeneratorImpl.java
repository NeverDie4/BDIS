package com.bdis.modules.growth.service.impl;

import com.bdis.modules.assistant.client.ArkResponsesClient;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.bdis.modules.growth.service.DigitalLifeNarrationGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DigitalLifeNarrationGeneratorImpl implements DigitalLifeNarrationGenerator {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DigitalLifeNarrationGeneratorImpl.class);

    private static final String SYSTEM_PROMPT =
            "你是中药材连续观测档案的阶段解说助手。请严格依据输入数据写一段80到160字的中文说明。"
                    + "不得补充输入中不存在的指标、地点或结论，不得生成医疗功效结论，不得判断数据绝对正常。"
                    + "只输出解说正文，不使用Markdown。";

    private final HerbAssistantProperties properties;

    public DigitalLifeNarrationGeneratorImpl(HerbAssistantProperties properties) {
        this.properties = properties;
    }

    @Override
    public GeneratedNarration generate(String stageInput, String templateNarration) {
        if (!canUseModel()) {
            return template(templateNarration);
        }
        try {
            String generated = ArkResponsesClient.chat(properties, SYSTEM_PROMPT, stageInput);
            String normalized = normalize(generated);
            if (normalized.length() < 80 || normalized.length() > 160) {
                LOGGER.warn("Digital life narration length out of range: {}", normalized.length());
                return template(templateNarration);
            }
            return new GeneratedNarration(normalized, "ai", properties.getModel());
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Digital life narration falls back to template: {}", exception.getMessage());
            return template(templateNarration);
        }
    }

    private boolean canUseModel() {
        return properties.isEnabled()
                && !properties.isMockEnabled()
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    private GeneratedNarration template(String narration) {
        return new GeneratedNarration(narration, "template", null);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("^```(?:text)?\\s*|\\s*```$", "").trim();
    }
}
