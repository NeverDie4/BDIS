package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.common.security.BusinessReferenceAccessService;
import com.bdis.modules.performance.dto.PerformanceRequest;
import com.bdis.modules.performance.entity.PerformanceAuditEntity;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.mapper.PerformanceAuditMapper;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceParticipantMapper;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.query.PerformanceQuery;
import com.bdis.modules.performance.query.PerformanceStatisticsQuery;
import com.bdis.modules.performance.service.PerformanceMaterialService;
import com.bdis.modules.performance.service.PerformanceService;
import com.bdis.modules.performance.vo.PerformanceDetailVO;
import com.bdis.modules.performance.vo.PerformanceParticipantVO;
import com.bdis.modules.performance.vo.PerformanceStatisticsVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private static final long MAX_PAGE_SIZE = 100L;

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> IDENTIFY_STATUSES =
            Set.of("draft", "submitted", "approved", "rejected");

    private final PerformanceMapper performanceMapper;
    private final PerformanceStandardMapper standardMapper;
    private final PerformanceParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final PerformanceAuditMapper auditMapper;
    private final PerformanceMaterialService materialService;
    private final BusinessAccessService accessService;
    private final BusinessReferenceAccessService referenceAccessService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public IPage<PerformanceEntity> listPerformances(PerformanceQuery query) {
        PerformanceQuery safeQuery = query == null ? new PerformanceQuery() : query;
        LambdaQueryWrapper<PerformanceEntity> wrapper =
                buildListWrapper(safeQuery).orderByDesc(PerformanceEntity::getUpdatedAt);
        accessService.requirePermission("performance:record:view");
        accessService.applyOwnerScope(wrapper, "perf_record", PerformanceEntity::getUserId);
        return performanceMapper.selectPage(
                page(safeQuery.getPageNum(), safeQuery.getPageSize()), wrapper);
    }

    @Override
    @Transactional
    public PerformanceEntity createPerformance(PerformanceRequest request) {
        accessService.requirePermission("performance:record:create");
        Long userId = accessService.currentUserId();
        validateStandard(request.getStandardId());
        validateSource(request.getSourceType(), request.getSourceId());
        PerformanceEntity entity = new PerformanceEntity();
        entity.setPerformanceNo(defaultText(request.getPerformanceNo(), generateNo()));
        entity.setUserId(userId);
        entity.setPerformanceTitle(request.getPerformanceTitle());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setPerformanceLevel(defaultText(request.getPerformanceLevel(), "unspecified"));
        entity.setOccurredAt(request.getOccurredAt());
        entity.setStandardId(request.getStandardId());
        entity.setSourceType(request.getSourceType());
        entity.setSourceId(request.getSourceId());
        entity.setSourceNameSnapshot(
                resolveSourceName(request.getSourceType(), request.getSourceId()));
        entity.setIdentifyStatus("draft");
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setRemark(request.getRemark());
        performanceMapper.insert(entity);
        createOwnerParticipant(entity, userId);
        return performanceMapper.selectById(entity.getId());
    }

    @Override
    public PerformanceDetailVO getPerformanceDetail(Long performanceId) {
        PerformanceEntity performance = findPerformance(performanceId);
        accessService.requireResourceAccess(
                "perf_record", performanceId, "performance:record:view", performance.getUserId());
        PerformanceDetailVO detail = new PerformanceDetailVO();
        detail.setPerformance(performance);
        detail.setStandard(
                performance.getStandardId() == null
                        ? null
                        : standardMapper.selectById(performance.getStandardId()));
        detail.setMaterials(materialService.listMaterials(performanceId));
        List<PerformanceParticipantEntity> participants =
                participantMapper.selectList(
                        new LambdaQueryWrapper<PerformanceParticipantEntity>()
                                .eq(PerformanceParticipantEntity::getPerformanceId, performanceId)
                                .orderByDesc(PerformanceParticipantEntity::getIsPrimary)
                                .orderByAsc(PerformanceParticipantEntity::getSortOrder)
                                .orderByAsc(PerformanceParticipantEntity::getId));
        Map<Long, UserEntity> usersById =
                participants.isEmpty()
                        ? Map.of()
                        : userMapper
                                .selectBatchIds(
                                        participants.stream()
                                                .map(PerformanceParticipantEntity::getUserId)
                                                .distinct()
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        detail.setParticipants(
                participants.stream()
                        .map(
                                participant ->
                                        PerformanceParticipantVO.from(
                                                participant,
                                                usersById.get(participant.getUserId())))
                        .toList());
        detail.setAuditRecords(
                auditMapper.selectList(
                        new LambdaQueryWrapper<PerformanceAuditEntity>()
                                .eq(PerformanceAuditEntity::getPerformanceId, performanceId)
                                .orderByDesc(PerformanceAuditEntity::getIdentifiedAt)));
        return detail;
    }

    @Override
    public PerformanceEntity updatePerformance(Long performanceId, PerformanceRequest request) {
        PerformanceEntity entity = findPerformance(performanceId);
        accessService.requireResourceAccess(
                "perf_record", performanceId, "performance:record:update", entity.getUserId());
        if (!"draft".equals(entity.getIdentifyStatus())
                && !"rejected".equals(entity.getIdentifyStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的业绩可以修改");
        }
        validateStandard(request.getStandardId());
        validateSource(request.getSourceType(), request.getSourceId());
        entity.setPerformanceTitle(request.getPerformanceTitle());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setPerformanceLevel(defaultText(request.getPerformanceLevel(), "unspecified"));
        entity.setOccurredAt(request.getOccurredAt());
        entity.setStandardId(request.getStandardId());
        entity.setSourceType(request.getSourceType());
        entity.setSourceId(request.getSourceId());
        entity.setSourceNameSnapshot(
                resolveSourceName(request.getSourceType(), request.getSourceId()));
        entity.setUpdatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        updateOrThrow(entity, "业绩已被其他请求修改，请刷新后重试");
        return performanceMapper.selectById(performanceId);
    }

    @Override
    @Transactional
    public PerformanceEntity submitPerformance(Long performanceId) {
        PerformanceEntity entity = findPerformance(performanceId);
        Long userId = accessService.currentUserId();
        accessService.requireResourceAccess(
                "perf_record", performanceId, "performance:record:submit", entity.getUserId());
        if (!"draft".equals(entity.getIdentifyStatus())
                && !"rejected".equals(entity.getIdentifyStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的业绩可以提交");
        }
        if (userId == null || !userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("提交人必须与业绩所属用户一致");
        }
        PerformanceStandardEntity standard = validateSubmittableStandard(entity.getStandardId());
        int materialCount = materialService.listMaterials(performanceId).size();
        int minMaterialCount =
                Integer.valueOf(1).equals(standard.getMaterialRequired())
                        ? standard.getMinMaterialCount() == null
                                ? 1
                                : standard.getMinMaterialCount()
                        : 0;
        if (materialCount < minMaterialCount) {
            throw new IllegalArgumentException("佐证材料不足，当前标准至少需要 " + minMaterialCount + " 份材料");
        }
        snapshotStandard(entity, standard);
        entity.setIdentifyStatus("submitted");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        updateOrThrow(entity, "业绩状态已变更，请刷新后重试");
        saveAuditRecord(performanceId, userId, "submit", "submitted", "业绩提交", null);
        return performanceMapper.selectById(performanceId);
    }

    @Override
    public PerformanceStatisticsVO getStatistics(PerformanceStatisticsQuery query) {
        accessService.requirePermission("performance:record:view");
        PerformanceStatisticsQuery safeQuery =
                query == null ? new PerformanceStatisticsQuery() : query;
        QueryWrapper<PerformanceEntity> wrapper = statisticsWrapper(safeQuery);
        PerformanceStatisticsVO statistics = new PerformanceStatisticsVO();
        statistics.setTotalCount(performanceMapper.selectCount(wrapper));
        Map<String, Long> statusCounts = groupCounts(safeQuery, "identify_status");
        statistics.setDraftCount(statusCounts.getOrDefault("draft", 0L));
        statistics.setSubmittedCount(statusCounts.getOrDefault("submitted", 0L));
        statistics.setApprovedCount(statusCounts.getOrDefault("approved", 0L));
        statistics.setRejectedCount(statusCounts.getOrDefault("rejected", 0L));
        statistics.setTypeCounts(groupCounts(safeQuery, "performance_type"));
        return statistics;
    }

    private QueryWrapper<PerformanceEntity> statisticsWrapper(
            PerformanceStatisticsQuery safeQuery) {
        QueryWrapper<PerformanceEntity> wrapper = buildStatisticsWrapper(safeQuery);
        if (safeQuery.getParticipantUserId() != null) {
            List<Long> performanceIds =
                    participantMapper
                            .selectObjs(
                                    new LambdaQueryWrapper<PerformanceParticipantEntity>()
                                            .select(PerformanceParticipantEntity::getPerformanceId)
                                            .eq(
                                                    PerformanceParticipantEntity::getUserId,
                                                    safeQuery.getParticipantUserId()))
                            .stream()
                            .map(value -> ((Number) value).longValue())
                            .distinct()
                            .toList();
            if (performanceIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in("id", performanceIds);
            }
        }
        accessService.applyOwnerScope(wrapper, "perf_record", "user_id");
        return wrapper;
    }

    private LambdaQueryWrapper<PerformanceEntity> buildListWrapper(PerformanceQuery query) {
        return new LambdaQueryWrapper<PerformanceEntity>()
                .like(
                        StringUtils.hasText(query.getKeyword()),
                        PerformanceEntity::getPerformanceTitle,
                        query.getKeyword())
                .eq(query.getUserId() != null, PerformanceEntity::getUserId, query.getUserId())
                .eq(
                        StringUtils.hasText(query.getPerformanceType()),
                        PerformanceEntity::getPerformanceType,
                        query.getPerformanceType())
                .eq(
                        StringUtils.hasText(query.getPerformanceLevel()),
                        PerformanceEntity::getPerformanceLevel,
                        query.getPerformanceLevel())
                .eq(
                        StringUtils.hasText(query.getIdentifyStatus()),
                        PerformanceEntity::getIdentifyStatus,
                        query.getIdentifyStatus())
                .eq(
                        query.getStandardId() != null,
                        PerformanceEntity::getStandardId,
                        query.getStandardId())
                .eq(
                        StringUtils.hasText(query.getSourceType()),
                        PerformanceEntity::getSourceType,
                        query.getSourceType())
                .ge(
                        query.getOccurredFrom() != null,
                        PerformanceEntity::getOccurredAt,
                        query.getOccurredFrom())
                .le(
                        query.getOccurredTo() != null,
                        PerformanceEntity::getOccurredAt,
                        query.getOccurredTo());
    }

    private QueryWrapper<PerformanceEntity> buildStatisticsWrapper(
            PerformanceStatisticsQuery query) {
        return new QueryWrapper<PerformanceEntity>()
                .eq(query.getUserId() != null, "user_id", query.getUserId())
                .eq(
                        StringUtils.hasText(query.getPerformanceType()),
                        "performance_type",
                        query.getPerformanceType())
                .eq(
                        StringUtils.hasText(query.getPerformanceLevel()),
                        "performance_level",
                        query.getPerformanceLevel())
                .eq(
                        StringUtils.hasText(query.getIdentifyStatus()),
                        "identify_status",
                        query.getIdentifyStatus())
                .ge(query.getOccurredFrom() != null, "occurred_at", query.getOccurredFrom())
                .le(query.getOccurredTo() != null, "occurred_at", query.getOccurredTo());
    }

    private Map<String, Long> groupCounts(PerformanceStatisticsQuery query, String column) {
        QueryWrapper<PerformanceEntity> wrapper = statisticsWrapper(query);
        wrapper.select(column + " as bucket", "count(*) as total").groupBy(column);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : performanceMapper.selectMaps(wrapper)) {
            Object bucket = row.get("bucket");
            Object total = row.get("total");
            String key =
                    bucket == null || bucket.toString().isBlank() ? "unknown" : bucket.toString();
            counts.put(key, total instanceof Number number ? number.longValue() : 0L);
        }
        return counts;
    }

    private void saveAuditRecord(
            Long performanceId,
            Long identifierId,
            String action,
            String result,
            String comment,
            String remark) {
        PerformanceAuditEntity audit = new PerformanceAuditEntity();
        audit.setPerformanceId(performanceId);
        audit.setIdentifierId(identifierId);
        audit.setIdentifyAction(action);
        audit.setIdentifyResult(result);
        audit.setIdentifyComment(comment);
        audit.setIdentifiedAt(LocalDateTime.now());
        audit.setCreatedBy(identifierId);
        audit.setRemark(remark);
        auditMapper.insert(audit);
    }

    private PerformanceEntity findPerformance(Long performanceId) {
        PerformanceEntity entity = performanceMapper.selectById(performanceId);
        if (entity == null) {
            throw new IllegalArgumentException("业绩记录不存在");
        }
        return entity;
    }

    private void validateStandard(Long standardId) {
        if (standardId != null && standardMapper.selectById(standardId) == null) {
            throw new IllegalArgumentException("认定标准不存在");
        }
    }

    private PerformanceStandardEntity validateSubmittableStandard(Long standardId) {
        if (standardId == null) {
            throw new IllegalArgumentException("提交业绩必须选择已发布的认定标准");
        }
        PerformanceStandardEntity standard = standardMapper.selectById(standardId);
        if (standard == null) {
            throw new IllegalArgumentException("认定标准不存在");
        }
        if (!"published".equals(standard.getLifecycleStatus())) {
            throw new IllegalArgumentException("认定标准未发布或已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if ((standard.getEffectiveFrom() != null && now.isBefore(standard.getEffectiveFrom()))
                || (standard.getEffectiveTo() != null && now.isAfter(standard.getEffectiveTo()))) {
            throw new IllegalArgumentException("认定标准当前不在适用期内");
        }
        return standard;
    }

    private void snapshotStandard(
            PerformanceEntity performance, PerformanceStandardEntity standard) {
        performance.setStandardNoSnapshot(standard.getStandardNo());
        performance.setStandardVersionSnapshot(standard.getStandardVersion());
        performance.setStandardNameSnapshot(standard.getStandardName());
        boolean materialRequired = Integer.valueOf(1).equals(standard.getMaterialRequired());
        int minMaterialCount =
                standard.getMinMaterialCount() == null ? 0 : standard.getMinMaterialCount();
        performance.setStandardRuleSnapshot(
                String.format(
                        "认定规则：%s%n等级规则：%s%n佐证材料：%s",
                        defaultText(standard.getScoreRule(), ""),
                        defaultText(standard.getLevelRule(), ""),
                        materialRequired
                                ? "至少需要 " + Math.max(1, minMaterialCount) + " 份材料"
                                : "无需提供材料"));
    }

    private void createOwnerParticipant(PerformanceEntity performance, Long userId) {
        PerformanceParticipantEntity participant = new PerformanceParticipantEntity();
        participant.setPerformanceId(performance.getId());
        participant.setUserId(userId);
        participant.setParticipantRole("owner");
        participant.setSortOrder(0);
        participant.setIsPrimary(1);
        participant.setStatus(1);
        participant.setCreatedBy(userId);
        participant.setRemark("业绩负责人");
        participantMapper.insert(participant);
    }

    private void validateSource(String sourceType, Long sourceId) {
        if (!StringUtils.hasText(sourceType) && sourceId == null) {
            return;
        }
        if (!StringUtils.hasText(sourceType) || sourceId == null) {
            throw new IllegalArgumentException("来源类型和来源 ID 必须同时提供");
        }
        if (!Set.of(
                        "eval_application",
                        "research_project",
                        "edu_course",
                        "edu_experiment_record",
                        "edu_training_plan")
                .contains(sourceType)) {
            throw new IllegalArgumentException("业绩不支持该来源类型");
        }
        referenceAccessService.validatePerformanceSource(sourceType, sourceId);
    }

    private String resolveSourceName(String sourceType, Long sourceId) {
        if (!StringUtils.hasText(sourceType) && sourceId == null) {
            return null;
        }
        return switch (sourceType) {
            case "eval_application" ->
                    sourceTitle(
                            "select application_title from eval_application where id = ?",
                            sourceId);
            case "research_project" ->
                    sourceTitle("select project_name from research_project where id = ?", sourceId);
            case "edu_course" ->
                    sourceTitle("select course_name from edu_course where id = ?", sourceId);
            case "edu_experiment_record" ->
                    sourceTitle(
                            "select experiment_title from edu_experiment_record where id = ?",
                            sourceId);
            case "edu_training_plan" ->
                    sourceTitle("select plan_name from edu_training_plan where id = ?", sourceId);
            default -> null;
        };
    }

    private String sourceTitle(String sql, Long sourceId) {
        List<String> values = jdbcTemplate.queryForList(sql, String.class, sourceId);
        return values.isEmpty() ? null : values.getFirst();
    }

    private void updateOrThrow(PerformanceEntity entity, String message) {
        if (performanceMapper.updateById(entity) != 1) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, message);
        }
    }

    private void assertIdentifyStatus(String identifyStatus) {
        if (!IDENTIFY_STATUSES.contains(identifyStatus)) {
            throw new IllegalArgumentException("认定状态只能是 draft、submitted、approved、rejected");
        }
    }

    private Page<PerformanceEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE));
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "PERF-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
