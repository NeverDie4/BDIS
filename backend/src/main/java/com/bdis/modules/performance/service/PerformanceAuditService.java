package com.bdis.modules.performance.service;

import com.bdis.modules.performance.dto.PerformanceAuditRequest;
import com.bdis.modules.performance.entity.PerformanceAuditEntity;

public interface PerformanceAuditService {

    PerformanceAuditEntity auditPerformance(Long performanceId, PerformanceAuditRequest request);
}
