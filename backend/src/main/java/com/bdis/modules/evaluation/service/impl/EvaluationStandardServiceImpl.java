package com.bdis.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.evaluation.dto.EvaluationStandardRequest;
import com.bdis.modules.evaluation.entity.EvaluationIndicatorEntity;
import com.bdis.modules.evaluation.mapper.EvaluationIndicatorMapper;
import com.bdis.modules.evaluation.query.EvaluationStandardQuery;
import com.bdis.modules.evaluation.service.EvaluationStandardService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EvaluationStandardServiceImpl implements EvaluationStandardService {

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EvaluationIndicatorMapper indicatorMapper;

    private final BusinessAccessService accessService;

    @Override
    public IPage<EvaluationIndicatorEntity> listStandards(EvaluationStandardQuery query) {
        accessService.requirePermission("evaluation:standard:view");
        EvaluationStandardQuery safeQuery = query == null ? new EvaluationStandardQuery() : query;
        LambdaQueryWrapper<EvaluationIndicatorEntity> wrapper =
                new LambdaQueryWrapper<EvaluationIndicatorEntity>()
                        .like(
                                StringUtils.hasText(safeQuery.getKeyword()),
                                EvaluationIndicatorEntity::getIndicatorName,
                                safeQuery.getKeyword())
                        .eq(
                                StringUtils.hasText(safeQuery.getIndicatorType()),
                                EvaluationIndicatorEntity::getIndicatorType,
                                safeQuery.getIndicatorType())
                        .eq(
                                safeQuery.getStatus() != null,
                                EvaluationIndicatorEntity::getStatus,
                                safeQuery.getStatus())
                        .orderByAsc(EvaluationIndicatorEntity::getSortOrder)
                        .orderByDesc(EvaluationIndicatorEntity::getUpdatedAt);
        return indicatorMapper.selectPage(
                page(safeQuery.getPageNum(), safeQuery.getPageSize()), wrapper);
    }

    @Override
    public EvaluationIndicatorEntity createStandard(EvaluationStandardRequest request) {
        accessService.requirePermission("evaluation:standard:manage");
        EvaluationIndicatorEntity entity = new EvaluationIndicatorEntity();
        fillEntity(entity, request);
        entity.setIndicatorNo(defaultText(request.getIndicatorNo(), generateNo("EVAL-IND")));
        entity.setCreatedBy(accessService.currentUserId());
        indicatorMapper.insert(entity);
        return indicatorMapper.selectById(entity.getId());
    }

    @Override
    public EvaluationIndicatorEntity updateStandard(
            Long standardId, EvaluationStandardRequest request) {
        accessService.requirePermission("evaluation:standard:manage");
        EvaluationIndicatorEntity entity = indicatorMapper.selectById(standardId);
        if (entity == null) {
            throw new IllegalArgumentException("评价标准不存在");
        }
        fillEntity(entity, request);
        entity.setUpdatedBy(accessService.currentUserId());
        if (StringUtils.hasText(request.getIndicatorNo())) {
            entity.setIndicatorNo(request.getIndicatorNo());
        }
        indicatorMapper.updateById(entity);
        return indicatorMapper.selectById(standardId);
    }

    private void fillEntity(EvaluationIndicatorEntity entity, EvaluationStandardRequest request) {
        entity.setIndicatorName(request.getIndicatorName());
        entity.setIndicatorType(request.getIndicatorType());
        entity.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        entity.setWeight(request.getWeight() == null ? BigDecimal.ZERO : request.getWeight());
        entity.setMaxScore(
                request.getMaxScore() == null ? new BigDecimal("100.00") : request.getMaxScore());
        entity.setScoreDesc(request.getScoreDesc());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
    }

    private Page<EvaluationIndicatorEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix + "-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
