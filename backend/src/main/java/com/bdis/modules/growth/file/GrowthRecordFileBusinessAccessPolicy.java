package com.bdis.modules.growth.file;

import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.stereotype.Component;

@Component
public class GrowthRecordFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final GrowthRecordMapper growthRecordMapper;
    private final CollectionAccessService collectionAccessService;
    private final AuthorizationService authorizationService;

    public GrowthRecordFileBusinessAccessPolicy(
            GrowthRecordMapper growthRecordMapper,
            CollectionAccessService collectionAccessService,
            AuthorizationService authorizationService) {
        this.growthRecordMapper = growthRecordMapper;
        this.collectionAccessService = collectionAccessService;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "herb_growth_record";
    }

    @Override
    public boolean exists(Long bizId) {
        return growthRecordMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("growth:record:view") && withinScope(bizId);
    }

    @Override
    public boolean canAttach(Long bizId) {
        return authorizationService.hasPermission("growth:record:update") && withinScope(bizId);
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return authorizationService.hasPermission("growth:record:audit") && withinScope(bizId);
    }

    private boolean withinScope(Long bizId) {
        GrowthRecordEntity record = growthRecordMapper.selectById(bizId);
        if (record == null) {
            return false;
        }
        CollectionAccessScope scope = collectionAccessService.currentScope();
        return scope.isAllIncluded()
                || (record.getCollectorId() != null
                        && scope.getOwnerIds().contains(record.getCollectorId()));
    }
}
