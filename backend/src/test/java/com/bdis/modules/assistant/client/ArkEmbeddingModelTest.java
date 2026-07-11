package com.bdis.modules.assistant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingRequest;

class ArkEmbeddingModelTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callParsesEmbeddingResponse() throws Exception {
        startServer(200, "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}");
        ArkEmbeddingModel model = new ArkEmbeddingModel(properties());

        var response = model.call(new EmbeddingRequest(List.of("hello"), null));

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOutput()).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void callRejectsMissingConfig() {
        HerbAssistantRagProperties properties = properties();
        properties.setEmbeddingApiKey("");
        ArkEmbeddingModel model = new ArkEmbeddingModel(properties);

        assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("hello"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("API Key");
    }

    @Test
    void callWrapsHttpError() throws Exception {
        startServer(400, "{\"error\":{\"message\":\"bad request\"}}");
        ArkEmbeddingModel model = new ArkEmbeddingModel(properties());

        assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("hello"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("400");
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/api/v3/embeddings",
                exchange -> {
                    consumeRequest(exchange);
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
    }

    private void consumeRequest(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
    }

    private HerbAssistantRagProperties properties() {
        HerbAssistantRagProperties properties = new HerbAssistantRagProperties();
        properties.setEmbeddingBaseUrl(
                server == null
                        ? "http://127.0.0.1:1/api/v3"
                        : "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v3");
        properties.setEmbeddingModel("embedding-model");
        properties.setEmbeddingApiKey("test-key");
        return properties;
    }
}
