package com.bdis.modules.settings.config;

import com.bdis.modules.settings.support.RoleExtensionSettingNamespaceHandler;
import com.bdis.modules.settings.support.SettingNamespaceHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleSettingNamespaceConfig {

    @Bean
    public SettingNamespaceHandler teachingSettingNamespaceHandler(ObjectMapper objectMapper) {
        return new RoleExtensionSettingNamespaceHandler("teaching", "TEACHER", objectMapper);
    }

    @Bean
    public SettingNamespaceHandler learningSettingNamespaceHandler(ObjectMapper objectMapper) {
        return new RoleExtensionSettingNamespaceHandler("learning", "STUDENT", objectMapper);
    }

    @Bean
    public SettingNamespaceHandler collectionSettingNamespaceHandler(ObjectMapper objectMapper) {
        return new RoleExtensionSettingNamespaceHandler("collection", "COLLECTOR", objectMapper);
    }

    @Bean
    public SettingNamespaceHandler reviewSettingNamespaceHandler(ObjectMapper objectMapper) {
        return new RoleExtensionSettingNamespaceHandler("review", "REVIEWER", objectMapper);
    }
}
