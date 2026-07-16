package com.bdis.dashboard.admin.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.dashboard.admin.service.AdminDashboardService;
import com.bdis.dashboard.admin.vo.AdminDashboardSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-dashboard")
@RequirePermission("auth:dashboard:view")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryVO> summary() {
        return Result.success(adminDashboardService.summary());
    }
}
