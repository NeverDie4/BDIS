package com.bdis.audit.controller;

import com.bdis.audit.query.LoginLogQuery;
import com.bdis.audit.service.LoginLogService;
import com.bdis.audit.vo.LoginLogVO;
import com.bdis.common.response.ApiResponse;
import com.bdis.common.response.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<LoginLogVO>> page(@Valid LoginLogQuery query) {
        return ApiResponse.success(loginLogService.page(query));
    }
}
