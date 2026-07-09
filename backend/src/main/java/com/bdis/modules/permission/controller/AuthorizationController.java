package com.bdis.modules.permission.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.dto.AuthorizationDecisionDTO;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.vo.AuthorizationDecisionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorization-decisions")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public Result<AuthorizationDecisionVO> decide(
            @Valid @RequestBody AuthorizationDecisionDTO dto) {
        authorizationService.requirePermission("auth:authorization:decide");
        return Result.success(authorizationService.decide(dto));
    }
}
