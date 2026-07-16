package com.bdis.modules.auth.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginVO {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private long expiresIn;

    private long refreshExpiresIn;

    private CurrentUserVO user;

    private String preferredLandingPath;

    private Boolean mustChangePassword;
}
