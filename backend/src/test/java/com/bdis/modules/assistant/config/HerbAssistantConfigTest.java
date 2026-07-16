package com.bdis.modules.assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HerbAssistantConfigTest {

    @Test
    void arkCompatibleChatClientUsesChatCompletionsPathWithoutOpenAiV1Prefix()
            throws Exception {
        String source =
                Files.readString(
                        Path.of(
                                "src/main/java/com/bdis/modules/assistant/config/HerbAssistantConfig.java"),
                        StandardCharsets.UTF_8);

        assertThat(source).contains(".completionsPath(\"/chat/completions\")");
    }
}
