package com.bdis.modules.settings.support;

import com.bdis.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSettingNamespaceHandler implements SettingNamespaceHandler {

    protected void requireObjectWithFields(JsonNode values, Set<String> allowedFields) {
        if (values == null || !values.isObject()) {
            throw new BusinessException("设置值必须是对象");
        }
        Set<String> unknownFields = new HashSet<>();
        values.fieldNames()
                .forEachRemaining(
                        field -> {
                            if (!allowedFields.contains(field)) {
                                unknownFields.add(field);
                            }
                        });
        if (!unknownFields.isEmpty()) {
            throw new BusinessException("包含未知设置字段：" + String.join(", ", unknownFields));
        }
    }

    protected String textValue(JsonNode values, String field, String defaultValue) {
        JsonNode value = values.get(field);
        if (value == null) {
            return defaultValue;
        }
        if (!value.isTextual()) {
            throw new BusinessException(field + " 必须是字符串");
        }
        return value.asText();
    }

    protected boolean booleanValue(JsonNode values, String field, boolean defaultValue) {
        JsonNode value = values.get(field);
        if (value == null) {
            return defaultValue;
        }
        if (!value.isBoolean()) {
            throw new BusinessException(field + " 必须是布尔值");
        }
        return value.asBoolean();
    }

    protected void requireOneOf(String field, String value, Set<String> allowedValues) {
        if (!allowedValues.contains(value)) {
            throw new BusinessException(field + " 的值不受支持");
        }
    }
}
