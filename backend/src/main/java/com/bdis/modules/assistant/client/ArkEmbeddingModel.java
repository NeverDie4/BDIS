package com.bdis.modules.assistant.client;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class ArkEmbeddingModel implements EmbeddingModel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final HerbAssistantRagProperties properties;

    public ArkEmbeddingModel(HerbAssistantRagProperties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        validateConfig();
        List<String> input = request.getInstructions();
        if (input == null || input.isEmpty()) {
            return new EmbeddingResponse(List.of());
        }
        try {
            String body =
                    restClient()
                            .post()
                            .uri(resolveEmbeddingsUrl(properties.getEmbeddingBaseUrl()))
                            .body(buildRequest(input))
                            .retrieve()
                            .body(String.class);
            return new EmbeddingResponse(parseEmbeddings(body));
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(
                    "Embedding 模型调用失败，状态码：" + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new BusinessException("Embedding 模型调用失败，请稍后重试");
        }
    }

    @Override
    public float[] embed(Document document) {
        return embed(document == null ? "" : document.getText());
    }

    private RestClient restClient() {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getEmbeddingApiKey().trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        requestFactory.setReadTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        return requestFactory;
    }

    private Map<String, Object> buildRequest(List<String> input) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getEmbeddingModel());
        request.put("input", input);
        return request;
    }

    private List<Embedding> parseEmbeddings(String body) {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException("Embedding 模型返回内容为空");
        }
        try {
            JsonNode data = OBJECT_MAPPER.readTree(body).path("data");
            if (!data.isArray()) {
                throw new BusinessException("Embedding 模型返回格式无效");
            }
            List<Embedding> embeddings = new ArrayList<>();
            int index = 0;
            for (JsonNode item : data) {
                embeddings.add(new Embedding(toFloatArray(item.path("embedding")), index++));
            }
            return embeddings;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Embedding 模型返回内容解析失败");
        }
    }

    private float[] toFloatArray(JsonNode node) {
        if (!node.isArray()) {
            throw new BusinessException("Embedding 向量格式无效");
        }
        float[] result = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            result[i] = (float) node.get(i).asDouble();
        }
        return result;
    }

    private String resolveEmbeddingsUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/embeddings")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + "embeddings";
        }
        return normalized + "/embeddings";
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getEmbeddingBaseUrl())) {
            throw new BusinessException("Embedding baseUrl 未配置");
        }
        if (!StringUtils.hasText(properties.getEmbeddingModel())) {
            throw new BusinessException("Embedding 模型名称未配置");
        }
        if (!StringUtils.hasText(properties.getEmbeddingApiKey())) {
            throw new BusinessException("Embedding API Key 未配置");
        }
    }
}
