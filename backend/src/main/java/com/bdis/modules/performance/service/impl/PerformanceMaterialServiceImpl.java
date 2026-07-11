package com.bdis.modules.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.performance.dto.PerformanceMaterialRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.service.PerformanceMaterialService;
import com.bdis.modules.performance.vo.PerformanceMaterialVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceMaterialServiceImpl implements PerformanceMaterialService {

    private static final String BIZ_TYPE = "perf_record";

    private final PerformanceMapper performanceMapper;

    private final FileResourceMapper fileResourceMapper;

    private final FileBusinessMapper fileBusinessMapper;

    @Override
    @Transactional
    public PerformanceMaterialVO addMaterial(Long performanceId, PerformanceMaterialRequest request) {
        PerformanceEntity performance = performanceMapper.selectById(performanceId);
        if (performance == null) {
            throw new IllegalArgumentException("业绩记录不存在");
        }
        FileResourceEntity file = fileResourceMapper.selectById(request.getFileId());
        if (file == null) {
            throw new IllegalArgumentException("文件资源不存在");
        }
        Long count = fileBusinessMapper.selectCount(
                new LambdaQueryWrapper<FileBusinessEntity>()
                        .eq(FileBusinessEntity::getBizType, BIZ_TYPE)
                        .eq(FileBusinessEntity::getBizId, performanceId)
                        .eq(FileBusinessEntity::getFileId, request.getFileId()));
        if (count > 0) {
            throw new IllegalArgumentException("该业绩材料已关联");
        }
        FileBusinessEntity entity = new FileBusinessEntity();
        entity.setFileId(request.getFileId());
        entity.setBizType(BIZ_TYPE);
        entity.setBizId(performanceId);
        entity.setFileUsage(StringUtils.hasText(request.getFileUsage()) ? request.getFileUsage() : "material");
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setCreatedBy(request.getOperatorId());
        entity.setRemark(request.getRemark());
        fileBusinessMapper.insert(entity);
        return PerformanceMaterialVO.fromEntity(fileBusinessMapper.selectById(entity.getId()));
    }

    @Override
    public List<PerformanceMaterialVO> listMaterials(Long performanceId) {
        PerformanceEntity performance = performanceMapper.selectById(performanceId);
        if (performance == null) {
            throw new IllegalArgumentException("业绩记录不存在");
        }
        return fileBusinessMapper.selectList(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getBizType, BIZ_TYPE)
                                .eq(FileBusinessEntity::getBizId, performanceId)
                                .orderByAsc(FileBusinessEntity::getSortOrder)
                                .orderByAsc(FileBusinessEntity::getId))
                .stream()
                .map(PerformanceMaterialVO::fromEntity)
                .toList();
    }
}
