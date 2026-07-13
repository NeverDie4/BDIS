package com.bdis.modules.declaration.file;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.mapper.DeclarationMapper;
import org.springframework.stereotype.Component;

@Component
public class DeclarationFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final DeclarationMapper declarationMapper;
    private final BusinessAccessService accessService;

    public DeclarationFileBusinessAccessPolicy(
            DeclarationMapper declarationMapper, BusinessAccessService accessService) {
        this.declarationMapper = declarationMapper;
        this.accessService = accessService;
    }

    @Override
    public String bizType() {
        return "eval_application";
    }

    @Override
    public boolean exists(Long bizId) {
        return declarationMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return isAllowed(bizId, "declaration:application:view");
    }

    @Override
    public boolean canAttach(Long bizId) {
        return isAllowed(bizId, "declaration:application:update");
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return isAllowed(bizId, "declaration:application:audit");
    }

    private boolean isAllowed(Long bizId, String permissionCode) {
        DeclarationEntity declaration = declarationMapper.selectById(bizId);
        if (declaration == null) {
            return false;
        }
        try {
            accessService.requireResourceAccess(
                    bizType(), bizId, permissionCode, declaration.getApplicantId());
            return true;
        } catch (ForbiddenException exception) {
            return false;
        }
    }
}
