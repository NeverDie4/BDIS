package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bdis.common.exception.SoapExchangeException;
import com.bdis.soap.component.SoapXmlParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoapXmlParserTest {

    private final SoapXmlParser soapXmlParser = new SoapXmlParser();

    @Test
    void parseGrowthRecordShouldReturnMappedFields() {
        String xml =
                """
                <Envelope>
                  <Body>
                    <GrowthRecord>
                      <externalNo>SOAP-001</externalNo>
                      <herbName>黄连</herbName>
                      <baseName>重庆示范基地</baseName>
                      <collectorName>collector-a</collectorName>
                      <collectedAt>2026-07-08T10:00:00</collectedAt>
                      <sourceType>SOAP</sourceType>
                    </GrowthRecord>
                  </Body>
                </Envelope>
                """;

        Map<String, Object> result = soapXmlParser.parseGrowthRecord(xml);

        assertThat(result)
                .containsEntry("externalNo", "SOAP-001")
                .containsEntry("herbName", "黄连")
                .containsEntry("baseName", "重庆示范基地")
                .containsEntry("collectorName", "collector-a")
                .containsEntry("sourceType", "SOAP");
    }

    @Test
    void parseGrowthRecordShouldRejectInvalidXml() {
        assertThrows(
                SoapExchangeException.class,
                () -> soapXmlParser.parseGrowthRecord("<Envelope><Body>"));
    }

    @Test
    void parseGrowthRecordShouldSupportNamespacedSoapResponse() {
        String xml =
                """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:camp="https://bdis.local/ws/campus-growth/v1">
                  <soapenv:Body>
                    <camp:queryGrowthRecordsResponse>
                      <camp:code>SUCCESS</camp:code>
                      <camp:message>ok</camp:message>
                      <camp:records><camp:record>
                        <camp:externalNo>CAMPUS-001</camp:externalNo>
                        <camp:herbName>SOAP演示黄连</camp:herbName>
                        <camp:baseName>SOAP演示重庆基地</camp:baseName>
                      </camp:record></camp:records>
                    </camp:queryGrowthRecordsResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        Map<String, Object> result = soapXmlParser.parseGrowthRecord(xml);

        assertThat(result)
                .containsEntry("code", "SUCCESS")
                .containsEntry("message", "ok")
                .containsEntry("externalNo", "CAMPUS-001")
                .containsEntry("herbName", "SOAP演示黄连");
    }

    @Test
    void parseGrowthRecordShouldRejectDoctype() {
        String xml =
                """
                <!DOCTYPE Envelope [<!ENTITY external SYSTEM "file:///etc/passwd">]>
                <Envelope><Body><externalNo>&external;</externalNo></Body></Envelope>
                """;

        assertThrows(SoapExchangeException.class, () -> soapXmlParser.parseGrowthRecord(xml));
    }
}
