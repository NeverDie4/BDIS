package com.bdis.modules.mobile.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.constant.HerbCollectionTaskStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchCreateRequest;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.mobile.dto.MobileBatchCreateRequest;
import com.bdis.modules.mobile.dto.MobileTaskQueryRequest;
import com.bdis.modules.mobile.service.MobileHerbTaskService;
import com.bdis.modules.mobile.vo.MobileBatchVO;
import com.bdis.modules.mobile.vo.MobileTaskDetailVO;
import com.bdis.modules.mobile.vo.MobileTaskVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MobileHerbTaskServiceImpl implements MobileHerbTaskService {

    private static final DateTimeFormatter BATCH_CODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HerbCollectionTaskService herbCollectionTaskService;
    private final HerbBatchService herbBatchService;
    private final HerbCollectionTaskMapper herbCollectionTaskMapper;
    private final HerbBatchMapper herbBatchMapper;

    public MobileHerbTaskServiceImpl(
            HerbCollectionTaskService herbCollectionTaskService,
            HerbBatchService herbBatchService,
            HerbCollectionTaskMapper herbCollectionTaskMapper,
            HerbBatchMapper herbBatchMapper) {
        this.herbCollectionTaskService = herbCollectionTaskService;
        this.herbBatchService = herbBatchService;
        this.herbCollectionTaskMapper = herbCollectionTaskMapper;
        this.herbBatchMapper = herbBatchMapper;
    }

    @Override
    public PageResult<MobileTaskVO> myTasks(MobileTaskQueryRequest request) {
        MobileTaskQueryRequest safeRequest =
                request == null ? new MobileTaskQueryRequest() : request;
        HerbCollectionTaskMyQueryRequest query = new HerbCollectionTaskMyQueryRequest();
        query.setCollectorId(safeRequest.getCollectorId());
        query.setTaskStatus(safeRequest.getTaskStatus());
        query.setPageNum(safeRequest.getPageNum());
        query.setPageSize(safeRequest.getPageSize());
        PageResult<HerbCollectionTaskVO> page = herbCollectionTaskService.myTasks(query);
        List<MobileTaskVO> records = page.getRecords().stream().map(this::toMobileTaskVO).toList();
        return new PageResult<>(page.getTotal(), page.getPage(), page.getSize(), records);
    }

    @Override
    public MobileTaskDetailVO detail(Long taskId, Long collectorId) {
        HerbCollectionTaskEntity task = getTask(taskId);
        validateCollector(task, collectorId);
        HerbCollectionTaskVO detail = herbCollectionTaskService.getById(taskId);
        MobileTaskDetailVO vo = toMobileTaskDetailVO(detail);
        vo.setBatches(batches(taskId, collectorId, null, 1, 100).getRecords());
        return vo;
    }

    @Override
    public PageResult<MobileBatchVO> batches(
            Long taskId, Long collectorId, String batchStatus, Integer pageNum, Integer pageSize) {
        HerbCollectionTaskEntity task = getTask(taskId);
        validateCollector(task, collectorId);
        HerbBatchQueryRequest query = new HerbBatchQueryRequest();
        query.setTaskId(taskId);
        query.setBatchStatus(batchStatus);
        query.setPageNum(pageNum == null ? 1 : pageNum);
        query.setPageSize(pageSize == null ? 10 : pageSize);
        if (!StringUtils.hasText(batchStatus)) {
            query.setExcludedStatuses(
                    List.of(HerbBatchStatusConstants.ARCHIVED, HerbBatchStatusConstants.CANCELLED));
        }
        PageResult<HerbBatchVO> page = herbBatchService.page(query);
        List<MobileBatchVO> records =
                page.getRecords().stream().map(this::toMobileBatchVO).toList();
        return new PageResult<>(page.getTotal(), page.getPage(), page.getSize(), records);
    }

    @Override
    @Transactional
    public MobileBatchVO createBatch(Long taskId, MobileBatchCreateRequest request) {
        HerbCollectionTaskEntity task = getTask(taskId);
        MobileBatchCreateRequest safeRequest =
                request == null ? new MobileBatchCreateRequest() : request;
        validateCollector(task, safeRequest.getCollectorId());
        if (!HerbCollectionTaskStatusConstants.PUBLISHED.equals(task.getTaskStatus())
                && !HerbCollectionTaskStatusConstants.IN_PROGRESS.equals(task.getTaskStatus())) {
            throw new BusinessException(
                    "Only published or in-progress tasks can create mobile batch");
        }
        if (HerbCollectionTaskStatusConstants.PUBLISHED.equals(task.getTaskStatus())) {
            herbCollectionTaskService.start(taskId);
        }
        HerbBatchCreateRequest createRequest = new HerbBatchCreateRequest();
        createRequest.setBatchCode(generateBatchCode());
        createRequest.setBatchName(safeRequest.getBatchName());
        createRequest.setTaskId(taskId);
        createRequest.setSpeciesId(task.getSpeciesId());
        createRequest.setSpeciesName(task.getSpeciesName());
        createRequest.setBaseId(
                safeRequest.getBaseId() == null ? task.getBaseId() : safeRequest.getBaseId());
        createRequest.setBaseName(
                StringUtils.hasText(safeRequest.getBaseName())
                        ? safeRequest.getBaseName()
                        : task.getBaseName());
        createRequest.setOriginPlace(
                StringUtils.hasText(safeRequest.getOriginPlace())
                        ? safeRequest.getOriginPlace()
                        : task.getCollectPlace());
        createRequest.setCollectStartTime(
                safeRequest.getCollectStartTime() == null
                        ? LocalDateTime.now()
                        : safeRequest.getCollectStartTime());
        createRequest.setBatchStatus(HerbBatchStatusConstants.COLLECTING);
        createRequest.setRemark(safeRequest.getRemark());
        return toMobileBatchVO(herbBatchService.create(createRequest));
    }

    private HerbCollectionTaskEntity getTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException("Task id is required");
        }
        HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Collection task not found");
        }
        return task;
    }

    private void validateCollector(HerbCollectionTaskEntity task, Long collectorId) {
        if (collectorId != null
                && task.getCollectorId() != null
                && !collectorId.equals(task.getCollectorId())) {
            throw new BusinessException("Collection task does not belong to collector");
        }
    }

    private MobileTaskVO toMobileTaskVO(HerbCollectionTaskVO task) {
        MobileTaskVO vo = new MobileTaskVO();
        vo.setTaskId(task.getId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTaskName(task.getTaskName());
        vo.setSpeciesId(task.getSpeciesId());
        vo.setSpeciesName(task.getSpeciesName());
        vo.setBaseId(task.getBaseId());
        vo.setBaseName(task.getBaseName());
        vo.setCollectPlace(task.getCollectPlace());
        vo.setPlannedStartTime(task.getPlannedStartTime());
        vo.setPlannedEndTime(task.getPlannedEndTime());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setBatchCount(toInt(task.getBatchCount()));
        vo.setUnfinishedBatchCount(toInt(herbBatchMapper.countUnfinishedByTaskId(task.getId())));
        vo.setDescription(task.getDescription());
        return vo;
    }

    private MobileTaskDetailVO toMobileTaskDetailVO(HerbCollectionTaskVO task) {
        MobileTaskDetailVO vo = new MobileTaskDetailVO();
        vo.setTaskId(task.getId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTaskName(task.getTaskName());
        vo.setSpeciesId(task.getSpeciesId());
        vo.setSpeciesName(task.getSpeciesName());
        vo.setBaseId(task.getBaseId());
        vo.setBaseName(task.getBaseName());
        vo.setCollectPlace(task.getCollectPlace());
        vo.setPlannedStartTime(task.getPlannedStartTime());
        vo.setPlannedEndTime(task.getPlannedEndTime());
        vo.setCollectorId(task.getCollectorId());
        vo.setCollectorName(task.getCollectorName());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setDescription(task.getDescription());
        vo.setRemark(task.getRemark());
        return vo;
    }

    private MobileBatchVO toMobileBatchVO(HerbBatchVO batch) {
        MobileBatchVO vo = new MobileBatchVO();
        vo.setBatchId(batch.getId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setBatchName(batch.getBatchName());
        vo.setTaskId(batch.getTaskId());
        vo.setSpeciesId(batch.getSpeciesId());
        vo.setSpeciesName(batch.getSpeciesName());
        vo.setBaseId(batch.getBaseId());
        vo.setBaseName(batch.getBaseName());
        vo.setOriginPlace(batch.getOriginPlace());
        vo.setCollectStartTime(batch.getCollectStartTime());
        vo.setCollectEndTime(batch.getCollectEndTime());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setImageCount(batch.getImageCount());
        vo.setIdentifiedCount(batch.getIdentifiedCount());
        vo.setReviewedCount(batch.getReviewedCount());
        vo.setNeedReviewCount(batch.getNeedReviewCount());
        vo.setFinalSpeciesName(batch.getFinalSpeciesName());
        vo.setQualityLevel(batch.getQualityLevel());
        vo.setQualityScore(batch.getQualityScore());
        vo.setEvaluationSummary(batch.getEvaluationSummary());
        vo.setCreateTime(batch.getCreateTime());
        return vo;
    }

    private String generateBatchCode() {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "BATCH_" + LocalDateTime.now().format(BATCH_CODE_TIME_FORMAT) + "_" + random;
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }
}
