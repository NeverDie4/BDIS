package com.bdis.modules.auth.service;

import com.bdis.modules.auth.dto.BootstrapAdminDTO;
import com.bdis.modules.auth.dto.LoginDTO;
import com.bdis.modules.auth.dto.RefreshSessionDTO;
import com.bdis.modules.auth.vo.CurrentUserVO;
import com.bdis.modules.auth.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    CurrentUserVO bootstrapAdmin(BootstrapAdminDTO dto, HttpServletRequest request);

    LoginVO login(LoginDTO dto, HttpServletRequest request);

    LoginVO refresh(RefreshSessionDTO dto, HttpServletRequest request);

    void logout(String authorizationHeader);

    CurrentUserVO currentUser();
}
