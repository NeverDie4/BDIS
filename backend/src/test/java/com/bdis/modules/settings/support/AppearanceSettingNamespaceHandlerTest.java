package com.bdis.modules.settings.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AppearanceSettingNamespaceHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AppearanceSettingNamespaceHandler handler =
            new AppearanceSettingNamespaceHandler(objectMapper);

    @Test
    void normalizesPartialValuesWithDefaults() {
        ObjectNode values = objectMapper.createObjectNode();
        values.put("contentDensity", "compact");

        var normalized = handler.validateAndNormalize(values, currentUser());

        assertThat(normalized.get("contentDensity").asText()).isEqualTo("compact");
        assertThat(normalized.get("sidebarMode").asText()).isEqualTo("auto");
        assertThat(normalized.get("reduceMotion").asBoolean()).isFalse();
    }

    @Test
    void rejectsUnknownFields() {
        ObjectNode values = objectMapper.createObjectNode();
        values.put("theme", "dark");

        assertThatThrownBy(() -> handler.validateAndNormalize(values, currentUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未知设置字段");
    }

    private CurrentUser currentUser() {
        return new CurrentUser(1L, "user", "User", null, null, Set.of(), Set.of(), Set.of());
    }
}
