package com.bdis.modules.settings.support;

import com.bdis.common.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;

public class RoleExtensionSettingNamespaceHandler extends AbstractSettingNamespaceHandler {

    private final String namespace;

    private final String roleCode;

    private final ObjectMapper objectMapper;

    public RoleExtensionSettingNamespaceHandler(
            String namespace, String roleCode, ObjectMapper objectMapper) {
        this.namespace = namespace;
        this.roleCode = roleCode;
        this.objectMapper = objectMapper;
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public boolean supports(CurrentUser user) {
        return user.getRoleCodes().contains(roleCode);
    }

    @Override
    public JsonNode defaultValues(CurrentUser user) {
        return objectMapper.createObjectNode();
    }

    @Override
    public JsonNode validateAndNormalize(JsonNode values, CurrentUser user) {
        requireObjectWithFields(values, Set.of());
        return objectMapper.createObjectNode();
    }
}
