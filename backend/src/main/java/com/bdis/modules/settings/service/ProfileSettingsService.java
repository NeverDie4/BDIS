package com.bdis.modules.settings.service;

import com.bdis.modules.settings.dto.AvatarUpdateRequest;
import com.bdis.modules.settings.dto.PasswordUpdateRequest;
import com.bdis.modules.settings.dto.ProfileUpdateRequest;
import com.bdis.modules.settings.vo.ProfileVO;

public interface ProfileSettingsService {

    ProfileVO getProfile();

    ProfileVO updateProfile(ProfileUpdateRequest request);

    ProfileVO updateAvatar(AvatarUpdateRequest request);

    ProfileVO clearAvatar();

    void updatePassword(PasswordUpdateRequest request);
}
