package com.bdis.modules.performance.file;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import org.springframework.stereotype.Component;

@Component
public class PerformanceFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final PerformanceMapper performanceMapper;
    private final BusinessAccessService accessService;

    public PerformanceFileBusinessAccessPolicy(
            PerformanceMapper performanceMapper, BusinessAccessService accessService) {
        this.performanceMapper = performanceMapper;
        this.accessService = accessService;
    }

    @Override
    public String bizType() {
        return "perf_record";
    }

    @Override
    public boolean exists(Long bizId) {
        return performanceMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return isAllowed(bizId, "performance:record:view");
    }

    @Override
    public boolean canAttach(Long bizId) {
        return isAllowed(bizId, "performance:record:update");
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return isAllowed(bizId, "performance:record:audit");
    }

    private boolean isAllowed(Long bizId, String permissionCode) {
        PerformanceEntity performance = performanceMapper.selectById(bizId);
        if (performance == null) {
            return false;
        }
        try {
            accessService.requireResourceAccess(
                    bizType(), bizId, permissionCode, performance.getUserId());
            return true;
        } catch (ForbiddenException exception) {
            return false;
        }
    }
}
