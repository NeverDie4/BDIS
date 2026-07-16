package com.bdis.modules.performance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.audit.annotation.AuditLogAnnotation;
import com.bdis.common.core.Result;
import com.bdis.modules.performance.dto.PerformanceStandardRequest;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.query.PerformanceStandardQuery;
import com.bdis.modules.performance.service.PerformanceStandardService;
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
@RequestMapping("/performance-standards")
public class PerformanceStandardController {

    private final PerformanceStandardService performanceStandardService;

    @GetMapping
    public Result<IPage<PerformanceStandardEntity>> listStandards(PerformanceStandardQuery query) {
        return Result.success(performanceStandardService.listStandards(query));
    }

    @PostMapping
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "CREATE_STANDARD",
            bizType = "perf_standard")
    public Result<PerformanceStandardEntity> createStandard(
            @Valid @RequestBody PerformanceStandardRequest request) {
        return Result.success(performanceStandardService.createStandard(request));
    }

    @PutMapping("/{standardId}")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "UPDATE_STANDARD",
            bizType = "perf_standard")
    public Result<PerformanceStandardEntity> updateStandard(
            @PathVariable Long standardId, @Valid @RequestBody PerformanceStandardRequest request) {
        return Result.success(performanceStandardService.updateStandard(standardId, request));
    }

    @PostMapping("/{standardId}/versions")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "CREATE_STANDARD_VERSION",
            bizType = "perf_standard")
    public Result<PerformanceStandardEntity> createVersion(
            @PathVariable Long standardId, @Valid @RequestBody PerformanceStandardRequest request) {
        return Result.success(performanceStandardService.createVersion(standardId, request));
    }

    @PostMapping("/{standardId}/publish")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "PUBLISH_STANDARD",
            bizType = "perf_standard")
    public Result<PerformanceStandardEntity> publish(@PathVariable Long standardId) {
        return Result.success(performanceStandardService.publishStandard(standardId));
    }

    @PostMapping("/{standardId}/disable")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "DISABLE_STANDARD",
            bizType = "perf_standard")
    public Result<PerformanceStandardEntity> disable(@PathVariable Long standardId) {
        return Result.success(performanceStandardService.disableStandard(standardId));
    }
}
