package com.bdis.audit.controller;

import com.bdis.audit.query.LoginLogQuery;
import com.bdis.audit.service.LoginLogService;
import com.bdis.audit.vo.LoginLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login-logs")
@RequirePermission("audit:login:view")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    public Result<PageResult<LoginLogVO>> page(@Valid LoginLogQuery query) {
        return Result.success(loginLogService.page(query));
    }
}
