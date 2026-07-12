package com.bdis.modules.performance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.common.core.Result;
import com.bdis.modules.performance.dto.PerformanceAuditRequest;
import com.bdis.modules.performance.dto.PerformanceRequest;
import com.bdis.modules.performance.entity.PerformanceAuditEntity;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.query.PerformanceQuery;
import com.bdis.modules.performance.service.PerformanceAuditService;
import com.bdis.modules.performance.service.PerformanceService;
import com.bdis.modules.performance.vo.PerformanceDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performances")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceAuditService performanceAuditService;

    @GetMapping
    public Result<IPage<PerformanceEntity>> listPerformances(PerformanceQuery query) {
        return Result.success(performanceService.listPerformances(query));
    }

    @PostMapping
    public Result<PerformanceEntity> createPerformance(
            @Valid @RequestBody PerformanceRequest request) {
        return Result.success(performanceService.createPerformance(request));
    }

    @GetMapping("/{performanceId}")
    public Result<PerformanceDetailVO> getPerformanceDetail(@PathVariable Long performanceId) {
        return Result.success(performanceService.getPerformanceDetail(performanceId));
    }

    @PutMapping("/{performanceId}")
    public Result<PerformanceEntity> updatePerformance(
            @PathVariable Long performanceId, @Valid @RequestBody PerformanceRequest request) {
        return Result.success(performanceService.updatePerformance(performanceId, request));
    }

    @PostMapping("/{performanceId}/submissions")
    public Result<PerformanceEntity> submitPerformance(@PathVariable Long performanceId) {
        return Result.success(performanceService.submitPerformance(performanceId));
    }

    @PostMapping("/{performanceId}/audit-records")
    public Result<PerformanceAuditEntity> auditPerformance(
            @PathVariable Long performanceId, @Valid @RequestBody PerformanceAuditRequest request) {
        return Result.success(performanceAuditService.auditPerformance(performanceId, request));
    }
}
