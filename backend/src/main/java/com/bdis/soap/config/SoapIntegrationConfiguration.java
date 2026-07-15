package com.bdis.soap.config;

import com.bdis.soap.campus.CampusGrowthMockService;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SoapIntegrationProperties.class)
public class SoapIntegrationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "bdis.soap", name = "mode", havingValue = "mock")
    public Endpoint campusGrowthMockEndpoint(
            Bus bus, CampusGrowthMockService campusGrowthMockService) {
        EndpointImpl endpoint = new EndpointImpl(bus, campusGrowthMockService);
        endpoint.publish("/campus-growth");
        return endpoint;
    }
}
