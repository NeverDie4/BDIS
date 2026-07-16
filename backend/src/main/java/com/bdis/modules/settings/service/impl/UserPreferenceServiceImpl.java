package com.bdis.modules.settings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.settings.dto.SettingUpdateRequest;
import com.bdis.modules.settings.entity.UserPreferenceEntity;
import com.bdis.modules.settings.mapper.UserPreferenceMapper;
import com.bdis.modules.settings.service.UserPreferenceService;
import com.bdis.modules.settings.support.SettingNamespaceHandler;
import com.bdis.modules.settings.vo.SettingNamespaceVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final int MAX_PREFERENCE_BYTES = 16 * 1024;

    private final UserPreferenceMapper preferenceMapper;

    private final ObjectMapper objectMapper;

    private final Map<String, SettingNamespaceHandler> handlers;

    public UserPreferenceServiceImpl(
            UserPreferenceMapper preferenceMapper,
            ObjectMapper objectMapper,
            List<SettingNamespaceHandler> handlers) {
        this.preferenceMapper = preferenceMapper;
        this.objectMapper = objectMapper;
        this.handlers =
                handlers.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        SettingNamespaceHandler::namespace, Function.identity()));
    }

    @Override
    public Map<String, SettingNamespaceVO> getAll() {
        CurrentUser user = SecurityUtils.currentUser();
        Map<String, SettingNamespaceVO> result = new LinkedHashMap<>();
        handlers.values().stream()
                .filter(handler -> handler.supports(user))
                .sorted(Comparator.comparing(SettingNamespaceHandler::namespace))
                .forEach(handler -> result.put(handler.namespace(), get(handler.namespace())));
        return result;
    }

    @Override
    public SettingNamespaceVO get(String namespace) {
        CurrentUser user = SecurityUtils.currentUser();
        SettingNamespaceHandler handler = requireHandler(namespace, user);
        UserPreferenceEntity entity = find(user.getUserId(), namespace);
        if (entity == null) {
            return toVO(0, handler.schemaVersion(), handler.defaultValues(user));
        }
        return toVO(entity.getVersion(), entity.getSchemaVersion(), readData(entity));
    }

    @Override
    @Transactional
    public SettingNamespaceVO update(String namespace, SettingUpdateRequest request) {
        CurrentUser user = SecurityUtils.currentUser();
        SettingNamespaceHandler handler = requireHandler(namespace, user);
        JsonNode normalized = handler.validateAndNormalize(request.getValues(), user);
        String data = writeData(normalized);
        UserPreferenceEntity existing = find(user.getUserId(), namespace);
        if (existing == null) {
            if (request.getVersion() != 0) {
                throw new ResourceConflictException("设置已被其他请求修改，请刷新后重试");
            }
            UserPreferenceEntity entity = new UserPreferenceEntity();
            entity.setUserId(user.getUserId());
            entity.setPreferenceNamespace(namespace);
            entity.setPreferenceData(data);
            entity.setSchemaVersion(handler.schemaVersion());
            entity.setVersion(0);
            try {
                preferenceMapper.insert(entity);
            } catch (DuplicateKeyException exception) {
                throw new ResourceConflictException("设置已被其他请求修改，请刷新后重试");
            }
            return toVO(entity.getVersion(), entity.getSchemaVersion(), normalized);
        }
        if (!existing.getVersion().equals(request.getVersion())) {
            throw new ResourceConflictException("设置已被其他请求修改，请刷新后重试");
        }
        existing.setPreferenceData(data);
        existing.setSchemaVersion(handler.schemaVersion());
        if (preferenceMapper.updateById(existing) != 1) {
            throw new ResourceConflictException("设置已被其他请求修改，请刷新后重试");
        }
        return toVO(existing.getVersion(), existing.getSchemaVersion(), normalized);
    }

    @Override
    @Transactional
    public SettingNamespaceVO reset(String namespace) {
        CurrentUser user = SecurityUtils.currentUser();
        SettingNamespaceHandler handler = requireHandler(namespace, user);
        preferenceMapper.delete(
                new LambdaQueryWrapper<UserPreferenceEntity>()
                        .eq(UserPreferenceEntity::getUserId, user.getUserId())
                        .eq(UserPreferenceEntity::getPreferenceNamespace, namespace));
        return toVO(0, handler.schemaVersion(), handler.defaultValues(user));
    }

    @Override
    public String preferredLandingPath(CurrentUser user) {
        SettingNamespaceHandler handler = handlers.get("common");
        UserPreferenceEntity entity = find(user.getUserId(), "common");
        JsonNode values = entity == null ? handler.defaultValues(user) : readData(entity);
        JsonNode path = values.get("defaultLandingPath");
        return path != null && path.isTextual() ? path.asText() : "/";
    }

    private SettingNamespaceHandler requireHandler(String namespace, CurrentUser user) {
        SettingNamespaceHandler handler = handlers.get(namespace);
        if (handler == null) {
            throw new ResourceNotFoundException("设置命名空间不存在");
        }
        if (!handler.supports(user)) {
            throw new ForbiddenException("无权访问该设置分区");
        }
        return handler;
    }

    private UserPreferenceEntity find(Long userId, String namespace) {
        return preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreferenceEntity>()
                        .eq(UserPreferenceEntity::getUserId, userId)
                        .eq(UserPreferenceEntity::getPreferenceNamespace, namespace)
                        .last("limit 1"));
    }

    private JsonNode readData(UserPreferenceEntity entity) {
        try {
            return objectMapper.readTree(entity.getPreferenceData());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("用户设置数据损坏", exception);
        }
    }

    private String writeData(JsonNode values) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(values);
            if (data.length > MAX_PREFERENCE_BYTES) {
                throw new BusinessException("单个设置分区不能超过16KB");
            }
            return new String(data, java.nio.charset.StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("设置值无法序列化");
        }
    }

    private SettingNamespaceVO toVO(int version, int schemaVersion, JsonNode values) {
        SettingNamespaceVO vo = new SettingNamespaceVO();
        vo.setVersion(version);
        vo.setSchemaVersion(schemaVersion);
        vo.setValues(values);
        return vo;
    }
}
