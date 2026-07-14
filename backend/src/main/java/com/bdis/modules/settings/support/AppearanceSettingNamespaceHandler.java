package com.bdis.modules.settings.support;

import com.bdis.common.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AppearanceSettingNamespaceHandler extends AbstractSettingNamespaceHandler {

    private static final Set<String> FIELDS =
            Set.of("contentDensity", "sidebarMode", "reduceMotion");

    private final ObjectMapper objectMapper;

    public AppearanceSettingNamespaceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String namespace() {
        return "appearance";
    }

    @Override
    public boolean supports(CurrentUser user) {
        return true;
    }

    @Override
    public JsonNode defaultValues(CurrentUser user) {
        ObjectNode values = objectMapper.createObjectNode();
        values.put("contentDensity", "comfortable");
        values.put("sidebarMode", "auto");
        values.put("reduceMotion", false);
        return values;
    }

    @Override
    public JsonNode validateAndNormalize(JsonNode values, CurrentUser user) {
        requireObjectWithFields(values, FIELDS);
        String density = textValue(values, "contentDensity", "comfortable");
        String sidebarMode = textValue(values, "sidebarMode", "auto");
        requireOneOf("contentDensity", density, Set.of("comfortable", "compact"));
        requireOneOf("sidebarMode", sidebarMode, Set.of("auto", "expanded", "collapsed"));
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("contentDensity", density);
        normalized.put("sidebarMode", sidebarMode);
        normalized.put("reduceMotion", booleanValue(values, "reduceMotion", false));
        return normalized;
    }
}
