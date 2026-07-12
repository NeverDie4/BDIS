package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceRequest;
import com.bdis.modules.performance.entity.PerformanceAuditEntity;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceAuditMapper;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.query.PerformanceQuery;
import com.bdis.modules.performance.query.PerformanceStatisticsQuery;
import com.bdis.modules.performance.service.PerformanceMaterialService;
import com.bdis.modules.performance.service.PerformanceService;
import com.bdis.modules.performance.vo.PerformanceDetailVO;
import com.bdis.modules.performance.vo.PerformanceStatisticsVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> IDENTIFY_STATUSES =
            Set.of("draft", "submitted", "approved", "rejected");

    private final PerformanceMapper performanceMapper;
    private final PerformanceStandardMapper standardMapper;
    private final PerformanceAuditMapper auditMapper;
    private final PerformanceMaterialService materialService;
    private final BusinessAccessService accessService;

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
    public PerformanceEntity createPerformance(PerformanceRequest request) {
        accessService.requirePermission("performance:record:create");
        Long userId = accessService.currentUserId();
        validateStandard(request.getStandardId());
        PerformanceEntity entity = new PerformanceEntity();
        entity.setPerformanceNo(defaultText(request.getPerformanceNo(), generateNo()));
        entity.setUserId(userId);
        entity.setPerformanceTitle(request.getPerformanceTitle());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardId(request.getStandardId());
        entity.setSourceType(request.getSourceType());
        entity.setSourceId(request.getSourceId());
        entity.setIdentifyStatus("draft");
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setRemark(request.getRemark());
        performanceMapper.insert(entity);
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
        entity.setPerformanceTitle(request.getPerformanceTitle());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardId(request.getStandardId());
        entity.setSourceType(request.getSourceType());
        entity.setSourceId(request.getSourceId());
        entity.setUpdatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        performanceMapper.updateById(entity);
        return performanceMapper.selectById(performanceId);
    }

    @Override
    @Transactional
    public PerformanceEntity submitPerformance(Long performanceId, Long userId) {
        PerformanceEntity entity = findPerformance(performanceId);
        userId = accessService.currentUserId();
        accessService.requireResourceAccess(
                "perf_record", performanceId, "performance:record:submit", entity.getUserId());
        if (!"draft".equals(entity.getIdentifyStatus())
                && !"rejected".equals(entity.getIdentifyStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的业绩可以提交");
        }
        if (userId == null || !userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("提交人必须与业绩所属用户一致");
        }
        entity.setIdentifyStatus("submitted");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        performanceMapper.updateById(entity);
        saveAuditRecord(performanceId, userId, "submit", "submitted", "业绩提交", null);
        return performanceMapper.selectById(performanceId);
    }

    @Override
    public PerformanceStatisticsVO getStatistics(PerformanceStatisticsQuery query) {
        accessService.requirePermission("performance:record:view");
        PerformanceStatisticsQuery safeQuery =
                query == null ? new PerformanceStatisticsQuery() : query;
        LambdaQueryWrapper<PerformanceEntity> wrapper = buildStatisticsWrapper(safeQuery);
        accessService.applyOwnerScope(wrapper, "perf_record", PerformanceEntity::getUserId);
        List<PerformanceEntity> records = performanceMapper.selectList(wrapper);
        PerformanceStatisticsVO statistics = new PerformanceStatisticsVO();
        statistics.setTotalCount((long) records.size());
        statistics.setDraftCount(countStatus(records, "draft"));
        statistics.setSubmittedCount(countStatus(records, "submitted"));
        statistics.setApprovedCount(countStatus(records, "approved"));
        statistics.setRejectedCount(countStatus(records, "rejected"));
        statistics.setTypeCounts(countByType(records));
        return statistics;
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
                        query.getSourceType());
    }

    private LambdaQueryWrapper<PerformanceEntity> buildStatisticsWrapper(
            PerformanceStatisticsQuery query) {
        return new LambdaQueryWrapper<PerformanceEntity>()
                .eq(query.getUserId() != null, PerformanceEntity::getUserId, query.getUserId())
                .eq(
                        StringUtils.hasText(query.getPerformanceType()),
                        PerformanceEntity::getPerformanceType,
                        query.getPerformanceType())
                .eq(
                        StringUtils.hasText(query.getIdentifyStatus()),
                        PerformanceEntity::getIdentifyStatus,
                        query.getIdentifyStatus());
    }

    private Long countStatus(List<PerformanceEntity> records, String status) {
        return records.stream().filter(record -> status.equals(record.getIdentifyStatus())).count();
    }

    private Map<String, Long> countByType(List<PerformanceEntity> records) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PerformanceEntity record : records) {
            String type = defaultText(record.getPerformanceType(), "unknown");
            counts.put(type, counts.getOrDefault(type, 0L) + 1);
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

    private void assertIdentifyStatus(String identifyStatus) {
        if (!IDENTIFY_STATUSES.contains(identifyStatus)) {
            throw new IllegalArgumentException("认定状态只能是 draft、submitted、approved、rejected");
        }
    }

    private Page<PerformanceEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "PERF-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
