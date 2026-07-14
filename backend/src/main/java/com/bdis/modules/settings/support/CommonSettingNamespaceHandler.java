package com.bdis.modules.settings.support;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CommonSettingNamespaceHandler extends AbstractSettingNamespaceHandler {

    private static final Set<String> FIELDS = Set.of("defaultLandingPath", "timezone", "locale");

    private static final Set<String> LANDING_PATHS =
            Set.of("/", "/profile", "/settings", "/dashboard");

    private final ObjectMapper objectMapper;

    public CommonSettingNamespaceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String namespace() {
        return "common";
    }

    @Override
    public boolean supports(CurrentUser user) {
        return true;
    }

    @Override
    public JsonNode defaultValues(CurrentUser user) {
        ObjectNode values = objectMapper.createObjectNode();
        values.put("defaultLandingPath", "/");
        values.put("timezone", "Asia/Shanghai");
        values.put("locale", "zh-CN");
        return values;
    }

    @Override
    public JsonNode validateAndNormalize(JsonNode values, CurrentUser user) {
        requireObjectWithFields(values, FIELDS);
        String landingPath = textValue(values, "defaultLandingPath", "/");
        requireOneOf("defaultLandingPath", landingPath, LANDING_PATHS);
        if ("/dashboard".equals(landingPath)
                && !user.getRoleCodes().contains("ADMIN")
                && !user.getPermissions().contains("dashboard:view")) {
            throw new BusinessException("当前用户不能将权限后台设为默认页面");
        }
        String timezone = textValue(values, "timezone", "Asia/Shanghai");
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new BusinessException("timezone 不是有效时区");
        }
        String locale = textValue(values, "locale", "zh-CN");
        requireOneOf("locale", locale, Set.of("zh-CN"));
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("defaultLandingPath", landingPath);
        normalized.put("timezone", timezone);
        normalized.put("locale", locale);
        return normalized;
    }
}
