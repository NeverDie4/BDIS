package com.bdis.modules.performance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
    public Result<PerformanceStandardEntity> createStandard(@Valid @RequestBody PerformanceStandardRequest request) {
        return Result.success(performanceStandardService.createStandard(request));
    }

    @PutMapping("/{standardId}")
    public Result<PerformanceStandardEntity> updateStandard(@PathVariable Long standardId, @Valid @RequestBody PerformanceStandardRequest request) {
        return Result.success(performanceStandardService.updateStandard(standardId, request));
    }
}
