package com.bdis.audit.controller;

import com.bdis.audit.query.AuditLogQuery;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.vo.AuditLogVO;
import com.bdis.common.response.ApiResponse;
import com.bdis.common.response.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLogVO>> page(@Valid AuditLogQuery query) {
        return ApiResponse.success(auditLogService.page(query));
    }

    @GetMapping("/{logId}")
    public ApiResponse<AuditLogVO> detail(@PathVariable Long logId) {
        return ApiResponse.success(auditLogService.detail(logId));
    }
}
