package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.bdis.modules.growth.service.impl.DigitalLifeNarrationGeneratorImpl;

import org.junit.jupiter.api.Test;

class DigitalLifeNarrationGeneratorImplTest {

    @Test
    void disabledModelUsesTemplateWithoutExternalCall() {
        HerbAssistantProperties properties = new HerbAssistantProperties();
        properties.setEnabled(false);
        properties.setMockEnabled(false);
        DigitalLifeNarrationGeneratorImpl generator =
                new DigitalLifeNarrationGeneratorImpl(properties);

        DigitalLifeNarrationGenerator.GeneratedNarration narration =
                generator.generate("真实阶段输入", "规则摘要");

        assertThat(narration.text()).isEqualTo("规则摘要");
        assertThat(narration.source()).isEqualTo("template");
        assertThat(narration.modelName()).isNull();
    }

    @Test
    void mockConfigurationAlsoUsesTemplate() {
        HerbAssistantProperties properties = new HerbAssistantProperties();
        properties.setEnabled(true);
        properties.setMockEnabled(true);
        DigitalLifeNarrationGeneratorImpl generator =
                new DigitalLifeNarrationGeneratorImpl(properties);

        DigitalLifeNarrationGenerator.GeneratedNarration narration =
                generator.generate("真实阶段输入", "规则摘要");

        assertThat(narration.source()).isEqualTo("template");
    }
}
