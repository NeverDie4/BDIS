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
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.growth.dto.GrowthAuditRequest;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.entity.GrowthAuditRecordEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.mapper.GrowthAuditRecordMapper;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.query.GrowthRecordQuery;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.GrowthTraceEventVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GrowthRecordServiceImpl implements GrowthRecordService {

    private static final String RESOURCE_TYPE = "herb_growth_record";

    private final GrowthRecordMapper growthRecordMapper;
    private final GrowthAuditRecordMapper growthAuditRecordMapper;
    private final MapPointMapper mapPointMapper;
    private final HerbMapper herbMapper;
    private final HerbImageMapper herbImageMapper;
    private final UserMapper userMapper;
    private final DataScopeService dataScopeService;
    private final DictionaryReferenceValidator dictionaryReferenceValidator;

    public GrowthRecordServiceImpl(
            GrowthRecordMapper growthRecordMapper,
            GrowthAuditRecordMapper growthAuditRecordMapper,
            MapPointMapper mapPointMapper,
            HerbMapper herbMapper,
            HerbImageMapper herbImageMapper,
            UserMapper userMapper,
            DataScopeService dataScopeService,
            DictionaryReferenceValidator dictionaryReferenceValidator) {
        this.growthRecordMapper = growthRecordMapper;
        this.growthAuditRecordMapper = growthAuditRecordMapper;
        this.mapPointMapper = mapPointMapper;
        this.herbMapper = herbMapper;
        this.herbImageMapper = herbImageMapper;
        this.userMapper = userMapper;
        this.dataScopeService = dataScopeService;
        this.dictionaryReferenceValidator = dictionaryReferenceValidator;
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
        point.setLastCollectedAt(record.getCollectedAt());
        point.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        mapPointMapper.updateById(point);
        return toVO(record);
    }

    @Override
    public PageResult<GrowthRecordVO> page(GrowthRecordQuery query) {
        LambdaQueryWrapper<GrowthRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(
                        query.getSpeciesId() != null,
                        GrowthRecordEntity::getSpeciesId,
                        query.getSpeciesId())
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
        GrowthRecordVO vo = toVO(entity);
        vo.setImages(herbImageMapper.selectByGrowthRecordId(id));
        return vo;
    }

    @Override
    @Transactional
    public GrowthRecordVO create(GrowthRecordUpsertRequest request) {
        validateReferences(request);
        GrowthRecordEntity entity = new GrowthRecordEntity();
        apply(entity, request);
        initializeNewRecord(entity, SecurityUtils.currentUser().getRealName());
        growthRecordMapper.insert(entity);
        return toVO(entity);
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
        validateReferences(request);
        apply(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
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
        saveAudit(entity.getId(), "submit", before, "submitted", "提交审核");
        return toVO(entity);
    }

    @Override
    @Transactional
    public GrowthRecordVO audit(Long id, GrowthAuditRequest request) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        if (!"submitted".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "只有已提交记录可以审核");
        }
        String before = entity.getReviewStatus();
        entity.setReviewStatus(request.getDecision());
        entity.setReviewedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        saveAudit(
                entity.getId(),
                request.getDecision(),
                before,
                request.getDecision(),
                request.getComment());
        return toVO(entity);
    }

    @Override
    @Transactional
    public GrowthRecordVO archive(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        if (!"approved".equals(entity.getReviewStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "只有审核通过记录可以归档");
        }
        entity.setReviewStatus("archived");
        entity.setArchivedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        growthRecordMapper.updateById(entity);
        saveAudit(entity.getId(), "archive", "approved", "archived", "归档");
        return toVO(entity);
    }

    @Override
    public List<GrowthTraceEventVO> trace(Long id) {
        GrowthRecordEntity entity = requireRecord(id);
        requireAccess(entity);
        List<GrowthTraceEventVO> events = new ArrayList<>();
        events.add(
                event(
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
                                events.add(
                                        event(
                                                "status_change",
                                                audit.getReviewAction(),
                                                audit.getBeforeStatus(),
                                                audit.getAfterStatus(),
                                                audit.getReviewerId(),
                                                audit.getReviewedAt(),
                                                audit.getReviewComment())));
        events.sort(Comparator.comparing(GrowthTraceEventVO::getEventTime));
        return events;
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

    private void apply(GrowthRecordEntity entity, GrowthRecordUpsertRequest request) {
        entity.setSpeciesId(request.getSpeciesId());
        entity.setDistributionId(request.getDistributionId());
        entity.setRegionId(request.getRegionId());
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setGrowthStage(request.getGrowthStage());
        entity.setSoilType(request.getSoilType());
        entity.setSoilPh(request.getSoilPh());
        entity.setTemperature(request.getTemperature());
        entity.setHumidity(request.getHumidity());
        entity.setWeather(request.getWeather());
        entity.setSampleWeight(request.getSampleWeight());
        entity.setDeviceType(defaultText(request.getDeviceType(), "web"));
        entity.setDataSource(defaultText(request.getDataSource(), "manual"));
        entity.setCollectedAt(request.getCollectedAt());
        entity.setRemark(request.getRemark());
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

    private void requireOwner(GrowthRecordEntity entity) {
        if (!SecurityUtils.currentUser().getUserId().equals(entity.getCollectorId())
                && !dataScopeService.resolveForCurrentUser(RESOURCE_TYPE).isAllIncluded()) {
            throw new ForbiddenException("只能操作本人采集记录");
        }
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

    private void saveAudit(
            Long recordId, String action, String before, String after, String comment) {
        GrowthAuditRecordEntity audit = new GrowthAuditRecordEntity();
        audit.setGrowthRecordId(recordId);
        audit.setReviewerId(SecurityUtils.currentUser().getUserId());
        audit.setReviewAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setReviewComment(comment);
        audit.setReviewedAt(LocalDateTime.now());
        audit.setCreatedBy(SecurityUtils.currentUser().getUserId());
        growthAuditRecordMapper.insert(audit);
    }

    private GrowthTraceEventVO event(
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

    private GrowthRecordVO toVO(GrowthRecordEntity entity) {
        GrowthRecordVO vo = new GrowthRecordVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCollectorName(entity.getCollectorNameSnapshot());
        HerbEntity species = herbMapper.selectById(entity.getSpeciesId());
        vo.setSpeciesName(species == null ? null : species.getHerbName());
        return vo;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
