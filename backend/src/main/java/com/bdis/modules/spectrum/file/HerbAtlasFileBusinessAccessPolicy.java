package com.bdis.modules.spectrum.file;

import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import org.springframework.stereotype.Component;

@Component
public class HerbAtlasFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final HerbAtlasMapper herbAtlasMapper;
    private final CollectionAccessService collectionAccessService;
    private final AuthorizationService authorizationService;

    public HerbAtlasFileBusinessAccessPolicy(
            HerbAtlasMapper herbAtlasMapper,
            CollectionAccessService collectionAccessService,
            AuthorizationService authorizationService) {
        this.herbAtlasMapper = herbAtlasMapper;
        this.collectionAccessService = collectionAccessService;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "herb_atlas";
    }

    @Override
    public boolean exists(Long bizId) {
        return herbAtlasMapper.selectActiveById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("herb:identification:view") && withinScope(bizId);
    }

    @Override
    public boolean canAttach(Long bizId) {
        return (authorizationService.hasPermission("herb:identification:execute")
                        || authorizationService.hasPermission("herb:atlas:import"))
                && withinScope(bizId);
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return (authorizationService.hasPermission("herb:identification:review")
                        || authorizationService.hasPermission("herb:atlas:import"))
                && withinScope(bizId);
    }

    private boolean withinScope(Long bizId) {
        SpectrumEntity atlas = herbAtlasMapper.selectActiveById(bizId);
        if (atlas == null) {
            return false;
        }
        CollectionAccessScope scope = collectionAccessService.currentScope();
        return scope.isAllIncluded()
                || (atlas.getCollectorId() != null
                        && scope.getOwnerIds().contains(atlas.getCollectorId()))
                || (atlas.getCreatedBy() != null
                        && scope.getOwnerIds().contains(atlas.getCreatedBy()));
    }
}
