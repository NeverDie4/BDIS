package com.bdis.modules.performance.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.performance.query.PerformanceStatisticsQuery;
import com.bdis.modules.performance.service.PerformanceService;
import com.bdis.modules.performance.vo.PerformanceStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performance-statistics")
public class PerformanceStatisticsController {

    private final PerformanceService performanceService;

    @GetMapping
    public Result<PerformanceStatisticsVO> getStatistics(PerformanceStatisticsQuery query) {
        return Result.success(performanceService.getStatistics(query));
    }
}
