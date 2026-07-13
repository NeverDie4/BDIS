package com.bdis.modules.herb.file;

import com.bdis.common.exception.BusinessException;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.support.HerbImageAccessService;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.stereotype.Component;

@Component
public class HerbImageFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final HerbImageMapper herbImageMapper;
    private final HerbImageAccessService herbImageAccessService;
    private final AuthorizationService authorizationService;

    public HerbImageFileBusinessAccessPolicy(
            HerbImageMapper herbImageMapper,
            HerbImageAccessService herbImageAccessService,
            AuthorizationService authorizationService) {
        this.herbImageMapper = herbImageMapper;
        this.herbImageAccessService = herbImageAccessService;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "herb_image";
    }

    @Override
    public boolean exists(Long bizId) {
        return herbImageMapper.selectActiveById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("herb:identification:view") && withinScope(bizId);
    }

    @Override
    public boolean canAttach(Long bizId) {
        return authorizationService.hasPermission("herb:identification:execute")
                && withinScope(bizId);
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return authorizationService.hasPermission("herb:identification:review")
                && withinScope(bizId);
    }

    private boolean withinScope(Long bizId) {
        try {
            herbImageAccessService.requireAccess(bizId);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }
}
