package com.bdis.modules.auth.service;

import com.bdis.common.security.CurrentUser;

public interface CurrentUserService {

    CurrentUser load(Long userId);
}
