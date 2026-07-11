package com.bdis.modules.assistant.client;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class ArkResponsesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArkResponsesClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ArkResponsesClient() {}

    public static String chat(HerbAssistantProperties properties, String system, String user) {
        String endpoint = resolveResponsesUrl(properties.getBaseUrl());
        try {
            RestClient restClient =
                    RestClient.builder()
                            .requestFactory(requestFactory(properties))
                            .defaultHeader(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + properties.getApiKey().trim())
                            .defaultHeader(
                                    HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();

            String body =
                    restClient
                            .post()
                            .uri(endpoint)
                            .body(buildRequest(properties.getModel(), system, user))
                            .retrieve()
                            .body(String.class);
            String answer = extractAnswer(body);
            if (!StringUtils.hasText(answer)) {
                throw new BusinessException("AI 模型返回内容为空");
            }
            return answer.trim();
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            LOGGER.warn(
                    "Ark Responses network error, endpoint={}, model={}, message={}",
                    endpoint,
                    properties.getModel(),
                    exception.getMessage());
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new BusinessException("AI 模型调用超时");
            }
            throw new BusinessException("AI 模型网络连接失败");
        } catch (HttpStatusCodeException exception) {
            LOGGER.warn(
                    "Ark Responses HTTP error, endpoint={}, model={}, status={}, error={}",
                    endpoint,
                    properties.getModel(),
                    exception.getStatusCode().value(),
                    extractErrorMessage(exception.getResponseBodyAsString()));
            throw new BusinessException(buildStatusMessage(exception));
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Ark Responses client error, endpoint={}, model={}, message={}",
                    endpoint,
                    properties.getModel(),
                    exception.getMessage());
            throw new BusinessException("AI 模型调用失败，请稍后重试");
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory(
            HerbAssistantProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }

    private static Map<String, Object> buildRequest(String model, String system, String user) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        List<Map<String, Object>> input = new ArrayList<>();
        if (StringUtils.hasText(system)) {
            input.add(message("system", system));
        }
        input.add(message("user", user));
        request.put("input", input);
        return request;
    }

    private static Map<String, Object> message(String role, String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", text);
        return message;
    }

    private static String resolveResponsesUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/responses")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + "responses";
        }
        return normalized + "/responses";
    }

    private static String extractAnswer(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            String outputText = textValue(root.path("output_text"));
            if (StringUtils.hasText(outputText)) {
                return outputText;
            }
            StringBuilder builder = new StringBuilder();
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    appendContentText(builder, item.path("content"));
                }
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
            return textValue(root.path("choices").path(0).path("message").path("content"));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("AI 模型返回内容解析失败");
        } catch (RuntimeException exception) {
            throw new BusinessException("AI 模型返回内容解析失败");
        }
    }

    private static void appendContentText(StringBuilder builder, JsonNode content) {
        if (!content.isArray()) {
            String text = textValue(content.path("text"));
            if (StringUtils.hasText(text)) {
                builder.append(text);
            }
            return;
        }
        for (JsonNode node : content) {
            String text = textValue(node.path("text"));
            if (!StringUtils.hasText(text)) {
                text = textValue(node.path("content"));
            }
            if (StringUtils.hasText(text)) {
                builder.append(text);
            }
        }
    }

    private static String textValue(JsonNode node) {
        return node.isTextual() ? node.asText() : null;
    }

    private static String buildStatusMessage(HttpStatusCodeException exception) {
        String detail = extractErrorMessage(exception.getResponseBodyAsString());
        if (StringUtils.hasText(detail)) {
            return "AI 模型调用失败：" + detail;
        }
        return "AI 模型调用失败，状态码：" + exception.getStatusCode().value();
    }

    private static String extractErrorMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            String message = textValue(root.path("error").path("message"));
            if (StringUtils.hasText(message)) {
                return message;
            }
            return textValue(root.path("message"));
        } catch (JsonProcessingException exception) {
            return null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
        Throwable current = exception;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
