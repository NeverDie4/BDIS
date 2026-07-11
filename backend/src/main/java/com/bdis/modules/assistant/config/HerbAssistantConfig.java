package com.bdis.modules.assistant.config;

import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HerbAssistantConfig {

    @Bean
    @Lazy
    public ChatClient herbAssistantChatClient(HerbAssistantProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        OpenAiApi openAiApi =
                OpenAiApi.builder()
                        .baseUrl(properties.getBaseUrl())
                        .apiKey(properties.getApiKey())
                        .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                        .build();
        OpenAiChatModel chatModel =
                OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(
                                OpenAiChatOptions.builder().model(properties.getModel()).build())
                        .build();
        return ChatClient.builder(chatModel).build();
    }
}
