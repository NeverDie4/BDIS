package com.bdis.modules.collection.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchImageRoleConstants;
import com.bdis.modules.collection.constant.HerbBatchImageStatusConstants;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchImageBatchBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchImageUpdateRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbBatchImageEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbBatchImageBindResultVO;
import com.bdis.modules.collection.vo.HerbBatchImageStatisticsVO;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbImageBatchVO;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.spectrum.entity.HerbIdentificationResultEntity;
import com.bdis.modules.spectrum.mapper.HerbIdentificationResultMapper;
import com.bdis.modules.spectrum.vo.HerbIdentificationPageVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class HerbBatchImageServiceImpl implements HerbBatchImageService {

    private final HerbBatchImageMapper herbBatchImageMapper;
    private final HerbBatchMapper herbBatchMapper;
    private final HerbImageMapper herbImageMapper;
    private final HerbIdentificationResultMapper herbIdentificationResultMapper;
    private final CollectionAccessService collectionAccessService;

    public HerbBatchImageServiceImpl(
            HerbBatchImageMapper herbBatchImageMapper,
            HerbBatchMapper herbBatchMapper,
            HerbImageMapper herbImageMapper,
            HerbIdentificationResultMapper herbIdentificationResultMapper,
            CollectionAccessService collectionAccessService) {
        this.herbBatchImageMapper = herbBatchImageMapper;
        this.herbBatchMapper = herbBatchMapper;
        this.herbImageMapper = herbImageMapper;
        this.herbIdentificationResultMapper = herbIdentificationResultMapper;
        this.collectionAccessService = collectionAccessService;
    }

    @Override
    @Transactional
    public HerbBatchImageVO bind(Long batchId, HerbBatchImageBindRequest request) {
        validateRequest(request);
        HerbBatchEntity batch = getEditableBatch(batchId);
        HerbBatchImageEntity entity = buildBinding(batch.getId(), request, true);
        if (entity.getIsPrimary() != null && entity.getIsPrimary() == 1) {
            herbBatchImageMapper.clearPrimaryByBatchId(batch.getId());
        }
        herbBatchImageMapper.insert(entity);
        refreshImageCount(batch);
        return herbBatchImageMapper.selectDetailByBatchIdAndImageId(
                batch.getId(), entity.getImageId());
    }

    @Override
    @Transactional
    public HerbBatchImageBindResultVO batchBind(
            Long batchId, HerbBatchImageBatchBindRequest request) {
        HerbBatchEntity batch = getEditableBatch(batchId);
        if (request == null || CollectionUtils.isEmpty(request.getImages())) {
            throw new BusinessException("Images are required");
        }

        List<HerbBatchImageEntity> entities = new ArrayList<>();
        Set<Long> imageIds = new HashSet<>();
        boolean primaryUsed = false;
        for (HerbBatchImageBindRequest item : request.getImages()) {
            validateRequest(item);
            if (!imageIds.add(item.getImageId())) {
                throw new BusinessException("Duplicate image in batch bind request");
            }
            HerbBatchImageEntity entity = buildBinding(batch.getId(), item, false);
            if (entity.getIsPrimary() != null && entity.getIsPrimary() == 1) {
                if (primaryUsed) {
                    entity.setIsPrimary(0);
                } else {
                    primaryUsed = true;
                }
            }
            entities.add(entity);
        }

        if (primaryUsed) {
            herbBatchImageMapper.clearPrimaryByBatchId(batch.getId());
        }
        herbBatchImageMapper.batchInsert(entities);
        refreshImageCount(batch);

        HerbBatchImageBindResultVO result = new HerbBatchImageBindResultVO();
        result.setBatchId(batch.getId());
        result.setSuccessCount(entities.size());
        result.setImages(
                herbBatchImageMapper.selectBatchImagesWithIdentification(batch.getId(), null));
        return result;
    }

    @Override
    @Transactional
    public void unbind(Long batchId, Long imageId) {
        HerbBatchEntity batch = getEditableBatch(batchId);
        HerbBatchImageEntity binding = getBoundBinding(batch.getId(), imageId);
        int affected = herbBatchImageMapper.logicDeleteByBatchIdAndImageId(batch.getId(), imageId);
        if (affected == 0) {
            throw new BusinessException("Batch image binding not found");
        }
        refreshImageCount(batch);
    }

    @Override
    public List<HerbBatchImageVO> listByBatch(Long batchId, HerbBatchImageQueryRequest request) {
        collectionAccessService.requireBatchAccess(getActiveBatch(batchId));
        HerbBatchImageQueryRequest safeRequest =
                request == null ? new HerbBatchImageQueryRequest() : request;
        if (!StringUtils.hasText(safeRequest.getBindStatus())) {
            safeRequest.setBindStatus(HerbBatchImageStatusConstants.BOUND);
        }
        validateQuery(safeRequest);
        return herbBatchImageMapper.selectBatchImagesWithIdentification(batchId, safeRequest);
    }

    @Override
    public HerbImageBatchVO getBatchByImageId(Long imageId) {
        if (imageId == null || herbImageMapper.selectActiveById(imageId) == null) {
            throw new BusinessException("Herb image not found");
        }
        HerbImageBatchVO batch = herbBatchImageMapper.selectBatchInfoByImageId(imageId);
        if (batch != null && batch.getBatchId() != null) {
            collectionAccessService.requireBatchAccess(getActiveBatch(batch.getBatchId()));
        }
        return batch;
    }

    @Override
    @Transactional
    public HerbBatchImageVO setPrimary(Long batchId, Long imageId) {
        HerbBatchEntity batch = getEditableBatch(batchId);
        getBoundBinding(batch.getId(), imageId);
        herbBatchImageMapper.clearPrimaryByBatchId(batch.getId());
        herbBatchImageMapper.updatePrimaryByBatchIdAndImageId(batch.getId(), imageId);
        return herbBatchImageMapper.selectDetailByBatchIdAndImageId(batch.getId(), imageId);
    }

    @Override
    @Transactional
    public HerbBatchImageVO update(
            Long batchId, Long imageId, HerbBatchImageUpdateRequest request) {
        HerbBatchEntity batch = getEditableBatch(batchId);
        HerbBatchImageEntity binding = getBoundBinding(batch.getId(), imageId);
        if (request == null) {
            throw new BusinessException("Update request is required");
        }
        String imageRole = normalizeRole(request.getImageRole(), binding.getImageRole());
        binding.setImageRole(imageRole);
        binding.setSortOrder(request.getSortOrder());
        binding.setRemark(request.getRemark());
        binding.setUpdatedAt(LocalDateTime.now());
        herbBatchImageMapper.updateById(binding);
        return herbBatchImageMapper.selectDetailByBatchIdAndImageId(batch.getId(), imageId);
    }

    @Override
    @Transactional
    public HerbBatchImageStatisticsVO refreshStatistics(Long batchId) {
        HerbBatchEntity batch = getEditableBatch(batchId);
        int imageCount = refreshImageCount(batch);
        HerbBatchImageStatisticsVO vo = new HerbBatchImageStatisticsVO();
        vo.setBatchId(batch.getId());
        vo.setImageCount(imageCount);
        return vo;
    }

    private HerbBatchImageEntity buildBinding(
            Long batchId, HerbBatchImageBindRequest request, boolean checkDuplicate) {
        HerbImageEntity image = herbImageMapper.selectActiveById(request.getImageId());
        if (image == null) {
            throw new BusinessException("Herb image not found");
        }
        if (checkDuplicate
                && herbBatchImageMapper.selectByBatchIdAndImageId(batchId, request.getImageId())
                        != null) {
            throw new BusinessException("Image is already bound to this batch");
        }
        if (!checkDuplicate
                && herbBatchImageMapper.selectByBatchIdAndImageId(batchId, request.getImageId())
                        != null) {
            throw new BusinessException("Image is already bound to this batch");
        }

        Long identificationResultId =
                resolveIdentificationResultId(
                        request.getImageId(), request.getIdentificationResultId());
        LocalDateTime now = LocalDateTime.now();
        HerbBatchImageEntity entity = new HerbBatchImageEntity();
        entity.setBatchId(batchId);
        entity.setImageId(request.getImageId());
        entity.setIdentificationResultId(identificationResultId);
        entity.setImageRole(
                normalizeRole(request.getImageRole(), HerbBatchImageRoleConstants.OTHER));
        entity.setIsPrimary(request.getIsPrimary() != null && request.getIsPrimary() == 1 ? 1 : 0);
        entity.setBindStatus(HerbBatchImageStatusConstants.BOUND);
        entity.setSortOrder(request.getSortOrder());
        entity.setRemark(request.getRemark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(0);
        entity.setStatus(1);
        entity.setVersion(0);
        entity.setCreatedBy(collectionAccessService.currentUserId());
        entity.setUpdatedBy(collectionAccessService.currentUserId());
        return entity;
    }

    private Long resolveIdentificationResultId(Long imageId, Long identificationResultId) {
        if (identificationResultId != null) {
            HerbIdentificationResultEntity result =
                    herbIdentificationResultMapper.selectActiveById(identificationResultId);
            if (result == null) {
                throw new BusinessException("Identification result not found");
            }
            if (!imageId.equals(result.getImageId())) {
                throw new BusinessException("Identification result does not belong to image");
            }
            return result.getId();
        }
        HerbIdentificationPageVO latest =
                herbIdentificationResultMapper.selectLatestByImageId(imageId);
        return latest == null ? null : latest.getId();
    }

    private HerbBatchImageEntity getBoundBinding(Long batchId, Long imageId) {
        HerbBatchImageEntity binding =
                herbBatchImageMapper.selectByBatchIdAndImageId(batchId, imageId);
        if (binding == null) {
            throw new BusinessException("Batch image binding not found");
        }
        return binding;
    }

    private HerbBatchEntity getActiveBatch(Long batchId) {
        if (batchId == null) {
            throw new BusinessException("Batch id is required");
        }
        HerbBatchEntity batch = herbBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("Batch not found");
        }
        return batch;
    }

    private HerbBatchEntity getEditableBatch(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchOwner(batch);
        if (HerbBatchStatusConstants.ARCHIVED.equals(batch.getBatchStatus())
                || HerbBatchStatusConstants.CANCELLED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Archived or cancelled batch cannot maintain images");
        }
        return batch;
    }

    private void validateRequest(HerbBatchImageBindRequest request) {
        if (request == null || request.getImageId() == null) {
            throw new BusinessException("Image id is required");
        }
        normalizeRole(request.getImageRole(), HerbBatchImageRoleConstants.OTHER);
    }

    private void validateQuery(HerbBatchImageQueryRequest request) {
        if (StringUtils.hasText(request.getImageRole())) {
            normalizeRole(request.getImageRole(), HerbBatchImageRoleConstants.OTHER);
        }
        if (StringUtils.hasText(request.getBindStatus())
                && !HerbBatchImageStatusConstants.BOUND.equals(request.getBindStatus())
                && !HerbBatchImageStatusConstants.REMOVED.equals(request.getBindStatus())) {
            throw new BusinessException("Invalid bind status");
        }
    }

    private String normalizeRole(String imageRole, String defaultRole) {
        String safeRole = StringUtils.hasText(imageRole) ? imageRole : defaultRole;
        if (!HerbBatchImageRoleConstants.VALID_ROLES.contains(safeRole)) {
            throw new BusinessException("Invalid image role");
        }
        return safeRole;
    }

    private int refreshImageCount(HerbBatchEntity batch) {
        Long count = herbBatchImageMapper.countBoundByBatchId(batch.getId());
        int imageCount = count == null ? 0 : count.intValue();
        batch.setImageCount(imageCount);
        batch.setUpdatedAt(LocalDateTime.now());
        batch.setUpdatedBy(collectionAccessService.currentUserId());
        herbBatchMapper.updateStatisticsById(batch);
        return imageCount;
    }
}
