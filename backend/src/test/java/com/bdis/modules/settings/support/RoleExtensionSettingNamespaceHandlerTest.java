package com.bdis.modules.settings.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.common.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleExtensionSettingNamespaceHandlerTest {

    private final RoleExtensionSettingNamespaceHandler handler =
            new RoleExtensionSettingNamespaceHandler("teaching", "TEACHER", new ObjectMapper());

    @Test
    void onlySupportsMatchingRole() {
        assertThat(handler.supports(user(Set.of("TEACHER")))).isTrue();
        assertThat(handler.supports(user(Set.of("STUDENT")))).isFalse();
    }

    private CurrentUser user(Set<String> roles) {
        return new CurrentUser(1L, "user", "User", null, null, roles, Set.of(), Set.of());
    }
}
