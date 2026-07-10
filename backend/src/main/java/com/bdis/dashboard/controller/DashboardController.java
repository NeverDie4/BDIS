package com.bdis.dashboard.controller;

import com.bdis.common.core.Result;
import com.bdis.dashboard.query.DashboardQuery;
import com.bdis.dashboard.service.DashboardService;
import com.bdis.dashboard.vo.DashboardMapVO;
import com.bdis.dashboard.vo.DashboardRecentGrowthRecordVO;
import com.bdis.dashboard.vo.DashboardSummaryVO;
import com.bdis.dashboard.vo.DashboardTodoVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> summary() {
        return Result.success(dashboardService.summary());
    }

    @GetMapping("/recent-growth-records")
    public Result<List<DashboardRecentGrowthRecordVO>> recentGrowthRecords(
            @Valid DashboardQuery query) {
        return Result.success(dashboardService.recentGrowthRecords(query));
    }

    @GetMapping("/pending-tasks")
    public Result<List<DashboardTodoVO>> pendingTasks(@Valid DashboardQuery query) {
        return Result.success(dashboardService.pendingTasks(query));
    }

    @GetMapping("/map-overview")
    public Result<DashboardMapVO> mapOverview() {
        return Result.success(dashboardService.mapOverview());
    }
}
