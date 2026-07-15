package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.soap.config.SoapIntegrationConfiguration;
import com.bdis.soap.config.SoapIntegrationProperties;
import jakarta.xml.ws.Endpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SoapIntegrationConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(SoapIntegrationConfiguration.class);

    @Test
    void doesNotPublishMockEndpointUnlessMockModeIsExplicitlyEnabled() {
        contextRunner.run(
                context -> {
                    assertThat(context.getBeansOfType(Endpoint.class)).isEmpty();
                    assertThat(context.getBean(SoapIntegrationProperties.class).getMode())
                            .isEqualTo("disabled");
                });
    }
}
