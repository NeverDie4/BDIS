package com.bdis.file.policy;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FileBusinessPolicyRegistry {

    private final Map<String, FileBusinessAccessPolicy> policies;

    public FileBusinessPolicyRegistry(List<FileBusinessAccessPolicy> policyList) {
        Map<String, FileBusinessAccessPolicy> registered = new HashMap<>();
        for (FileBusinessAccessPolicy policy : policyList) {
            String bizType = normalize(policy.bizType());
            FileBusinessAccessPolicy duplicate = registered.putIfAbsent(bizType, policy);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate file business policy: " + bizType);
            }
        }
        this.policies = Map.copyOf(registered);
    }

    public void require(String bizType, Long bizId, FileBusinessAction action) {
        validateBizId(bizId);
        FileBusinessAccessPolicy policy = requirePolicy(bizType);
        if (!policy.exists(bizId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "关联业务对象不存在");
        }
        if (!isAllowed(policy, bizId, action)) {
            throw new ForbiddenException("无权执行该业务文件操作");
        }
    }

    public boolean can(String bizType, Long bizId, FileBusinessAction action) {
        if (bizId == null || bizId <= 0) {
            return false;
        }
        FileBusinessAccessPolicy policy = policies.get(normalizeNullable(bizType));
        return policy != null && policy.exists(bizId) && isAllowed(policy, bizId, action);
    }

    private FileBusinessAccessPolicy requirePolicy(String bizType) {
        String normalized = normalize(bizType);
        FileBusinessAccessPolicy policy = policies.get(normalized);
        if (policy == null) {
            throw new ForbiddenException("业务类型未注册文件访问策略");
        }
        return policy;
    }

    private boolean isAllowed(
            FileBusinessAccessPolicy policy, Long bizId, FileBusinessAction action) {
        return switch (action) {
            case VIEW -> policy.canView(bizId);
            case ATTACH -> policy.canAttach(bizId);
            case DETACH -> policy.canDetach(bizId);
            case PUBLISH -> policy.canPublish(bizId);
        };
    }

    private void validateBizId(Long bizId) {
        if (bizId == null || bizId <= 0) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务 ID 不合法");
        }
    }

    private String normalize(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "业务类型不能为空");
        }
        return bizType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String bizType) {
        return StringUtils.hasText(bizType) ? bizType.trim().toLowerCase(Locale.ROOT) : "";
    }
}
