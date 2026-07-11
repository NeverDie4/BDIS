package com.bdis.soap.component;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class SoapClient {

    public String mockResponse(String resourceType) {
        String now = LocalDateTime.now().toString();
        return """
                <Envelope>
                  <Body>
                    <GrowthRecord>
                      <externalNo>SOAP-GROWTH-001</externalNo>
                      <herbName>黄连</herbName>
                      <baseName>重庆示范基地</baseName>
                      <collectorName>soap-system</collectorName>
                      <collectedAt>%s</collectedAt>
                      <sourceType>SOAP</sourceType>
                    </GrowthRecord>
                  </Body>
                </Envelope>
                """
                .formatted(now);
    }
}
