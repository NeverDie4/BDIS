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
    void parseGrowthRecordShouldRejectDoctype() {
        String xml =
                """
                <!DOCTYPE Envelope [<!ENTITY external SYSTEM "file:///etc/passwd">]>
                <Envelope><Body><externalNo>&external;</externalNo></Body></Envelope>
                """;

        assertThrows(SoapExchangeException.class, () -> soapXmlParser.parseGrowthRecord(xml));
    }
}
