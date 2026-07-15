package com.bdis.modules.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.constant.HerbCollectionTaskStatusConstants;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.growth.dto.GrowthAuditCommentRequest;
import com.bdis.modules.growth.dto.GrowthAuditRequest;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.entity.GrowthAuditRecordEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.entity.GrowthTraceEventEntity;
import com.bdis.modules.growth.mapper.GrowthAuditRecordMapper;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.mapper.GrowthTraceEventMapper;
import com.bdis.modules.growth.query.GrowthRecordQuery;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthAuditHistoryVO;
import com.bdis.modules.growth.vo.GrowthChartPointVO;
import com.bdis.modules.growth.vo.GrowthPublicAuditVO;
import com.bdis.modules.growth.vo.GrowthPublicTraceArchiveVO;
import com.bdis.modules.growth.vo.GrowthPublicTraceEventVO;
import com.bdis.modules.growth.vo.GrowthPublicTraceImageVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.GrowthTraceEventVO;
import com.bdis.modules.growth.vo.GrowthTraceQrCodeVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GrowthRecordServiceImpl implements GrowthRecordService {

    private static final String RESOURCE_TYPE = "herb_growth_record";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TRACE_CODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TRACE_URL_PREFIX = "/trace/growth/";

    @Value("${bdis.file.storage-path:./storage}")
    private String storagePath = "./storage";

    @Value("${bdis.trace.public-web-base-url:http://localhost:3000}")
    private String publicWebBaseUrl = "http://localhost:3000";

    private final GrowthRecordMapper growthRecordMapper;
    private final GrowthAuditRecordMapper growthAuditRecordMapper;
    private final GrowthTraceEventMapper growthTraceEventMapper;
    private final HerbBatchMapper herbBatchMapper;
    private final HerbCollectionTaskMapper herbCollectionTaskMapper;
    private final MapPointMapper mapPointMapper;
    private final HerbMapper herbMapper;
    private final HerbImageMapper herbImageMapper;
    private final UserMapper userMapper;
    private final DataScopeService dataScopeService;
    private final DictionaryReferenceValidator dictionaryReferenceValidator;
    private final CollectionAccessService collectionAccessService;
    private final FileResourceService fileResourceService;

    public GrowthRecordServiceImpl(
            GrowthRecordMapper growthRecordMapper,
            GrowthAuditRecordMapper growthAuditRecordMapper,
            GrowthTraceEventMapper growthTraceEventMapper,
            HerbBatchMapper herbBatchMapper,
            HerbCollectionTaskMapper herbCollectionTaskMapper,
            MapPointMapper mapPointMapper,
            HerbMapper herbMapper,
            HerbImageMapper herbImageMapper,
            UserMapper userMapper,
            DataScopeService dataScopeService,
            DictionaryReferenceValidator dictionaryReferenceValidator,
            CollectionAccessService collectionAccessService,
            FileResourceService fileResourceService) {
        this.growthRecordMapper = growthRecordMapper;
        this.growthAuditRecordMapper = growthAuditRecordMapper;
        this.growthTraceEventMapper = growthTraceEventMapper;
        this.herbBatchMapper = herbBatchMapper;
        this.herbCollectionTaskMapper = herbCollectionTaskMapper;
        this.mapPointMapper = mapPointMapper;
        this.herbMapper = herbMapper;
        this.herbImageMapper = herbImageMapper;
        this.userMapper = userMapper;
        this.dataScopeService = dataScopeService;
        this.dictionaryReferenceValidator = dictionaryReferenceValidator;
        this.collectionAccessService = collectionAccessService;
        this.fileResourceService = fileResourceService;
    }

    @Override
    public List<GrowthRecordVO> listByPointId(Long pointId) {
        if (mapPointMapper.selectById(pointId) == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        LambdaQueryWrapper<GrowthRecordEntity> wrapper =
                new LambdaQueryWrapper<GrowthRecordEntity>()
                        .eq(GrowthRecordEntity::getDistributionId, pointId)
                        .orderByDesc(GrowthRecordEntity::getCollectedAt)
                        .orderByDesc(GrowthRecordEntity::getId);
        applyDataScope(wrapper);
        return growthRecordMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public GrowthRecordVO createForPoint(Long pointId, GrowthRecordCreateRequest request) {
        MapPointEntity point = mapPointMapper.selectById(pointId);
        if (point == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        if (!Integer.valueOf(1).equals(point.getStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "停用的地图点位不能新增采集记录");
        }
        validateGrowthDictionaries(
                request.getGrowthStage(), request.getSoilType(), request.getWeather());
        GrowthRecordEntity record = new GrowthRecordEntity();
        record.setSpeciesId(point.getSpeciesId());
        record.setDistributionId(pointId);
        record.setRegionId(point.getRegionId());
        record.setLongitude(point.getLongitude());
        record.setLatitude(point.getLatitude());
        record.setGrowthStage(request.getGrowthStage());
        record.setSoilType(request.getSoilType());
        record.setSoilPh(request.getSoilPh());
        record.setTemperature(request.getTemperature());
        record.setHumidity(request.getHumidity());
        record.setWeather(request.getWeather());
        record.setSampleWeight(request.getSampleWeight());
        record.setDeviceType("pc");
        record.setDataSource(defaultText(request.getDataSource(), "map"));
        record.setCollectedAt(
                request.getCollectedAt() == null ? LocalDateTime.now() : request.getCollectedAt());
        record.setRemark(request.getRemark());
        initializeNewRecord(record, request.getCollectorName());
        growthRecordMapper.insert(record);
        saveTraceEvent(record.getId(), "created", null, "draft", "创建采集记录", "采集员创建生长采集记录", null);
        point.setLastCollectedAt(record.getCollectedAt());
        point.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        mapPointMapper.updateById(point);
        return toVO(record);
    }

    @Override
    public PageResult<GrowthRecordVO> page(GrowthRecordQuery query) {
        normalizePageQuery(query);
        LambdaQueryWrapper<GrowthRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getHerbId() != null, GrowthRecordEntity::getSpeciesId, query.getHerbId())
                .eq(
                        query.getSpeciesId() != null,
                        GrowthRecordEntity::getSpeciesId,
                        query.getSpeciesId())
                .eq(query.getTaskId() != null, GrowthRecordEntity::getTaskId, query.getTaskId())
                .eq(query.getBatchId() != null, GrowthRecordEntity::getBatchId, query.getBatchId())
                .eq(query.getBaseId() != null, GrowthRecordEntity::getBaseId, query.getBaseId())
                .eq(
                        query.getDistributionId() != null,
                        GrowthRecordEntity::getDistributionId,
                        query.getDistributionId())
                .eq(
                        query.getCollectorId() != null,
                        GrowthRecordEntity::getCollectorId,
                        query.getCollectorId())
                .eq(
                        StringUtils.hasText(query.getReviewStatus()),
                        GrowthRecordEntity::getReviewStatus,
                        query.getReviewStatus())
                .and(
                        StringUtils.hasText(query.getKeyword()),
                        condition ->
                                condition
                                        .like(
                                                GrowthRecordEntity::getCollectorNameSnapshot,
                                                query.getKeyword())
                                        .or()
                                        .like(
                                                GrowthRecordEntity::getGrowthStage,
                                                query.getKeyword())
                                        .or()
                                        .like(GrowthRecordEntity::getWeather, query.getKeyword())
                                        .or()
                                        .like(GrowthRecordEntity::getRemark, query.getKeyword())
                                        .or()
                                        .like(
                                                GrowthRecordEntity::getExternalNo,
                                                query.getKeyword()))
                .ge(
                        query.getStartTime() != null,
                        GrowthRecordEntity::getCollectedAt,
                        query.getStartTime())
                .le(
                        query.getEndTime() != null,
                        GrowthRecordEntity::getCollectedAt,
                        query.getEndTime())
                .orderByDesc(GrowthRecordEntity::getCollectedAt)
                .orderByDesc(GrowthRecordEntity::getId);
        applyDataScope(wrapper);
        Page<GrowthRecordEntity> result =
                growthRecordMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toVO).toList(), result);
    }

    @Override
    public GrowthRecordVO detail(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        GrowthRecordVO vo = growthRecordMapper.selectJoinedById(id);
        if (vo == null) {
            throw new ResourceNotFoundException("生长记录不存在");
        }
        vo.setImages(herbImageMapper.selectByGrowthRecordId(id));
        return vo;
    }

    @Override
    @Transactional
    public GrowthRecordVO create(GrowthRecordUpsertRequest request) {
        if (request == null || request.getBatchId() == null) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "采集批次不能为空");
        }
        return createForBatch(request.getBatchId(), request);
    }

    @Override
    @Transactional
    public GrowthRecordVO importFromSoap(
            String externalNo, GrowthRecordUpsertRequest request, String externalCollectorName) {
        if (!StringUtils.hasText(externalNo)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "SOAP 外部编号不能为空");
        }
        GrowthRecordEntity existing =
                growthRecordMapper.selectOne(
                        new LambdaQueryWrapper<GrowthRecordEntity>()
                                .eq(GrowthRecordEntity::getExternalSource, "SOAP")
                                .eq(GrowthRecordEntity::getExternalNo, externalNo));
        if (existing != null) {
            requireAccess(existing);
            return toVO(existing);
        }
        validateReferences(request);
        GrowthRecordEntity entity = new GrowthRecordEntity();
        apply(entity, request);
        entity.setExternalSource("SOAP");
        entity.setExternalNo(externalNo);
        initializeNewRecord(entity, externalCollectorName);
        growthRecordMapper.insert(entity);
        saveTraceEvent(entity.getId(), "created", null, "draft", "导入采集记录", "SOAP 导入生长采集记录", null);
        return toVO(entity);
    }

    @Override
    @Transactional
    public GrowthRecordVO update(Long id, GrowthRecordUpsertRequest request) {
        GrowthRecordEntity entity = requireRecord(id);
        requireOwner(entity);
        if (!"draft".equals(entity.getReviewStatus())
                && !"rejected".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "当前状态不允许编辑");
        }
        if (entity.getBatchId() != null) {
            HerbBatchEntity batch = requireWritableBatch(entity.getBatchId());
            if (request.getSpeciesId() != null
                    && !Objects.equals(request.getSpeciesId(), batch.getSpeciesId())) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "药材品种必须与所属采集批次一致");
            }
            request.setSpeciesId(batch.getSpeciesId());
        }
        validateReferences(request);
        String metadataJson = buildUpdateMetadata(entity, request);
        apply(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        saveTraceEvent(
                entity.getId(),
                "updated",
                entity.getReviewStatus(),
                entity.getReviewStatus(),
                "修改采集记录",
                "采集员修改生长采集记录",
                metadataJson);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireOwner(entity);
        if (!"draft".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "只有草稿记录可以删除");
        }
        growthRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public GrowthRecordVO submit(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireOwner(entity);
        if (!"draft".equals(entity.getReviewStatus())
                && !"rejected".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "当前状态不允许提交");
        }
        String before = entity.getReviewStatus();
        entity.setReviewStatus("submitted");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        String action = "rejected".equals(before) ? "resubmit" : "submit";
        String eventType = "rejected".equals(before) ? "resubmitted" : "submitted";
        String comment = "rejected".equals(before) ? "重新提交审核" : "提交审核";
        saveAudit(entity.getId(), action, before, "submitted", comment);
        saveTraceEvent(entity.getId(), eventType, before, "submitted", comment, comment, null);
        return toVO(entity);
    }

    @Override
    public GrowthRecordVO getByBatchId(Long batchId) {
        requireBatch(batchId);
        GrowthRecordEntity entity =
                growthRecordMapper.selectOne(
                        new LambdaQueryWrapper<GrowthRecordEntity>()
                                .eq(GrowthRecordEntity::getBatchId, batchId));
        if (entity == null) {
            throw new ResourceNotFoundException("该批次尚无生长记录");
        }
        requireAccess(entity);
        GrowthRecordVO vo = growthRecordMapper.selectJoinedByBatchId(batchId);
        vo.setImages(herbImageMapper.selectByGrowthRecordId(entity.getId()));
        return vo;
    }

    @Override
    @Transactional
    public GrowthRecordVO createForBatch(Long batchId, GrowthRecordUpsertRequest request) {
        HerbBatchEntity batch = requireWritableBatch(batchId);
        Long count =
                growthRecordMapper.selectCount(
                        new LambdaQueryWrapper<GrowthRecordEntity>()
                                .eq(GrowthRecordEntity::getBatchId, batchId));
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "该批次已存在生长记录");
        }
        validateGrowthDictionaries(
                request.getGrowthStage(), request.getSoilType(), request.getWeather());
        GrowthRecordEntity entity = new GrowthRecordEntity();
        apply(entity, request);
        applyBatchContext(entity, batch);
        if (entity.getCollectedAt() == null) {
            entity.setCollectedAt(
                    batch.getCollectStartTime() == null
                            ? LocalDateTime.now()
                            : batch.getCollectStartTime());
        }
        initializeNewRecord(entity, SecurityUtils.currentUser().getRealName());
        growthRecordMapper.insert(entity);
        saveTraceEvent(entity.getId(), "created", null, "draft", "创建采集记录", "按采集批次创建生长记录", null);
        return detail(entity.getId());
    }

    @Override
    @Transactional
    public GrowthRecordVO updateForBatch(
            Long batchId, Long recordId, GrowthRecordUpsertRequest request) {
        HerbBatchEntity batch = requireWritableBatch(batchId);
        GrowthRecordEntity entity = requireRecord(recordId);
        if (!Objects.equals(batchId, entity.getBatchId())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "生长记录不属于指定批次");
        }
        request.setSpeciesId(batch.getSpeciesId());
        update(recordId, request);
        return detail(recordId);
    }

    @Override
    public List<GrowthChartPointVO> getChartByTaskId(Long taskId, String metric) {
        if (herbCollectionTaskMapper.selectById(taskId) == null) {
            throw new ResourceNotFoundException("采集任务不存在");
        }
        String safeMetric = normalizeMetric(metric);
        LambdaQueryWrapper<GrowthRecordEntity> wrapper =
                new LambdaQueryWrapper<GrowthRecordEntity>()
                        .eq(GrowthRecordEntity::getTaskId, taskId)
                        .isNotNull(GrowthRecordEntity::getBatchId);
        applyDataScope(wrapper);
        List<Long> recordIds =
                growthRecordMapper.selectList(wrapper).stream()
                        .map(GrowthRecordEntity::getId)
                        .toList();
        if (recordIds.isEmpty()) {
            return List.of();
        }
        List<GrowthChartPointVO> points = growthRecordMapper.selectChartPoints(taskId, recordIds);
        points.forEach(
                point -> {
                    point.setMetric(safeMetric);
                    point.setValue(metricValue(point, safeMetric));
                });
        return points;
    }

    @Override
    @Transactional
    public GrowthRecordVO approve(Long id, GrowthAuditCommentRequest request) {
        return review(id, "approved", request == null ? null : request.getComment());
    }

    @Override
    @Transactional
    public GrowthRecordVO reject(Long id, GrowthAuditCommentRequest request) {
        String comment = request == null ? null : request.getComment();
        if (!StringUtils.hasText(comment)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "驳回意见不能为空");
        }
        return review(id, "rejected", comment);
    }

    @Override
    @Transactional
    public GrowthRecordVO audit(Long id, GrowthAuditRequest request) {
        if ("rejected".equals(request.getDecision())
                && !StringUtils.hasText(request.getComment())) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "驳回意见不能为空");
        }
        return review(id, request.getDecision(), request.getComment());
    }

    @Override
    @Transactional
    public GrowthRecordVO archive(Long id) {
        return archive(id, new GrowthAuditCommentRequest());
    }

    @Override
    @Transactional
    public GrowthRecordVO archive(Long id, GrowthAuditCommentRequest request) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        requireAdmin();
        if (!"approved".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "只有审核通过记录可以归档");
        }
        entity.setReviewStatus("archived");
        entity.setArchivedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        String comment =
                request != null && StringUtils.hasText(request.getComment())
                        ? request.getComment()
                        : "归档";
        saveAudit(entity.getId(), "archive", "approved", "archived", comment);
        saveTraceEvent(entity.getId(), "archived", "approved", "archived", "归档", comment, null);
        return toVO(entity);
    }

    @Override
    public PageResult<GrowthRecordVO> reviewPage(GrowthRecordQuery query) {
        query.setReviewStatus("submitted");
        return page(query);
    }

    @Override
    public List<GrowthAuditHistoryVO> auditHistory(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        return growthAuditRecordMapper
                .selectList(
                        new LambdaQueryWrapper<GrowthAuditRecordEntity>()
                                .eq(GrowthAuditRecordEntity::getGrowthRecordId, id)
                                .orderByAsc(GrowthAuditRecordEntity::getReviewedAt)
                                .orderByAsc(GrowthAuditRecordEntity::getId))
                .stream()
                .map(this::toAuditHistoryVO)
                .toList();
    }

    @Override
    public List<GrowthTraceEventVO> trace(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        return loadTraceEvents(id);
    }

    @Override
    @Transactional
    public GrowthTraceQrCodeVO generateTraceCode(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireTraceManageAccess(entity);
        ensureTraceCode(entity, true);
        return toTraceQrCodeVO(entity);
    }

    @Override
    @Transactional
    public GrowthTraceQrCodeVO generateTraceQrCode(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireTraceManageAccess(entity);
        ensureTraceCode(entity, true);
        String traceUrl = traceUrl(entity.getTraceCode());
        String qrCodeUrl =
                writeQrCode(
                        entity.getId(),
                        entity.getTraceCode(),
                        fullTraceUrl(publicWebBaseUrl, traceUrl));
        entity.setTraceQrcodeUrl(qrCodeUrl);
        entity.setTracePublicUrl(traceUrl);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        saveTraceEvent(
                entity.getId(),
                "trace_qrcode_generated",
                entity.getReviewStatus(),
                entity.getReviewStatus(),
                "生成溯源二维码",
                "系统生成该生长记录的公开溯源二维码",
                null);
        return toTraceQrCodeVO(entity);
    }

    @Override
    public GrowthTraceQrCodeVO getTraceQrCode(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        return toTraceQrCodeVO(entity);
    }

    @Override
    @Transactional
    public GrowthTraceQrCodeVO enablePublicTrace(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireTraceManageAccess(entity);
        ensureTraceCode(entity, true);
        entity.setPublicVisible(1);
        entity.setTracePublicUrl(traceUrl(entity.getTraceCode()));
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        saveTraceEvent(
                entity.getId(),
                "public_trace_enabled",
                entity.getReviewStatus(),
                entity.getReviewStatus(),
                "开启公开溯源",
                "该生长记录已允许通过溯源码公开查询",
                null);
        synchronizeTaskDigitalLifeArchive(entity.getTaskId());
        return toTraceQrCodeVO(entity);
    }

    @Override
    @Transactional
    public GrowthTraceQrCodeVO disablePublicTrace(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireTraceManageAccess(entity);
        entity.setPublicVisible(0);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        saveTraceEvent(
                entity.getId(),
                "public_trace_disabled",
                entity.getReviewStatus(),
                entity.getReviewStatus(),
                "关闭公开溯源",
                "该生长记录已关闭公开查询",
                null);
        synchronizeTaskDigitalLifeArchive(entity.getTaskId());
        return toTraceQrCodeVO(entity);
    }

    private void synchronizeTaskDigitalLifeArchive(Long taskId) {
        if (taskId == null) {
            return;
        }
        HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(taskId);
        if (task == null
                || !Integer.valueOf(1).equals(task.getStatus())
                || HerbCollectionTaskStatusConstants.CANCELLED.equals(task.getTaskStatus())) {
            return;
        }
        long publicStageCount =
                growthRecordMapper.selectCount(
                        new LambdaQueryWrapper<GrowthRecordEntity>()
                                .eq(GrowthRecordEntity::getTaskId, taskId)
                                .eq(GrowthRecordEntity::getStatus, 1)
                                .eq(GrowthRecordEntity::getReviewStatus, "approved")
                                .eq(GrowthRecordEntity::getPublicVisible, 1));
        boolean shouldPublish = publicStageCount >= 2;
        boolean changed = false;
        if (shouldPublish && !StringUtils.hasText(task.getTraceCode())) {
            task.setTraceCode(String.format("DL-TASK-%08d", taskId));
            changed = true;
        }
        int publicVisible = shouldPublish ? 1 : 0;
        if (!Integer.valueOf(publicVisible).equals(task.getPublicVisible())) {
            task.setPublicVisible(publicVisible);
            changed = true;
        }
        if (changed) {
            task.setUpdatedBy(SecurityUtils.currentUser().getUserId());
            herbCollectionTaskMapper.updateById(task);
        }
    }

    @Override
    public GrowthPublicTraceArchiveVO publicTrace(String traceCode) {
        GrowthRecordEntity entity = requirePublicTrace(traceCode);
        GrowthPublicTraceArchiveVO archive = toPublicTraceArchive(entity);
        attachPublicDigitalLifeTrace(archive);
        archive.setImages(loadPublicImages(entity.getBatchId(), entity.getTraceCode()));
        archive.setAuditHistory(
                loadAuditHistory(entity.getId()).stream().map(this::toPublicAudit).toList());
        archive.setTraceTimeline(
                loadTraceEvents(entity.getId()).stream().map(this::toPublicTraceEvent).toList());
        applyLatestAudit(archive);
        return archive;
    }

    private void attachPublicDigitalLifeTrace(GrowthPublicTraceArchiveVO archive) {
        if (archive.getTaskId() == null) {
            return;
        }
        HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(archive.getTaskId());
        if (task == null
                || !Integer.valueOf(1).equals(task.getStatus())
                || !Integer.valueOf(1).equals(task.getPublicVisible())
                || HerbCollectionTaskStatusConstants.CANCELLED.equals(task.getTaskStatus())
                || !StringUtils.hasText(task.getTraceCode())) {
            return;
        }
        archive.setTaskTraceCode(task.getTraceCode());
    }

    @Override
    public FileContentVO traceQrCodeContent(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        return loadTraceQrCode(entity);
    }

    @Override
    public FileContentVO publicTraceQrCode(String traceCode) {
        return loadTraceQrCode(requirePublicTrace(traceCode));
    }

    @Override
    public FileContentVO publicTraceImage(String traceCode, Long imageId) {
        GrowthRecordEntity entity = requirePublicTrace(traceCode);
        HerbImageVO image =
                herbImageMapper.selectByBatchId(entity.getBatchId()).stream()
                        .filter(item -> Objects.equals(item.getId(), imageId))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("公开溯源图片不存在"));
        Long fileId = fileResourceService.resolveFileId(image.getImageUrl());
        if (fileId == null) {
            throw new ResourceNotFoundException("公开溯源图片文件不存在");
        }
        return fileResourceService.internalContent(fileId);
    }

    private GrowthRecordEntity requirePublicTrace(String traceCode) {
        if (!StringUtils.hasText(traceCode)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "溯源码不能为空");
        }
        GrowthRecordEntity entity =
                growthRecordMapper.selectOne(
                        new LambdaQueryWrapper<GrowthRecordEntity>()
                                .eq(GrowthRecordEntity::getTraceCode, traceCode));
        if (entity == null) {
            throw new ResourceNotFoundException("溯源码不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getPublicVisible())) {
            throw new ForbiddenException("该溯源档案暂未公开");
        }
        return entity;
    }

    private List<GrowthTraceEventVO> loadTraceEvents(Long id) {
        List<GrowthTraceEventVO> events =
                growthTraceEventMapper
                        .selectList(
                                new LambdaQueryWrapper<GrowthTraceEventEntity>()
                                        .eq(GrowthTraceEventEntity::getRecordId, id)
                                        .orderByAsc(GrowthTraceEventEntity::getEventTime)
                                        .orderByAsc(GrowthTraceEventEntity::getId))
                        .stream()
                        .map(this::toTraceEventVO)
                        .toList();
        if (!events.isEmpty()) {
            return events;
        }
        GrowthRecordEntity entity = growthRecordMapper.selectById(id);
        if (entity == null) {
            return List.of();
        }
        List<GrowthTraceEventVO> legacyEvents = new ArrayList<>();
        legacyEvents.add(
                legacyEvent(
                        "created",
                        "create",
                        null,
                        "draft",
                        entity.getCreatedBy(),
                        entity.getCreatedAt(),
                        entity.getRemark()));
        growthAuditRecordMapper
                .selectList(
                        new LambdaQueryWrapper<GrowthAuditRecordEntity>()
                                .eq(GrowthAuditRecordEntity::getGrowthRecordId, id)
                                .orderByAsc(GrowthAuditRecordEntity::getReviewedAt)
                                .orderByAsc(GrowthAuditRecordEntity::getId))
                .forEach(
                        audit ->
                                legacyEvents.add(
                                        legacyEvent(
                                                "status_change",
                                                audit.getReviewAction(),
                                                audit.getBeforeStatus(),
                                                audit.getAfterStatus(),
                                                audit.getReviewerId(),
                                                audit.getReviewedAt(),
                                                audit.getReviewComment())));
        return legacyEvents;
    }

    private void initializeNewRecord(GrowthRecordEntity entity, String collectorName) {
        CurrentUser user = SecurityUtils.currentUser();
        entity.setCollectorId(user.getUserId());
        entity.setCollectorNameSnapshot(
                StringUtils.hasText(collectorName) ? collectorName : user.getUsername());
        entity.setReviewStatus("draft");
        entity.setCollectedAt(
                entity.getCollectedAt() == null ? LocalDateTime.now() : entity.getCollectedAt());
        entity.setCreatedBy(user.getUserId());
        entity.setStatus(1);
    }

    private void normalizePageQuery(GrowthRecordQuery query) {
        if (query.getPageNum() != null) {
            query.setPage(query.getPageNum());
        }
        if (query.getPageSize() != null) {
            query.setSize(query.getPageSize());
        }
    }

    private void apply(GrowthRecordEntity entity, GrowthRecordUpsertRequest request) {
        validateCoordinates(request);
        entity.setSpeciesId(request.getSpeciesId());
        entity.setDistributionId(request.getDistributionId());
        entity.setRegionId(request.getRegionId());
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setGrowthStage(request.getGrowthStage());
        entity.setPlantHeight(request.getPlantHeight());
        entity.setSoilType(request.getSoilType());
        entity.setSoilPh(request.getSoilPh());
        entity.setTemperature(request.getTemperature());
        entity.setHumidity(request.getHumidity());
        entity.setSoilMoisture(request.getSoilMoisture());
        entity.setLight(request.getLight());
        entity.setStemDiameter(request.getStemDiameter());
        entity.setLeafColor(request.getLeafColor());
        entity.setFloweringStatus(request.getFloweringStatus());
        entity.setGrowthEvaluation(request.getGrowthEvaluation());
        entity.setWeather(request.getWeather());
        entity.setSampleWeight(request.getSampleWeight());
        entity.setDeviceType(defaultText(request.getDeviceType(), "web"));
        entity.setDataSource(defaultText(request.getDataSource(), "manual"));
        entity.setCollectedAt(request.getCollectedAt());
        entity.setRemark(request.getRemark());
    }

    private void validateCoordinates(GrowthRecordUpsertRequest request) {
        boolean hasLongitude = request.getLongitude() != null;
        boolean hasLatitude = request.getLatitude() != null;
        if (hasLongitude != hasLatitude) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "经纬度必须同时填写");
        }
        if (!hasLongitude) {
            return;
        }
        if (request.getLongitude().compareTo(new BigDecimal("-180")) < 0
                || request.getLongitude().compareTo(new BigDecimal("180")) > 0) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "经度范围必须在 -180 到 180 之间");
        }
        if (request.getLatitude().compareTo(new BigDecimal("-90")) < 0
                || request.getLatitude().compareTo(new BigDecimal("90")) > 0) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "纬度范围必须在 -90 到 90 之间");
        }
    }

    private void validateReferences(GrowthRecordUpsertRequest request) {
        if (herbMapper.selectById(request.getSpeciesId()) == null) {
            throw new ResourceNotFoundException("药材品种不存在");
        }
        if (request.getDistributionId() != null
                && mapPointMapper.selectById(request.getDistributionId()) == null) {
            throw new ResourceNotFoundException("地图点位不存在");
        }
        validateGrowthDictionaries(
                request.getGrowthStage(), request.getSoilType(), request.getWeather());
    }

    private void validateGrowthDictionaries(String growthStage, String soilType, String weather) {
        dictionaryReferenceValidator.validateIfConfigured("growth_stage", growthStage, "生长阶段");
        dictionaryReferenceValidator.validateIfConfigured("soil_type", soilType, "土壤类型");
        dictionaryReferenceValidator.validateIfConfigured("weather", weather, "天气");
    }

    private GrowthRecordEntity requireRecord(Long id) {
        GrowthRecordEntity entity = growthRecordMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("生长记录不存在");
        }
        return entity;
    }

    private HerbBatchEntity requireBatch(Long batchId) {
        if (batchId == null) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "采集批次不能为空");
        }
        HerbBatchEntity batch = herbBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ResourceNotFoundException("采集批次不存在");
        }
        if (batch.getTaskId() == null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "采集批次尚未关联采集任务");
        }
        return batch;
    }

    private HerbBatchEntity requireWritableBatch(Long batchId) {
        HerbBatchEntity batch = requireBatch(batchId);
        collectionAccessService.requireBatchOwner(batch);
        if (Set.of(HerbBatchStatusConstants.ARCHIVED, HerbBatchStatusConstants.CANCELLED)
                .contains(batch.getBatchStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "当前批次状态不可填写生长记录");
        }
        HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(batch.getTaskId());
        if (task == null) {
            throw new ResourceNotFoundException("采集任务不存在");
        }
        collectionAccessService.requireTaskExecution(task);
        if (Set.of(
                        HerbCollectionTaskStatusConstants.COMPLETED,
                        HerbCollectionTaskStatusConstants.CANCELLED)
                .contains(task.getTaskStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "当前任务状态不可填写生长记录");
        }
        return batch;
    }

    private void applyBatchContext(GrowthRecordEntity entity, HerbBatchEntity batch) {
        entity.setBatchId(batch.getId());
        entity.setTaskId(batch.getTaskId());
        entity.setSpeciesId(batch.getSpeciesId());
        entity.setSpeciesName(batch.getSpeciesName());
        entity.setBaseId(batch.getBaseId());
        entity.setBaseName(batch.getBaseName());
    }

    private String normalizeMetric(String metric) {
        String safeMetric = StringUtils.hasText(metric) ? metric : "plantHeight";
        Set<String> supported =
                Set.of(
                        "plantHeight",
                        "temperature",
                        "humidity",
                        "soilMoisture",
                        "soilPh",
                        "light",
                        "sampleWeight");
        if (!supported.contains(safeMetric)) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "不支持的生长趋势指标");
        }
        return safeMetric;
    }

    private java.math.BigDecimal metricValue(GrowthChartPointVO point, String metric) {
        return switch (metric) {
            case "temperature" -> point.getTemperature();
            case "humidity" -> point.getHumidity();
            case "soilMoisture" -> point.getSoilMoisture();
            case "soilPh" -> point.getSoilPh();
            case "light" -> point.getLight();
            case "sampleWeight" -> point.getSampleWeight();
            default -> point.getPlantHeight();
        };
    }

    private void requireOwner(GrowthRecordEntity entity) {
        if (!SecurityUtils.currentUser().getUserId().equals(entity.getCollectorId())
                && !dataScopeService.resolveForCurrentUser(RESOURCE_TYPE).isAllIncluded()) {
            throw new ForbiddenException("只能操作本人采集记录");
        }
    }

    private void requireTraceManageAccess(GrowthRecordEntity entity) {
        requireAccess(entity);
        Set<String> roles = SecurityUtils.currentUser().getRoleCodes();
        if (roles.contains("ADMIN") || roles.contains("REVIEWER") || roles.contains("TEACHER")) {
            return;
        }
        throw new ForbiddenException("无权操作生长记录公开溯源");
    }

    private void requireAccess(GrowthRecordEntity entity) {
        CurrentUser current = SecurityUtils.currentUser();
        if (current.getUserId().equals(entity.getCollectorId())) {
            return;
        }
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser(RESOURCE_TYPE);
        if (scope.isAllIncluded()) {
            return;
        }
        UserEntity collector = userMapper.selectById(entity.getCollectorId());
        if (collector != null
                && ((collector.getOrganizationId() != null
                                && scope.getOrganizationIds()
                                        .contains(collector.getOrganizationId()))
                        || (collector.getDepartmentId() != null
                                && scope.getDepartmentIds()
                                        .contains(collector.getDepartmentId())))) {
            return;
        }
        throw new ForbiddenException("生长记录超出当前数据范围");
    }

    private void ensureTraceCode(GrowthRecordEntity entity, boolean writeEvent) {
        if (StringUtils.hasText(entity.getTraceCode())) {
            if (!StringUtils.hasText(entity.getTracePublicUrl())) {
                entity.setTracePublicUrl(traceUrl(entity.getTraceCode()));
                entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
                growthRecordMapper.updateById(entity);
            }
            return;
        }
        CurrentUser current = SecurityUtils.currentUser();
        String traceCode = nextTraceCode();
        entity.setTraceCode(traceCode);
        entity.setTraceGeneratedTime(LocalDateTime.now());
        entity.setTraceGeneratedBy(current.getUserId());
        entity.setTraceGeneratedByName(displayName(current));
        entity.setTracePublicUrl(traceUrl(traceCode));
        entity.setPublicVisible(entity.getPublicVisible() == null ? 0 : entity.getPublicVisible());
        entity.setUpdatedBy(current.getUserId());
        growthRecordMapper.updateById(entity);
        if (writeEvent) {
            saveTraceEvent(
                    entity.getId(),
                    "trace_code_generated",
                    entity.getReviewStatus(),
                    entity.getReviewStatus(),
                    "生成溯源码",
                    "为该生长记录生成溯源码",
                    null);
        }
    }

    private String nextTraceCode() {
        for (int i = 0; i < 10; i++) {
            String candidate =
                    "TRACE_GROWTH_"
                            + LocalDateTime.now().format(TRACE_CODE_TIME_FORMAT)
                            + "_"
                            + ThreadLocalRandom.current().nextInt(100000, 1000000);
            Long count =
                    growthRecordMapper.selectCount(
                            new LambdaQueryWrapper<GrowthRecordEntity>()
                                    .eq(GrowthRecordEntity::getTraceCode, candidate));
            if (count == null || count == 0) {
                return candidate;
            }
        }
        throw new BusinessException(ResultCodeEnum.CONFLICT, "溯源码生成冲突，请重试");
    }

    private String writeQrCode(Long recordId, String traceCode, String content) {
        try {
            Path directory = Path.of(storagePath, "trace", "qrcode").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = "growth_" + recordId + "_" + traceCode + ".png";
            Path target = directory.resolve(fileName).normalize();
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320);
            MatrixToImageWriter.writeToPath(matrix, "PNG", target);
            return "/api/growth-records/" + recordId + "/trace-qrcode/content";
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "生成溯源二维码失败");
        } catch (Exception ex) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "生成溯源二维码失败");
        }
    }

    private String traceUrl(String traceCode) {
        return TRACE_URL_PREFIX + traceCode;
    }

    private String fullTraceUrl(String publicBaseUrl, String traceUrl) {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return traceUrl;
        }
        return publicBaseUrl.replaceAll("/+$", "") + traceUrl;
    }

    private FileContentVO loadTraceQrCode(GrowthRecordEntity entity) {
        if (!StringUtils.hasText(entity.getTraceCode())
                || !StringUtils.hasText(entity.getTraceQrcodeUrl())) {
            throw new ResourceNotFoundException("溯源二维码尚未生成");
        }
        Path directory = Path.of(storagePath, "trace", "qrcode").toAbsolutePath().normalize();
        String fileName = "growth_" + entity.getId() + "_" + entity.getTraceCode() + ".png";
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory) || !Files.isRegularFile(target)) {
            throw new ResourceNotFoundException("溯源二维码文件不存在");
        }
        FileContentVO content = new FileContentVO();
        content.setResource(new PathResource(target));
        content.setFileName(fileName);
        content.setContentType("image/png");
        try {
            content.setFileSize(Files.size(target));
        } catch (IOException ex) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "读取溯源二维码失败");
        }
        return content;
    }

    private GrowthTraceQrCodeVO toTraceQrCodeVO(GrowthRecordEntity entity) {
        GrowthTraceQrCodeVO vo = new GrowthTraceQrCodeVO();
        vo.setRecordId(entity.getId());
        vo.setTraceCode(entity.getTraceCode());
        vo.setTraceUrl(entity.getTracePublicUrl());
        vo.setQrCodeUrl(entity.getTraceQrcodeUrl());
        vo.setPublicVisible(entity.getPublicVisible() == null ? 0 : entity.getPublicVisible());
        vo.setTraceGeneratedTime(entity.getTraceGeneratedTime());
        return vo;
    }

    private GrowthPublicTraceArchiveVO toPublicTraceArchive(GrowthRecordEntity entity) {
        GrowthRecordVO record = toVO(entity);
        GrowthPublicTraceArchiveVO archive = new GrowthPublicTraceArchiveVO();
        archive.setRecordId(entity.getId());
        archive.setTraceCode(entity.getTraceCode());
        archive.setTraceUrl(entity.getTracePublicUrl());
        archive.setQrCodeUrl(
                StringUtils.hasText(entity.getTraceQrcodeUrl())
                        ? "/api/trace/growth/" + entity.getTraceCode() + "/qrcode"
                        : null);
        archive.setPublicVisible(entity.getPublicVisible() == null ? 0 : entity.getPublicVisible());
        archive.setSpeciesName(record.getSpeciesName());
        archive.setHerbName(record.getSpeciesName());
        archive.setTaskId(entity.getTaskId());
        archive.setTaskName(record.getTaskName());
        archive.setBatchId(entity.getBatchId());
        archive.setBatchName(record.getBatchName());
        archive.setBaseName(record.getBaseName());
        archive.setCollectPlace(record.getCollectPlace());
        archive.setCollectTime(entity.getCollectedAt());
        archive.setCollectorName(record.getCollectorName());
        archive.setGrowthStage(entity.getGrowthStage());
        archive.setAuditStatus(entity.getReviewStatus());
        archive.setTemperature(entity.getTemperature());
        archive.setHumidity(entity.getHumidity());
        archive.setLight(entity.getLight());
        archive.setSoilMoisture(entity.getSoilMoisture());
        archive.setSoilPh(entity.getSoilPh());
        archive.setSoilType(entity.getSoilType());
        archive.setPlantHeight(entity.getPlantHeight());
        archive.setStemDiameter(entity.getStemDiameter());
        archive.setLeafColor(entity.getLeafColor());
        archive.setFloweringStatus(entity.getFloweringStatus());
        archive.setGrowthEvaluation(entity.getGrowthEvaluation());
        archive.setSampleWeight(entity.getSampleWeight());
        return archive;
    }

    private List<GrowthPublicTraceImageVO> loadPublicImages(Long batchId, String traceCode) {
        if (batchId == null) {
            return List.of();
        }
        return herbImageMapper.selectByBatchId(batchId).stream()
                .map(image -> toPublicTraceImage(image, traceCode))
                .toList();
    }

    private GrowthPublicTraceImageVO toPublicTraceImage(HerbImageVO image, String traceCode) {
        GrowthPublicTraceImageVO vo = new GrowthPublicTraceImageVO();
        vo.setImageUrl("/api/trace/growth/" + traceCode + "/images/" + image.getId());
        vo.setImageType(image.getImageType());
        vo.setImageRole(image.getImageRole());
        vo.setUploadTime(
                image.getUploadTime() == null ? image.getCollectTime() : image.getUploadTime());
        vo.setUploaderName(image.getUploaderName());
        return vo;
    }

    private List<GrowthAuditHistoryVO> loadAuditHistory(Long id) {
        return growthAuditRecordMapper
                .selectList(
                        new LambdaQueryWrapper<GrowthAuditRecordEntity>()
                                .eq(GrowthAuditRecordEntity::getGrowthRecordId, id)
                                .orderByAsc(GrowthAuditRecordEntity::getReviewedAt)
                                .orderByAsc(GrowthAuditRecordEntity::getId))
                .stream()
                .map(this::toAuditHistoryVO)
                .toList();
    }

    private void applyLatestAudit(GrowthPublicTraceArchiveVO archive) {
        if (archive.getAuditHistory().isEmpty()) {
            return;
        }
        GrowthPublicAuditVO latest = archive.getAuditHistory().getLast();
        archive.setLatestAuditResult(latest.getAfterStatus());
        archive.setLatestAuditTime(latest.getOperateTime());
        archive.setReviewerName(latest.getOperatorName());
    }

    private GrowthPublicAuditVO toPublicAudit(GrowthAuditHistoryVO audit) {
        GrowthPublicAuditVO vo = new GrowthPublicAuditVO();
        vo.setActionType(audit.getActionType());
        vo.setBeforeStatus(audit.getBeforeStatus());
        vo.setAfterStatus(audit.getAfterStatus());
        vo.setOperatorName(audit.getOperatorName());
        vo.setOperateTime(audit.getOperateTime());
        return vo;
    }

    private GrowthPublicTraceEventVO toPublicTraceEvent(GrowthTraceEventVO event) {
        GrowthPublicTraceEventVO vo = new GrowthPublicTraceEventVO();
        vo.setEventType(event.getEventType());
        vo.setEventTitle(event.getEventTitle());
        vo.setBeforeStatus(event.getBeforeStatus());
        vo.setAfterStatus(event.getAfterStatus());
        vo.setOperatorName(event.getOperatorName());
        vo.setEventTime(event.getEventTime());
        return vo;
    }

    private void applyDataScope(LambdaQueryWrapper<GrowthRecordEntity> wrapper) {
        DataScopeResultVO scope = dataScopeService.resolveForCurrentUser(RESOURCE_TYPE);
        if (scope.isAllIncluded()) {
            return;
        }
        CurrentUser user = SecurityUtils.currentUser();
        wrapper.and(
                condition -> {
                    boolean applied = false;
                    if (scope.isSelfIncluded()) {
                        condition.eq(GrowthRecordEntity::getCollectorId, user.getUserId());
                        applied = true;
                    }
                    if (!scope.getOrganizationIds().isEmpty()) {
                        if (applied) {
                            condition.or();
                        }
                        condition.inSql(
                                GrowthRecordEntity::getCollectorId,
                                "select id from sys_user where is_deleted = 0 and organization_id in ("
                                        + joinIds(scope.getOrganizationIds())
                                        + ")");
                        applied = true;
                    }
                    if (!scope.getDepartmentIds().isEmpty()) {
                        if (applied) {
                            condition.or();
                        }
                        condition.inSql(
                                GrowthRecordEntity::getCollectorId,
                                "select id from sys_user where is_deleted = 0 and department_id in ("
                                        + joinIds(scope.getDepartmentIds())
                                        + ")");
                        applied = true;
                    }
                    if (!applied) {
                        condition.apply("1 = 0");
                    }
                });
    }

    private String joinIds(Iterable<Long> ids) {
        List<String> values = new ArrayList<>();
        ids.forEach(id -> values.add(String.valueOf(id)));
        return String.join(",", values);
    }

    private GrowthRecordVO review(Long id, String decision, String comment) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        if (!"submitted".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "只有已提交记录可以审核");
        }
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw new BusinessException(
                    ResultCodeEnum.VALIDATION_ERROR, "审核决定只能是 approved 或 rejected");
        }
        String before = entity.getReviewStatus();
        entity.setReviewStatus(decision);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        String action = "approved".equals(decision) ? "approve" : "reject";
        String title = "approved".equals(decision) ? "审核通过" : "审核驳回";
        String defaultComment = "approved".equals(decision) ? "审核通过" : "审核驳回";
        String finalComment = StringUtils.hasText(comment) ? comment : defaultComment;
        saveAudit(entity.getId(), action, before, decision, finalComment);
        saveTraceEvent(entity.getId(), decision, before, decision, title, finalComment, null);
        return toVO(entity);
    }

    private void requireAdmin() {
        if (!SecurityUtils.currentUser().getRoleCodes().contains("ADMIN")) {
            throw new ForbiddenException("只有管理员可以归档生长采集记录");
        }
    }

    private void saveAudit(
            Long recordId, String action, String before, String after, String comment) {
        CurrentUser current = SecurityUtils.currentUser();
        GrowthAuditRecordEntity audit = new GrowthAuditRecordEntity();
        audit.setGrowthRecordId(recordId);
        audit.setReviewerId(current.getUserId());
        audit.setReviewerName(displayName(current));
        audit.setReviewerRole(currentRole(current));
        audit.setReviewAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setReviewComment(comment);
        audit.setReviewedAt(LocalDateTime.now());
        audit.setCreatedBy(current.getUserId());
        growthAuditRecordMapper.insert(audit);
    }

    private void saveTraceEvent(
            Long recordId,
            String eventType,
            String before,
            String after,
            String title,
            String content,
            String metadataJson) {
        CurrentUser current = SecurityUtils.currentUser();
        GrowthTraceEventEntity event = new GrowthTraceEventEntity();
        event.setRecordId(recordId);
        event.setEventType(eventType);
        event.setEventTitle(title);
        event.setEventContent(content);
        event.setBeforeStatus(before);
        event.setAfterStatus(after);
        event.setOperatorId(current.getUserId());
        event.setOperatorName(displayName(current));
        event.setOperatorRole(currentRole(current));
        event.setEventTime(LocalDateTime.now());
        event.setMetadataJson(metadataJson);
        event.setCreatedBy(current.getUserId());
        event.setUpdatedBy(current.getUserId());
        event.setStatus(1);
        growthTraceEventMapper.insert(event);
    }

    private GrowthTraceEventVO legacyEvent(
            String eventType,
            String action,
            String before,
            String after,
            Long operatorId,
            LocalDateTime time,
            String comment) {
        GrowthTraceEventVO vo = new GrowthTraceEventVO();
        vo.setEventType(eventType);
        vo.setAction(action);
        vo.setBeforeStatus(before);
        vo.setAfterStatus(after);
        vo.setOperatorId(operatorId);
        UserEntity user = operatorId == null ? null : userMapper.selectById(operatorId);
        vo.setOperatorName(user == null ? null : user.getRealName());
        vo.setComment(comment);
        vo.setEventTime(time);
        return vo;
    }

    private GrowthAuditHistoryVO toAuditHistoryVO(GrowthAuditRecordEntity audit) {
        GrowthAuditHistoryVO vo = new GrowthAuditHistoryVO();
        vo.setActionType(audit.getReviewAction());
        vo.setBeforeStatus(audit.getBeforeStatus());
        vo.setAfterStatus(audit.getAfterStatus());
        vo.setOperatorId(audit.getReviewerId());
        vo.setOperatorName(resolveOperatorName(audit.getReviewerId(), audit.getReviewerName()));
        vo.setOperatorRole(audit.getReviewerRole());
        vo.setComment(audit.getReviewComment());
        vo.setOperateTime(audit.getReviewedAt());
        return vo;
    }

    private GrowthTraceEventVO toTraceEventVO(GrowthTraceEventEntity event) {
        GrowthTraceEventVO vo = new GrowthTraceEventVO();
        vo.setEventType(event.getEventType());
        vo.setEventTitle(event.getEventTitle());
        vo.setEventContent(event.getEventContent());
        vo.setAction(event.getEventType());
        vo.setBeforeStatus(event.getBeforeStatus());
        vo.setAfterStatus(event.getAfterStatus());
        vo.setOperatorId(event.getOperatorId());
        vo.setOperatorName(resolveOperatorName(event.getOperatorId(), event.getOperatorName()));
        vo.setOperatorRole(event.getOperatorRole());
        vo.setComment(event.getEventContent());
        vo.setEventTime(event.getEventTime());
        vo.setRemark(event.getRemark());
        vo.setMetadataJson(event.getMetadataJson());
        return vo;
    }

    private String buildUpdateMetadata(
            GrowthRecordEntity before, GrowthRecordUpsertRequest request) {
        List<String> changedFields = new ArrayList<>();
        addChange(changedFields, "药材品种", before.getSpeciesId(), request.getSpeciesId());
        addChange(changedFields, "地图点位", before.getDistributionId(), request.getDistributionId());
        addChange(changedFields, "区域", before.getRegionId(), request.getRegionId());
        addChange(changedFields, "经度", before.getLongitude(), request.getLongitude());
        addChange(changedFields, "纬度", before.getLatitude(), request.getLatitude());
        addChange(changedFields, "生长阶段", before.getGrowthStage(), request.getGrowthStage());
        addChange(changedFields, "土壤类型", before.getSoilType(), request.getSoilType());
        addChange(changedFields, "土壤 pH", before.getSoilPh(), request.getSoilPh());
        addChange(changedFields, "温度", before.getTemperature(), request.getTemperature());
        addChange(changedFields, "湿度", before.getHumidity(), request.getHumidity());
        addChange(changedFields, "天气", before.getWeather(), request.getWeather());
        addChange(changedFields, "样本重量", before.getSampleWeight(), request.getSampleWeight());
        addChange(changedFields, "采集时间", before.getCollectedAt(), request.getCollectedAt());
        addChange(changedFields, "备注", before.getRemark(), request.getRemark());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("changedFields", changedFields);
        metadata.put(
                "summary",
                changedFields.isEmpty() ? "保存记录，字段无变化" : String.join("、", changedFields));
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return "{\"summary\":\"修改采集记录\"}";
        }
    }

    private void addChange(List<String> changedFields, String label, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changedFields.add("修改了" + label);
        }
    }

    private String displayName(CurrentUser current) {
        return StringUtils.hasText(current.getRealName())
                ? current.getRealName()
                : current.getUsername();
    }

    private String currentRole(CurrentUser current) {
        return String.join(",", current.getRoleCodes());
    }

    private String resolveOperatorName(Long operatorId, String snapshotName) {
        if (StringUtils.hasText(snapshotName)) {
            return snapshotName;
        }
        UserEntity user = operatorId == null ? null : userMapper.selectById(operatorId);
        return user == null ? null : user.getRealName();
    }

    private GrowthRecordVO toVO(GrowthRecordEntity entity) {
        GrowthRecordVO vo = new GrowthRecordVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCollectorName(entity.getCollectorNameSnapshot());
        HerbEntity species = herbMapper.selectById(entity.getSpeciesId());
        vo.setSpeciesName(
                StringUtils.hasText(entity.getSpeciesName())
                        ? entity.getSpeciesName()
                        : species == null ? null : species.getHerbName());
        if (entity.getBatchId() != null) {
            HerbBatchEntity batch = herbBatchMapper.selectById(entity.getBatchId());
            vo.setBatchName(batch == null ? null : batch.getBatchName());
        }
        if (entity.getTaskId() != null) {
            HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(entity.getTaskId());
            vo.setTaskName(task == null ? null : task.getTaskName());
            vo.setCollectPlace(task == null ? null : task.getCollectPlace());
        }
        return vo;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
