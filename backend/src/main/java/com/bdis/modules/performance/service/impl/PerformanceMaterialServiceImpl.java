package com.bdis.modules.performance.service.impl;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.vo.FileBusinessVO;
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
    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;
    private final BusinessAccessService accessService;

    @Override
    @Transactional
    public PerformanceMaterialVO addMaterial(
            Long performanceId, PerformanceMaterialRequest request) {
        PerformanceEntity performance = findPerformance(performanceId);
        accessService.requireResourceAccess(
                BIZ_TYPE, performanceId, "performance:record:update", performance.getUserId());
        if (!"draft".equals(performance.getIdentifyStatus())
                && !"rejected".equals(performance.getIdentifyStatus())) {
            throw new IllegalArgumentException("只有草稿或退回状态的业绩可以添加材料");
        }

        fileResourceService.detail(request.getFileId());
        FileBusinessBindDTO bind = new FileBusinessBindDTO();
        bind.setFileId(request.getFileId());
        bind.setBizType(BIZ_TYPE);
        bind.setBizId(performanceId);
        bind.setFileUsage(
                StringUtils.hasText(request.getFileUsage()) ? request.getFileUsage() : "material");
        bind.setRemark(request.getRemark());
        FileBusinessVO relation = fileBusinessService.bind(bind);
        return PerformanceMaterialVO.fromFileBusiness(relation);
    }

    @Override
    public List<PerformanceMaterialVO> listMaterials(Long performanceId) {
        PerformanceEntity performance = findPerformance(performanceId);
        accessService.requireResourceAccess(
                BIZ_TYPE, performanceId, "performance:record:view", performance.getUserId());
        return fileBusinessService.listBindingsByBusiness(BIZ_TYPE, performanceId).stream()
                .map(PerformanceMaterialVO::fromFileBusiness)
                .toList();
    }

    private PerformanceEntity findPerformance(Long performanceId) {
        PerformanceEntity performance = performanceMapper.selectById(performanceId);
        if (performance == null) {
            throw new IllegalArgumentException("业绩记录不存在");
        }
        return performance;
    }
}
