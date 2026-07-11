package com.bdis.modules.collection.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchCreateRequest;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchUpdateRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbBatchServiceImpl implements HerbBatchService {

    private static final Set<String> VALID_STATUSES =
            Set.of(
                    HerbBatchStatusConstants.DRAFT,
                    HerbBatchStatusConstants.COLLECTING,
                    HerbBatchStatusConstants.SUBMITTED,
                    HerbBatchStatusConstants.IDENTIFYING,
                    HerbBatchStatusConstants.REVIEWING,
                    HerbBatchStatusConstants.CONFIRMED,
                    HerbBatchStatusConstants.ARCHIVED,
                    HerbBatchStatusConstants.CANCELLED);

    private final HerbBatchMapper herbBatchMapper;
    private final HerbCollectionTaskMapper herbCollectionTaskMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final HerbBatchImageMapper herbBatchImageMapper;
    private final CollectionAccessService collectionAccessService;

    public HerbBatchServiceImpl(
            HerbBatchMapper herbBatchMapper,
            HerbCollectionTaskMapper herbCollectionTaskMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            HerbBatchImageMapper herbBatchImageMapper,
            CollectionAccessService collectionAccessService) {
        this.herbBatchMapper = herbBatchMapper;
        this.herbCollectionTaskMapper = herbCollectionTaskMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.herbBatchImageMapper = herbBatchImageMapper;
        this.collectionAccessService = collectionAccessService;
    }

    @Override
    @Transactional
    public HerbBatchVO create(HerbBatchCreateRequest request) {
        validateCreateRequest(request);
        if (herbBatchMapper.selectByBatchCode(request.getBatchCode()) != null) {
            throw new BusinessException("Batch code already exists");
        }
        validateTaskForExecution(request.getTaskId());
        String speciesName = resolveSpeciesName(request.getSpeciesId(), request.getSpeciesName());
        String batchStatus =
                normalizeStatus(request.getBatchStatus(), HerbBatchStatusConstants.DRAFT);

        LocalDateTime now = LocalDateTime.now();
        HerbBatchEntity entity = new HerbBatchEntity();
        entity.setBatchCode(request.getBatchCode());
        entity.setBatchName(request.getBatchName());
        entity.setTaskId(request.getTaskId());
        entity.setSpeciesId(request.getSpeciesId());
        entity.setSpeciesName(speciesName);
        entity.setBaseId(request.getBaseId());
        entity.setBaseName(request.getBaseName());
        entity.setOriginPlace(request.getOriginPlace());
        entity.setCollectStartTime(request.getCollectStartTime());
        entity.setCollectEndTime(request.getCollectEndTime());
        entity.setHarvestTime(request.getHarvestTime());
        entity.setProductionDate(request.getProductionDate());
        entity.setBatchStatus(batchStatus);
        entity.setImageCount(0);
        entity.setIdentifiedCount(0);
        entity.setReviewedCount(0);
        entity.setNeedReviewCount(0);
        entity.setTraceCode(request.getTraceCode());
        entity.setRemark(request.getRemark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(0);
        entity.setStatus(1);
        entity.setVersion(0);
        entity.setCreatedBy(collectionAccessService.currentUserId());
        entity.setUpdatedBy(collectionAccessService.currentUserId());
        herbBatchMapper.insert(entity);
        return entity.getId() == null ? toVO(entity) : getById(entity.getId());
    }

    @Override
    @Transactional
    public HerbBatchVO update(Long id, HerbBatchUpdateRequest request) {
        HerbBatchEntity existing = getActiveEntity(id);
        collectionAccessService.requireBatchOwner(existing);
        ensureEditable(existing);
        validateUpdateRequest(request);
        validateTaskForExecution(request.getTaskId());
        String speciesName = resolveSpeciesName(request.getSpeciesId(), request.getSpeciesName());
        validateNoDirectStatusChange(request.getBatchStatus(), existing.getBatchStatus());

        existing.setBatchName(request.getBatchName());
        existing.setTaskId(request.getTaskId());
        existing.setSpeciesId(request.getSpeciesId());
        existing.setSpeciesName(speciesName);
        existing.setBaseId(request.getBaseId());
        existing.setBaseName(request.getBaseName());
        existing.setOriginPlace(request.getOriginPlace());
        existing.setCollectStartTime(request.getCollectStartTime());
        existing.setCollectEndTime(request.getCollectEndTime());
        existing.setHarvestTime(request.getHarvestTime());
        existing.setProductionDate(request.getProductionDate());
        existing.setBatchStatus(existing.getBatchStatus());
        existing.setTraceCode(request.getTraceCode());
        existing.setRemark(request.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(collectionAccessService.currentUserId());
        int affected = herbBatchMapper.updateById(existing);
        if (affected == 0) {
            throw new BusinessException("Batch not found or already deleted");
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbBatchEntity existing = getActiveEntity(id);
        collectionAccessService.requireBatchOwner(existing);
        Long imageCount = herbBatchImageMapper.countByBatchId(id);
        if (imageCount != null && imageCount > 0) {
            throw new BusinessException(
                    "This batch has bound collection images and cannot be deleted directly");
        }
        int affected = herbBatchMapper.logicDeleteById(existing.getId());
        if (affected == 0) {
            throw new BusinessException("Batch not found or already deleted");
        }
    }

    @Override
    public HerbBatchVO getById(Long id) {
        collectionAccessService.requireBatchAccess(getActiveEntity(id));
        HerbBatchVO detail = herbBatchMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("Batch not found");
        }
        return detail;
    }

    @Override
    public PageResult<HerbBatchVO> page(HerbBatchQueryRequest request) {
        HerbBatchQueryRequest safeRequest = request == null ? new HerbBatchQueryRequest() : request;
        normalizePageRequest(safeRequest);
        CollectionAccessScope scope = collectionAccessService.currentScope();
        Long total = herbBatchMapper.countPage(safeRequest, scope);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbBatchVO> records =
                herbBatchMapper.selectPage(safeRequest, scope, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    @Override
    public List<HerbBatchListVO> list(HerbBatchQueryRequest request) {
        HerbBatchQueryRequest safeRequest = request == null ? new HerbBatchQueryRequest() : request;
        if (!StringUtils.hasText(safeRequest.getBatchStatus())) {
            safeRequest.setExcludedStatuses(
                    List.of(HerbBatchStatusConstants.ARCHIVED, HerbBatchStatusConstants.CANCELLED));
        } else {
            safeRequest.setExcludedStatuses(null);
        }
        return herbBatchMapper.selectList(safeRequest, collectionAccessService.currentScope());
    }

    private HerbBatchEntity getActiveEntity(Long id) {
        if (id == null) {
            throw new BusinessException("Batch id is required");
        }
        HerbBatchEntity entity = herbBatchMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Batch not found");
        }
        return entity;
    }

    private void validateCreateRequest(HerbBatchCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getBatchCode())
                || !StringUtils.hasText(request.getBatchName())) {
            throw new BusinessException("Batch code and batch name are required");
        }
    }

    private void validateUpdateRequest(HerbBatchUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getBatchName())) {
            throw new BusinessException("Batch name is required");
        }
    }

    private void validateTaskForExecution(Long taskId) {
        if (taskId == null) {
            return;
        }
        HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Collection task not found");
        }
        collectionAccessService.requireTaskExecution(task);
    }

    private String resolveSpeciesName(Long speciesId, String speciesName) {
        if (speciesId == null) {
            return speciesName;
        }
        HerbEntity species = herbSpeciesMapper.selectActiveById(speciesId);
        if (species == null) {
            throw new BusinessException("Herb species not found");
        }
        return StringUtils.hasText(speciesName) ? speciesName : species.getHerbName();
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String safeStatus = StringUtils.hasText(status) ? status : defaultStatus;
        if (!VALID_STATUSES.contains(safeStatus)) {
            throw new BusinessException("Invalid batch status");
        }
        return safeStatus;
    }

    private void validateNoDirectStatusChange(String requestedStatus, String currentStatus) {
        if (!StringUtils.hasText(requestedStatus)) {
            return;
        }
        normalizeStatus(requestedStatus, currentStatus);
        if (!requestedStatus.equals(currentStatus)) {
            throw new BusinessException("Batch status must be changed through status APIs");
        }
    }

    private void ensureEditable(HerbBatchEntity batch) {
        if (HerbBatchStatusConstants.ARCHIVED.equals(batch.getBatchStatus())
                || HerbBatchStatusConstants.CANCELLED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Archived or cancelled batch cannot be updated");
        }
    }

    private void normalizePageRequest(HerbBatchQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private HerbBatchVO toVO(HerbBatchEntity entity) {
        HerbBatchVO vo = new HerbBatchVO();
        vo.setId(entity.getId());
        vo.setBatchCode(entity.getBatchCode());
        vo.setBatchName(entity.getBatchName());
        vo.setTaskId(entity.getTaskId());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(entity.getSpeciesName());
        vo.setBaseId(entity.getBaseId());
        vo.setBaseName(entity.getBaseName());
        vo.setOriginPlace(entity.getOriginPlace());
        vo.setCollectStartTime(entity.getCollectStartTime());
        vo.setCollectEndTime(entity.getCollectEndTime());
        vo.setHarvestTime(entity.getHarvestTime());
        vo.setProductionDate(entity.getProductionDate());
        vo.setBatchStatus(entity.getBatchStatus());
        vo.setImageCount(entity.getImageCount());
        vo.setIdentifiedCount(entity.getIdentifiedCount());
        vo.setReviewedCount(entity.getReviewedCount());
        vo.setNeedReviewCount(entity.getNeedReviewCount());
        vo.setFinalSpeciesId(entity.getFinalSpeciesId());
        vo.setFinalSpeciesName(entity.getFinalSpeciesName());
        vo.setAvgSimilarity(entity.getAvgSimilarity());
        vo.setQualityLevel(entity.getQualityLevel());
        vo.setQualityScore(entity.getQualityScore());
        vo.setEvaluationSummary(entity.getEvaluationSummary());
        vo.setTraceCode(entity.getTraceCode());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }
}
