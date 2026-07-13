package com.bdis.modules.settings.support;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationSettingNamespaceHandler extends AbstractSettingNamespaceHandler {

    private static final Set<String> FIELDS =
            Set.of(
                    "siteEnabled",
                    "emailEnabled",
                    "taskReminderEnabled",
                    "reviewReminderEnabled",
                    "quietHoursEnabled",
                    "quietHoursStart",
                    "quietHoursEnd");

    private final ObjectMapper objectMapper;

    private final UserMapper userMapper;

    public NotificationSettingNamespaceHandler(ObjectMapper objectMapper, UserMapper userMapper) {
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
    }

    @Override
    public String namespace() {
        return "notification";
    }

    @Override
    public boolean supports(CurrentUser user) {
        return true;
    }

    @Override
    public JsonNode defaultValues(CurrentUser user) {
        return normalizedValues(user, true, false, true, true, false, "22:00", "08:00");
    }

    @Override
    public JsonNode validateAndNormalize(JsonNode values, CurrentUser user) {
        requireObjectWithFields(values, FIELDS);
        String start = textValue(values, "quietHoursStart", "22:00");
        String end = textValue(values, "quietHoursEnd", "08:00");
        requireTime(start, "quietHoursStart");
        requireTime(end, "quietHoursEnd");
        return normalizedValues(
                user,
                booleanValue(values, "siteEnabled", true),
                booleanValue(values, "emailEnabled", false),
                booleanValue(values, "taskReminderEnabled", true),
                booleanValue(values, "reviewReminderEnabled", true),
                booleanValue(values, "quietHoursEnabled", false),
                start,
                end);
    }

    private JsonNode normalizedValues(
            CurrentUser user,
            boolean siteEnabled,
            boolean emailEnabled,
            boolean taskReminderEnabled,
            boolean reviewReminderEnabled,
            boolean quietHoursEnabled,
            String start,
            String end) {
        UserEntity userEntity = userMapper.selectById(user.getUserId());
        boolean hasEmail = userEntity != null && StringUtils.hasText(userEntity.getEmail());
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("siteEnabled", siteEnabled);
        normalized.put("emailEnabled", hasEmail && emailEnabled);
        normalized.put("taskReminderEnabled", taskReminderEnabled);
        normalized.put("reviewReminderEnabled", reviewReminderEnabled);
        normalized.put("quietHoursEnabled", quietHoursEnabled);
        normalized.put("quietHoursStart", start);
        normalized.put("quietHoursEnd", end);
        return normalized;
    }

    private void requireTime(String value, String field) {
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(field + " 必须使用 HH:mm 格式");
        }
    }
}
