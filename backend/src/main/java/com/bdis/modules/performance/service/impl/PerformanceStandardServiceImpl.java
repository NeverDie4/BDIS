package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.modules.performance.dto.PerformanceStandardRequest;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.query.PerformanceStandardQuery;
import com.bdis.modules.performance.service.PerformanceStandardService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceStandardServiceImpl implements PerformanceStandardService {

    private static final DateTimeFormatter NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PerformanceStandardMapper standardMapper;

    @Override
    public IPage<PerformanceStandardEntity> listStandards(PerformanceStandardQuery query) {
        PerformanceStandardQuery safeQuery = query == null ? new PerformanceStandardQuery() : query;
        LambdaQueryWrapper<PerformanceStandardEntity> wrapper =
                new LambdaQueryWrapper<PerformanceStandardEntity>()
                        .like(StringUtils.hasText(safeQuery.getKeyword()), PerformanceStandardEntity::getStandardName, safeQuery.getKeyword())
                        .eq(StringUtils.hasText(safeQuery.getPerformanceType()), PerformanceStandardEntity::getPerformanceType, safeQuery.getPerformanceType())
                        .eq(safeQuery.getStatus() != null, PerformanceStandardEntity::getStatus, safeQuery.getStatus())
                        .orderByAsc(PerformanceStandardEntity::getSortOrder)
                        .orderByDesc(PerformanceStandardEntity::getUpdatedAt);
        return standardMapper.selectPage(page(safeQuery.getPageNum(), safeQuery.getPageSize()), wrapper);
    }

    @Override
    public PerformanceStandardEntity createStandard(PerformanceStandardRequest request) {
        PerformanceStandardEntity entity = new PerformanceStandardEntity();
        entity.setStandardNo(defaultText(request.getStandardNo(), generateNo()));
        entity.setStandardName(request.getStandardName());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardDesc(request.getStandardDesc());
        entity.setScoreRule(request.getScoreRule());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(1);
        entity.setCreatedBy(request.getOperatorId());
        entity.setRemark(request.getRemark());
        standardMapper.insert(entity);
        return standardMapper.selectById(entity.getId());
    }

    @Override
    public PerformanceStandardEntity updateStandard(Long standardId, PerformanceStandardRequest request) {
        PerformanceStandardEntity entity = standardMapper.selectById(standardId);
        if (entity == null) {
            throw new IllegalArgumentException("认定标准不存在");
        }
        entity.setStandardName(request.getStandardName());
        entity.setPerformanceType(request.getPerformanceType());
        entity.setStandardDesc(request.getStandardDesc());
        entity.setScoreRule(request.getScoreRule());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setUpdatedBy(request.getOperatorId());
        entity.setRemark(request.getRemark());
        standardMapper.updateById(entity);
        return standardMapper.selectById(standardId);
    }

    private Page<PerformanceStandardEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum, pageSize == null || pageSize < 1 ? 10 : pageSize);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "PSTD-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
