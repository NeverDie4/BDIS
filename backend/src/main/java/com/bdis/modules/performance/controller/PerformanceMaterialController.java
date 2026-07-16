package com.bdis.modules.performance.controller;

import com.bdis.audit.annotation.AuditLogAnnotation;
import com.bdis.common.core.Result;
import com.bdis.modules.performance.dto.PerformanceMaterialRequest;
import com.bdis.modules.performance.service.PerformanceMaterialService;
import com.bdis.modules.performance.vo.PerformanceMaterialVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performances/{performanceId}/materials")
public class PerformanceMaterialController {

    private final PerformanceMaterialService performanceMaterialService;

    @GetMapping
    public Result<List<PerformanceMaterialVO>> listMaterials(@PathVariable Long performanceId) {
        return Result.success(performanceMaterialService.listMaterials(performanceId));
    }

    @PostMapping
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "ADD_MATERIAL",
            bizType = "perf_record")
    public Result<PerformanceMaterialVO> addMaterial(
            @PathVariable Long performanceId,
            @Valid @RequestBody PerformanceMaterialRequest request) {
        return Result.success(performanceMaterialService.addMaterial(performanceId, request));
    }

    @DeleteMapping("/{relationId}")
    @AuditLogAnnotation(
            module = "M18_PERFORMANCE",
            operationType = "REMOVE_MATERIAL",
            bizType = "perf_record")
    public Result<Void> removeMaterial(
            @PathVariable Long performanceId, @PathVariable Long relationId) {
        performanceMaterialService.removeMaterial(performanceId, relationId);
        return Result.success();
    }
}
