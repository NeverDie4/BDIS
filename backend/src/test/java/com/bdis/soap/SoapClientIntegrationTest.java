package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.SoapExchangeException;
import com.bdis.soap.campus.CampusGrowthMockService;
import com.bdis.soap.campus.CampusGrowthQueryRequest;
import com.bdis.soap.campus.CampusGrowthQueryResponse;
import com.bdis.soap.component.SoapClient;
import com.bdis.soap.component.SoapXmlParser;
import com.bdis.soap.config.SoapIntegrationProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SoapClientIntegrationTest {

    @Test
    void invokesLocalSoapEndpointAndReturnsNamespacedResponse() throws Exception {
        AtomicReference<String> receivedRequest = new AtomicReference<>();
        HttpServer server = startServer(receivedRequest);
        try {
            SoapClient client = new SoapClient(properties(server.getAddress().getPort()));
            String request =
                    client.createGrowthQueryRequest(
                            "BDIS-SOAP-DEMO-001", LocalDateTime.of(2026, 7, 15, 0, 0));

            String response = client.invoke(request);
            Map<String, Object> parsed = new SoapXmlParser().parseGrowthRecord(response);

            assertThat(request).contains("queryGrowthRecordsRequest", "BDIS-SOAP-DEMO-001");
            assertThat(receivedRequest.get())
                    .contains("queryGrowthRecordsRequest", "BDIS-SOAP-DEMO-001");
            assertThat(response).contains("queryGrowthRecordsResponse");
            assertThat(parsed)
                    .containsEntry("code", "SUCCESS")
                    .containsEntry("externalNo", "CAMPUS-DEMO-GROWTH-001")
                    .containsEntry("herbName", "SOAP演示黄连");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void recordsSoapFaultAsClientFailure() throws Exception {
        HttpServer server = startServer(new AtomicReference<>());
        try {
            SoapClient client = new SoapClient(properties(server.getAddress().getPort()));
            String request = client.createGrowthQueryRequest("SOAP-FAULT-001", null);

            assertThatThrownBy(() -> client.invoke(request))
                    .isInstanceOf(SoapExchangeException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mockCampusServiceProvidesContractedRecord() {
        CampusGrowthQueryRequest request = new CampusGrowthQueryRequest();
        request.setRequestId("BDIS-SOAP-DEMO-001");

        CampusGrowthQueryResponse response =
                new CampusGrowthMockService().queryGrowthRecords(request);

        assertThat(response.getCode()).isEqualTo("SUCCESS");
        assertThat(response.getRecords())
                .singleElement()
                .satisfies(
                        record -> {
                            assertThat(record.getExternalNo()).isEqualTo("CAMPUS-DEMO-GROWTH-001");
                            assertThat(record.getHerbName()).isEqualTo("SOAP演示黄连");
                            assertThat(record.getBaseName()).isEqualTo("SOAP演示重庆基地");
                        });
    }

    private SoapIntegrationProperties properties(int port) {
        SoapIntegrationProperties properties = new SoapIntegrationProperties();
        properties.setMode("mock");
        properties.setCampusEndpoint("http://127.0.0.1:" + port + "/campus-growth");
        return properties;
    }

    private HttpServer startServer(AtomicReference<String> receivedRequest) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/campus-growth",
                exchange -> {
                    String request =
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);
                    receivedRequest.set(request);
                    boolean fault = request.contains("SOAP-FAULT-");
                    byte[] response =
                            (fault ? SOAP_FAULT_RESPONSE : SOAP_SUCCESS_RESPONSE)
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=UTF-8");
                    exchange.sendResponseHeaders(fault ? 500 : 200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
        server.start();
        return server;
    }

    private static final String SOAP_SUCCESS_RESPONSE =
            """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:camp="https://bdis.local/ws/campus-growth/v1">
              <soapenv:Body><camp:queryGrowthRecordsResponse>
                <camp:code>SUCCESS</camp:code><camp:message>ok</camp:message>
                <camp:records><camp:record>
                  <camp:externalNo>CAMPUS-DEMO-GROWTH-001</camp:externalNo>
                  <camp:herbName>SOAP演示黄连</camp:herbName>
                  <camp:baseName>SOAP演示重庆基地</camp:baseName>
                </camp:record></camp:records>
              </camp:queryGrowthRecordsResponse></soapenv:Body>
            </soapenv:Envelope>
            """;

    private static final String SOAP_FAULT_RESPONSE =
            """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
              <soapenv:Body><soapenv:Fault>
                <faultcode>soapenv:Server</faultcode>
                <faultstring>本地模拟校内 SOAP 服务故障</faultstring>
              </soapenv:Fault></soapenv:Body>
            </soapenv:Envelope>
            """;
}
