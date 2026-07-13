package com.bdis.modules.assistant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.config.HerbAssistantProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ArkResponsesClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chatExtractsOutputText() throws Exception {
        startServer(200, "{\"output_text\":\"hello\"}");
        HerbAssistantProperties properties = properties();

        String answer = ArkResponsesClient.chat(properties, "system", "user");

        assertThat(answer).isEqualTo("hello");
    }

    @Test
    void chatIncludesArkErrorMessage() throws Exception {
        startServer(400, "{\"error\":{\"message\":\"bad model\"}}");
        HerbAssistantProperties properties = properties();

        assertThatThrownBy(() -> ArkResponsesClient.chat(properties, "system", "user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bad model");
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/api/v3/responses",
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

    private HerbAssistantProperties properties() {
        HerbAssistantProperties properties = new HerbAssistantProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v3");
        properties.setModel("test-model");
        properties.setApiKey("test-key");
        properties.setTimeoutSeconds(5);
        return properties;
    }
}
