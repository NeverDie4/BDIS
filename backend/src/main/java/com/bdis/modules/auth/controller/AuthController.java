package com.bdis.modules.auth.controller;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.core.Result;
import com.bdis.modules.auth.dto.BootstrapAdminDTO;
import com.bdis.modules.auth.dto.LoginDTO;
import com.bdis.modules.auth.service.AuthService;
import com.bdis.modules.auth.vo.CurrentUserVO;
import com.bdis.modules.auth.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/bootstrap-admin")
    public Result<CurrentUserVO> bootstrapAdmin(
            @Valid @RequestBody BootstrapAdminDTO dto, HttpServletRequest request) {
        return Result.success(authService.bootstrapAdmin(dto, request));
    }

    @PostMapping("/sessions")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success(authService.login(dto, request));
    }

    @DeleteMapping("/sessions/current")
    public Result<Void> logout(
            @RequestHeader(value = SecurityConstants.AUTHORIZATION_HEADER, required = false)
                    String authorizationHeader) {
        authService.logout(authorizationHeader);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<CurrentUserVO> currentUser() {
        return Result.success(authService.currentUser());
    }
}
