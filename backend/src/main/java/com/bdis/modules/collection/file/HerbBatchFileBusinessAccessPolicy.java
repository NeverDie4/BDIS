package com.bdis.modules.collection.file;

import com.bdis.common.exception.BusinessException;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.stereotype.Component;

@Component
public class HerbBatchFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final HerbBatchMapper herbBatchMapper;
    private final CollectionAccessService collectionAccessService;
    private final AuthorizationService authorizationService;

    public HerbBatchFileBusinessAccessPolicy(
            HerbBatchMapper herbBatchMapper,
            CollectionAccessService collectionAccessService,
            AuthorizationService authorizationService) {
        this.herbBatchMapper = herbBatchMapper;
        this.collectionAccessService = collectionAccessService;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "herb_batch";
    }

    @Override
    public boolean exists(Long bizId) {
        return herbBatchMapper.selectById(bizId) != null;
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
        HerbBatchEntity batch = herbBatchMapper.selectById(bizId);
        if (batch == null) {
            return false;
        }
        try {
            collectionAccessService.requireBatchAccess(batch);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }
}
