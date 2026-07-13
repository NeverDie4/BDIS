package com.bdis.modules.settings.service;

import com.bdis.common.security.IssuedToken;
import com.bdis.common.security.JwtClaims;
import com.bdis.common.security.SessionAuthenticationDetails;
import com.bdis.modules.settings.entity.UserSessionEntity;
import com.bdis.modules.settings.vo.UserSessionVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserSessionService {

    SessionAuthenticationDetails create(Long userId, IssuedToken token, HttpServletRequest request);

    UserSessionEntity validate(JwtClaims claims);

    void revokeByJti(String jti, String reason);

    List<UserSessionVO> listCurrentUserSessions();

    void revokeCurrentUserSession(String sessionId);

    void revokeOtherCurrentUserSessions();

    void revokeAllForUser(Long userId, String reason);
}
