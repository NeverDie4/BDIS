package com.bdis.modules.herb.file;

import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.stereotype.Component;

@Component
public class HerbSpeciesFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final HerbSpeciesMapper herbSpeciesMapper;
    private final AuthorizationService authorizationService;

    public HerbSpeciesFileBusinessAccessPolicy(
            HerbSpeciesMapper herbSpeciesMapper, AuthorizationService authorizationService) {
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "herb_species";
    }

    @Override
    public boolean exists(Long bizId) {
        return herbSpeciesMapper.selectActiveById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("herb:species:view");
    }

    @Override
    public boolean canAttach(Long bizId) {
        return authorizationService.hasPermission("herb:species:update");
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return authorizationService.hasPermission("herb:species:update");
    }
}
