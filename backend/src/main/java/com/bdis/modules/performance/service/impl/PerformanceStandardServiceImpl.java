package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceStandardRequest;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.query.PerformanceStandardQuery;
import com.bdis.modules.performance.service.PerformanceStandardService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceStandardServiceImpl implements PerformanceStandardService {

    private static final long MAX_PAGE_SIZE = 100L;

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PerformanceStandardMapper standardMapper;
    private final BusinessAccessService accessService;

    @Override
    public IPage<PerformanceStandardEntity> listStandards(PerformanceStandardQuery query) {
        accessService.requirePermission("performance:standard:view");
        PerformanceStandardQuery safeQuery = query == null ? new PerformanceStandardQuery() : query;
        LambdaQueryWrapper<PerformanceStandardEntity> wrapper =
                new LambdaQueryWrapper<PerformanceStandardEntity>()
                        .like(
                                StringUtils.hasText(safeQuery.getKeyword()),
                                PerformanceStandardEntity::getStandardName,
                                safeQuery.getKeyword())
                        .eq(
                                StringUtils.hasText(safeQuery.getPerformanceType()),
                                PerformanceStandardEntity::getPerformanceType,
                                safeQuery.getPerformanceType())
                        .eq(
                                safeQuery.getStatus() != null,
                                PerformanceStandardEntity::getStatus,
                                safeQuery.getStatus())
                        .eq(
                                StringUtils.hasText(safeQuery.getLifecycleStatus()),
                                PerformanceStandardEntity::getLifecycleStatus,
                                safeQuery.getLifecycleStatus())
                        .orderByAsc(PerformanceStandardEntity::getSortOrder)
                        .orderByDesc(PerformanceStandardEntity::getUpdatedAt);
        return standardMapper.selectPage(
                page(safeQuery.getPageNum(), safeQuery.getPageSize()), wrapper);
    }

    @Override
    public PerformanceStandardEntity createStandard(PerformanceStandardRequest request) {
        accessService.requirePermission("performance:standard:manage");
        PerformanceStandardEntity entity = new PerformanceStandardEntity();
        entity.setStandardNo(defaultText(request.getStandardNo(), generateNo()));
        entity.setStandardVersion(1);
        entity.setStandardName(request.getStandardName());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardDesc(request.getStandardDesc());
        entity.setScoreRule(request.getScoreRule());
        entity.setLevelRule(request.getLevelRule());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setMaterialRequired(Boolean.FALSE.equals(request.getMaterialRequired()) ? 0 : 1);
        entity.setMinMaterialCount(resolveMinMaterialCount(request));
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(1);
        entity.setLifecycleStatus("draft");
        entity.setCreatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        insertVersionOrThrow(entity);
        return standardMapper.selectById(entity.getId());
    }

    @Override
    public PerformanceStandardEntity updateStandard(
            Long standardId, PerformanceStandardRequest request) {
        accessService.requirePermission("performance:standard:manage");
        PerformanceStandardEntity entity = standardMapper.selectById(standardId);
        if (entity == null) {
            throw new IllegalArgumentException("认定标准不存在");
        }
        if (!"draft".equals(entity.getLifecycleStatus())) {
            throw new IllegalArgumentException("已发布或已停用的标准不能修改，请创建新版本");
        }
        entity.setStandardName(request.getStandardName());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardDesc(request.getStandardDesc());
        entity.setScoreRule(request.getScoreRule());
        entity.setLevelRule(request.getLevelRule());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setMaterialRequired(Boolean.FALSE.equals(request.getMaterialRequired()) ? 0 : 1);
        entity.setMinMaterialCount(resolveMinMaterialCount(request));
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setUpdatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        updateOrThrow(entity);
        return standardMapper.selectById(standardId);
    }

    @Override
    public PerformanceStandardEntity createVersion(
            Long standardId, PerformanceStandardRequest request) {
        accessService.requirePermission("performance:standard:manage");
        PerformanceStandardEntity source = standardMapper.selectById(standardId);
        if (source == null) {
            throw new IllegalArgumentException("认定标准不存在");
        }
        if ("draft".equals(source.getLifecycleStatus())) {
            throw new IllegalArgumentException("草稿标准应直接编辑，发布或停用后才能创建新版本");
        }
        Integer maxVersion =
                standardMapper
                        .selectList(
                                new LambdaQueryWrapper<PerformanceStandardEntity>()
                                        .eq(
                                                PerformanceStandardEntity::getStandardNo,
                                                source.getStandardNo()))
                        .stream()
                        .map(PerformanceStandardEntity::getStandardVersion)
                        .filter(java.util.Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0);
        PerformanceStandardEntity entity = new PerformanceStandardEntity();
        entity.setStandardNo(source.getStandardNo());
        entity.setStandardVersion(maxVersion + 1);
        entity.setStandardName(request.getStandardName());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardDesc(request.getStandardDesc());
        entity.setScoreRule(request.getScoreRule());
        entity.setLevelRule(request.getLevelRule());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setMaterialRequired(Boolean.FALSE.equals(request.getMaterialRequired()) ? 0 : 1);
        entity.setMinMaterialCount(resolveMinMaterialCount(request));
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(1);
        entity.setLifecycleStatus("draft");
        entity.setCreatedBy(accessService.currentUserId());
        entity.setRemark(request.getRemark());
        insertVersionOrThrow(entity);
        return standardMapper.selectById(entity.getId());
    }

    @Override
    public PerformanceStandardEntity publishStandard(Long standardId) {
        accessService.requirePermission("performance:standard:manage");
        PerformanceStandardEntity entity = requireStandard(standardId);
        if (!"draft".equals(entity.getLifecycleStatus())) {
            throw new IllegalArgumentException("只有草稿状态的标准可以发布");
        }
        if (entity.getEffectiveFrom() != null
                && entity.getEffectiveTo() != null
                && entity.getEffectiveTo().isBefore(entity.getEffectiveFrom())) {
            throw new IllegalArgumentException("标准适用结束时间不能早于开始时间");
        }
        entity.setLifecycleStatus("published");
        entity.setPublishedAt(LocalDateTime.now());
        entity.setPublishedBy(accessService.currentUserId());
        entity.setUpdatedBy(accessService.currentUserId());
        updateOrThrow(entity);
        return standardMapper.selectById(standardId);
    }

    @Override
    public PerformanceStandardEntity disableStandard(Long standardId) {
        accessService.requirePermission("performance:standard:manage");
        PerformanceStandardEntity entity = requireStandard(standardId);
        if (!"published".equals(entity.getLifecycleStatus())) {
            throw new IllegalArgumentException("只有已发布的标准可以停用");
        }
        entity.setLifecycleStatus("disabled");
        entity.setUpdatedBy(accessService.currentUserId());
        updateOrThrow(entity);
        return standardMapper.selectById(standardId);
    }

    private Page<PerformanceStandardEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE));
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private int resolveMinMaterialCount(PerformanceStandardRequest request) {
        if (Boolean.FALSE.equals(request.getMaterialRequired())) {
            return 0;
        }
        if (request.getMinMaterialCount() != null && request.getMinMaterialCount() < 1) {
            throw new IllegalArgumentException("要求佐证材料时，最少材料数量必须大于 0");
        }
        return request.getMinMaterialCount() == null ? 1 : request.getMinMaterialCount();
    }

    private PerformanceStandardEntity requireStandard(Long standardId) {
        PerformanceStandardEntity entity = standardMapper.selectById(standardId);
        if (entity == null) {
            throw new IllegalArgumentException("认定标准不存在");
        }
        return entity;
    }

    private void insertVersionOrThrow(PerformanceStandardEntity entity) {
        try {
            standardMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "标准版本已被其他请求创建，请刷新后重试");
        }
    }

    private void updateOrThrow(PerformanceStandardEntity entity) {
        if (standardMapper.updateById(entity) != 1) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "标准已被其他请求修改，请刷新后重试");
        }
    }

    private String generateNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "PSTD-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
