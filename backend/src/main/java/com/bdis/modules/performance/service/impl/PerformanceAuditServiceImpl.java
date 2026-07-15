package com.bdis.modules.performance.service.impl;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceAuditRequest;
import com.bdis.modules.performance.entity.PerformanceAuditEntity;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceAuditMapper;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.service.PerformanceAuditService;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PerformanceAuditServiceImpl implements PerformanceAuditService {

    private static final Set<String> AUDIT_RESULTS = Set.of("approved", "rejected");

    private final PerformanceMapper performanceMapper;

    private final PerformanceAuditMapper auditMapper;
    private final BusinessAccessService accessService;

    @Override
    @Transactional
    public PerformanceAuditEntity auditPerformance(
            Long performanceId, PerformanceAuditRequest request) {
        PerformanceEntity performance = performanceMapper.selectById(performanceId);
        if (performance == null) {
            throw new IllegalArgumentException("业绩记录不存在");
        }
        accessService.requireResourceAccess(
                "perf_record", performanceId, "performance:record:audit", performance.getUserId());
        Long identifierId = accessService.currentUserId();
        if (identifierId.equals(performance.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "不能审核本人业绩");
        }
        if (!"submitted".equals(performance.getIdentifyStatus())) {
            throw new IllegalArgumentException("只有已提交状态的业绩可以审核认定");
        }
        String result = request.resolvedResult();
        if (!StringUtils.hasText(result)) {
            throw new IllegalArgumentException("认定结果不能为空");
        }
        if (!AUDIT_RESULTS.contains(result)) {
            throw new IllegalArgumentException("认定结果只能是 approved 或 rejected");
        }
        if ("rejected".equals(result) && !StringUtils.hasText(request.resolvedComment())) {
            throw new IllegalArgumentException("退回业绩时必须填写认定意见");
        }

        performance.setIdentifyStatus(result);
        performance.setUpdatedBy(identifierId);
        if (performanceMapper.updateById(performance) != 1) {
            throw new BusinessException(ResultCodeEnum.RESOURCE_CONFLICT, "业绩已被其他审核人处理");
        }

        PerformanceAuditEntity audit = new PerformanceAuditEntity();
        audit.setPerformanceId(performanceId);
        audit.setIdentifierId(identifierId);
        audit.setIdentifyAction(request.resolvedAction());
        audit.setIdentifyResult(result);
        audit.setIdentifyComment(request.resolvedComment());
        audit.setIdentifiedAt(LocalDateTime.now());
        audit.setCreatedBy(identifierId);
        audit.setRemark(request.getRemark());
        auditMapper.insert(audit);
        return auditMapper.selectById(audit.getId());
    }
}
