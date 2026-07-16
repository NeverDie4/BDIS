package com.bdis.modules.herb.support;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HerbImageAccessService {

    private final CollectionAccessService collectionAccessService;
    private final HerbImageMapper herbImageMapper;
    private final GrowthRecordMapper growthRecordMapper;

    public HerbImageAccessService(
            CollectionAccessService collectionAccessService,
            HerbImageMapper herbImageMapper,
            GrowthRecordMapper growthRecordMapper) {
        this.collectionAccessService = collectionAccessService;
        this.herbImageMapper = herbImageMapper;
        this.growthRecordMapper = growthRecordMapper;
    }

    public CollectionAccessScope currentScope() {
        return collectionAccessService.currentScope();
    }

    public HerbImageEntity requireAccess(Long imageId) {
        if (imageId == null) {
            throw new BusinessException("Herb image id is required");
        }
        HerbImageEntity image = herbImageMapper.selectActiveById(imageId);
        if (image == null) {
            throw new BusinessException("Herb image not found");
        }
        requireAccess(image);
        return image;
    }

    public void requireAccess(HerbImageEntity image) {
        CollectionAccessScope scope = currentScope();
        if (scope.isAllIncluded()) {
            return;
        }
        Set<Long> ownerIds = imageOwnerIds(image);
        if (ownerIds.stream().anyMatch(scope.getOwnerIds()::contains)) {
            return;
        }
        throw new ForbiddenException("图片超出当前数据范围");
    }

    public void requireGrowthRecordAccess(GrowthRecordEntity growthRecord) {
        CollectionAccessScope scope = currentScope();
        if (scope.isAllIncluded()
                || (growthRecord.getCollectorId() != null
                        && scope.getOwnerIds().contains(growthRecord.getCollectorId()))) {
            return;
        }
        throw new ForbiddenException("生长记录超出当前数据范围");
    }

    public void requireAllScope() {
        if (!currentScope().isAllIncluded()) {
            throw new ForbiddenException("批量处理需要全部数据范围");
        }
    }

    private Set<Long> imageOwnerIds(HerbImageEntity image) {
        Set<Long> ownerIds = new LinkedHashSet<>();
        if (image.getUploaderId() != null) {
            ownerIds.add(image.getUploaderId());
        }
        if (image.getGrowthRecordId() != null) {
            GrowthRecordEntity growthRecord =
                    growthRecordMapper.selectById(image.getGrowthRecordId());
            if (growthRecord != null
                    && !Integer.valueOf(1).equals(growthRecord.getIsDeleted())
                    && growthRecord.getCollectorId() != null) {
                ownerIds.add(growthRecord.getCollectorId());
            }
        }
        return ownerIds;
    }
}
