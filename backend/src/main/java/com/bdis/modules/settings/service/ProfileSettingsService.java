package com.bdis.modules.settings.service;

import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.settings.dto.PasswordUpdateRequest;
import com.bdis.modules.settings.dto.ProfileUpdateRequest;
import com.bdis.modules.settings.vo.ProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileSettingsService {

    ProfileVO getProfile();

    ProfileVO updateProfile(ProfileUpdateRequest request);

    ProfileVO updateAvatar(MultipartFile file);

    FileContentVO avatarContent();

    ProfileVO clearAvatar();

    void updatePassword(PasswordUpdateRequest request);
}
