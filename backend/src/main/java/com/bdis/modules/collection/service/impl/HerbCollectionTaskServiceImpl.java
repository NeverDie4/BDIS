package com.bdis.modules.collection.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbCollectionTaskStatusConstants;
import com.bdis.modules.collection.dto.HerbCollectionTaskCreateRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskUpdateRequest;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbCollectionTaskListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbCollectionTaskServiceImpl implements HerbCollectionTaskService {

    private static final Set<String> VALID_STATUSES =
            Set.of(
                    HerbCollectionTaskStatusConstants.DRAFT,
                    HerbCollectionTaskStatusConstants.PUBLISHED,
                    HerbCollectionTaskStatusConstants.IN_PROGRESS,
                    HerbCollectionTaskStatusConstants.COMPLETED,
                    HerbCollectionTaskStatusConstants.CANCELLED);

    private final HerbCollectionTaskMapper herbCollectionTaskMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final CollectionAccessService collectionAccessService;

    public HerbCollectionTaskServiceImpl(
            HerbCollectionTaskMapper herbCollectionTaskMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            CollectionAccessService collectionAccessService) {
        this.herbCollectionTaskMapper = herbCollectionTaskMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.collectionAccessService = collectionAccessService;
    }

    @Override
    @Transactional
    public HerbCollectionTaskVO create(HerbCollectionTaskCreateRequest request) {
        validateCreateRequest(request);
        if (herbCollectionTaskMapper.selectByTaskCode(request.getTaskCode()) != null) {
            throw new BusinessException("Task code already exists");
        }
        String speciesName = resolveSpeciesName(request.getSpeciesId(), request.getSpeciesName());
        collectionAccessService.requireAssignableCollector(request.getCollectorId());
        String taskStatus =
                normalizeStatus(request.getTaskStatus(), HerbCollectionTaskStatusConstants.DRAFT);

        LocalDateTime now = LocalDateTime.now();
        HerbCollectionTaskEntity entity = new HerbCollectionTaskEntity();
        entity.setTaskCode(request.getTaskCode());
        entity.setTaskName(request.getTaskName());
        entity.setSpeciesId(request.getSpeciesId());
        entity.setSpeciesName(speciesName);
        entity.setBaseId(request.getBaseId());
        entity.setBaseName(request.getBaseName());
        entity.setCollectPlace(request.getCollectPlace());
        entity.setPlannedStartTime(request.getPlannedStartTime());
        entity.setPlannedEndTime(request.getPlannedEndTime());
        entity.setCollectorId(request.getCollectorId());
        entity.setCollectorName(request.getCollectorName());
        entity.setTaskStatus(taskStatus);
        entity.setDescription(request.getDescription());
        entity.setRemark(request.getRemark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(0);
        entity.setStatus(1);
        entity.setVersion(0);
        entity.setCreatedBy(collectionAccessService.currentUserId());
        entity.setUpdatedBy(collectionAccessService.currentUserId());
        herbCollectionTaskMapper.insert(entity);
        return entity.getId() == null ? toVO(entity) : getById(entity.getId());
    }

    @Override
    @Transactional
    public HerbCollectionTaskVO update(Long id, HerbCollectionTaskUpdateRequest request) {
        HerbCollectionTaskEntity existing = getActiveEntity(id);
        collectionAccessService.requireTaskManage(existing);
        validateUpdateRequest(request);
        collectionAccessService.requireAssignableCollector(request.getCollectorId());
        String speciesName = resolveSpeciesName(request.getSpeciesId(), request.getSpeciesName());
        String taskStatus = normalizeStatus(request.getTaskStatus(), existing.getTaskStatus());

        existing.setTaskName(request.getTaskName());
        existing.setSpeciesId(request.getSpeciesId());
        existing.setSpeciesName(speciesName);
        existing.setBaseId(request.getBaseId());
        existing.setBaseName(request.getBaseName());
        existing.setCollectPlace(request.getCollectPlace());
        existing.setPlannedStartTime(request.getPlannedStartTime());
        existing.setPlannedEndTime(request.getPlannedEndTime());
        existing.setCollectorId(request.getCollectorId());
        existing.setCollectorName(request.getCollectorName());
        existing.setTaskStatus(taskStatus);
        existing.setDescription(request.getDescription());
        existing.setRemark(request.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(collectionAccessService.currentUserId());
        int affected = herbCollectionTaskMapper.updateById(existing);
        if (affected == 0) {
            throw new BusinessException("Collection task not found or already deleted");
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbCollectionTaskEntity existing = getActiveEntity(id);
        collectionAccessService.requireTaskManage(existing);
        Long batchCount = herbCollectionTaskMapper.countBatchByTaskId(id);
        if (batchCount != null && batchCount > 0) {
            throw new BusinessException(
                    "This collection task has associated batches and cannot be deleted directly");
        }
        int affected = herbCollectionTaskMapper.logicDeleteById(existing.getId());
        if (affected == 0) {
            throw new BusinessException("Collection task not found or already deleted");
        }
    }

    @Override
    public HerbCollectionTaskVO getById(Long id) {
        collectionAccessService.requireTaskAccess(getActiveEntity(id));
        HerbCollectionTaskVO detail = herbCollectionTaskMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("Collection task not found");
        }
        return detail;
    }

    @Override
    public PageResult<HerbCollectionTaskVO> page(HerbCollectionTaskQueryRequest request) {
        HerbCollectionTaskQueryRequest safeRequest =
                request == null ? new HerbCollectionTaskQueryRequest() : request;
        normalizePageRequest(safeRequest);
        safeRequest.setExcludedStatuses(null);
        CollectionAccessScope scope = collectionAccessService.currentScope();
        Long total = herbCollectionTaskMapper.countPage(safeRequest, scope);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbCollectionTaskVO> records =
                herbCollectionTaskMapper.selectPage(
                        safeRequest, scope, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    @Override
    public PageResult<HerbCollectionTaskVO> myTasks(HerbCollectionTaskMyQueryRequest request) {
        HerbCollectionTaskMyQueryRequest safeRequest =
                request == null ? new HerbCollectionTaskMyQueryRequest() : request;
        safeRequest.setCollectorId(collectionAccessService.currentUserId());
        normalizePageRequest(safeRequest);
        if (!StringUtils.hasText(safeRequest.getTaskStatus())) {
            safeRequest.setIncludedStatuses(
                    List.of(
                            HerbCollectionTaskStatusConstants.PUBLISHED,
                            HerbCollectionTaskStatusConstants.IN_PROGRESS));
        } else {
            validateStatus(safeRequest.getTaskStatus());
            safeRequest.setIncludedStatuses(null);
        }
        Long total = herbCollectionTaskMapper.countMyTasks(safeRequest);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbCollectionTaskVO> records =
                herbCollectionTaskMapper.selectMyTasks(
                        safeRequest, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    @Override
    public List<HerbCollectionTaskListVO> list(HerbCollectionTaskQueryRequest request) {
        HerbCollectionTaskQueryRequest safeRequest =
                request == null ? new HerbCollectionTaskQueryRequest() : request;
        if (!StringUtils.hasText(safeRequest.getTaskStatus())) {
            safeRequest.setExcludedStatuses(
                    List.of(
                            HerbCollectionTaskStatusConstants.COMPLETED,
                            HerbCollectionTaskStatusConstants.CANCELLED));
        } else {
            validateStatus(safeRequest.getTaskStatus());
            safeRequest.setExcludedStatuses(null);
        }
        return herbCollectionTaskMapper.selectList(
                safeRequest, collectionAccessService.currentScope());
    }

    @Override
    @Transactional
    public HerbCollectionTaskVO publish(Long id) {
        HerbCollectionTaskEntity existing = getActiveEntity(id);
        collectionAccessService.requireTaskManage(existing);
        if (!HerbCollectionTaskStatusConstants.DRAFT.equals(existing.getTaskStatus())) {
            throw new BusinessException("Only draft tasks can be published");
        }
        return updateStatus(id, HerbCollectionTaskStatusConstants.PUBLISHED);
    }

    @Override
    @Transactional
    public HerbCollectionTaskVO start(Long id) {
        HerbCollectionTaskEntity existing = getActiveEntity(id);
        collectionAccessService.requireTaskExecution(existing);
        if (!HerbCollectionTaskStatusConstants.PUBLISHED.equals(existing.getTaskStatus())) {
            throw new BusinessException("Only published tasks can be started");
        }
        return updateStatus(id, HerbCollectionTaskStatusConstants.IN_PROGRESS);
    }

    @Override
    @Transactional
    public HerbCollectionTaskVO complete(Long id) {
        HerbCollectionTaskEntity existing = getActiveEntity(id);
        collectionAccessService.requireTaskExecution(existing);
        if (!HerbCollectionTaskStatusConstants.IN_PROGRESS.equals(existing.getTaskStatus())) {
            throw new BusinessException("Only in-progress tasks can be completed");
        }
        return updateStatus(id, HerbCollectionTaskStatusConstants.COMPLETED);
    }

    @Override
    @Transactional
    public HerbCollectionTaskVO cancel(Long id) {
        HerbCollectionTaskEntity existing = getActiveEntity(id);
        collectionAccessService.requireTaskManage(existing);
        if (HerbCollectionTaskStatusConstants.COMPLETED.equals(existing.getTaskStatus())) {
            throw new BusinessException("Completed tasks cannot be cancelled");
        }
        if (HerbCollectionTaskStatusConstants.CANCELLED.equals(existing.getTaskStatus())) {
            throw new BusinessException("Collection task has already been cancelled");
        }
        return updateStatus(id, HerbCollectionTaskStatusConstants.CANCELLED);
    }

    private HerbCollectionTaskVO updateStatus(Long id, String status) {
        int affected = herbCollectionTaskMapper.updateStatusById(id, status);
        if (affected == 0) {
            throw new BusinessException("Collection task not found or already deleted");
        }
        return getById(id);
    }

    private HerbCollectionTaskEntity getActiveEntity(Long id) {
        if (id == null) {
            throw new BusinessException("Collection task id is required");
        }
        HerbCollectionTaskEntity entity = herbCollectionTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Collection task not found");
        }
        return entity;
    }

    private void validateCreateRequest(HerbCollectionTaskCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getTaskCode())
                || !StringUtils.hasText(request.getTaskName())) {
            throw new BusinessException("Task code and task name are required");
        }
        validatePlanTime(request.getPlannedStartTime(), request.getPlannedEndTime());
    }

    private void validateUpdateRequest(HerbCollectionTaskUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getTaskName())) {
            throw new BusinessException("Task name is required");
        }
        validatePlanTime(request.getPlannedStartTime(), request.getPlannedEndTime());
    }

    private void validatePlanTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException("Planned end time cannot be earlier than start time");
        }
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
        validateStatus(safeStatus);
        return safeStatus;
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException("Invalid collection task status");
        }
    }

    private void normalizePageRequest(HerbCollectionTaskQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private void normalizePageRequest(HerbCollectionTaskMyQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private HerbCollectionTaskVO toVO(HerbCollectionTaskEntity entity) {
        HerbCollectionTaskVO vo = new HerbCollectionTaskVO();
        vo.setId(entity.getId());
        vo.setTaskCode(entity.getTaskCode());
        vo.setTaskName(entity.getTaskName());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(entity.getSpeciesName());
        vo.setBaseId(entity.getBaseId());
        vo.setBaseName(entity.getBaseName());
        vo.setCollectPlace(entity.getCollectPlace());
        vo.setPlannedStartTime(entity.getPlannedStartTime());
        vo.setPlannedEndTime(entity.getPlannedEndTime());
        vo.setCollectorId(entity.getCollectorId());
        vo.setCollectorName(entity.getCollectorName());
        vo.setTaskStatus(entity.getTaskStatus());
        vo.setDescription(entity.getDescription());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }
}
