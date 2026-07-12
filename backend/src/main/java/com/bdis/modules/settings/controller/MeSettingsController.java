package com.bdis.modules.settings.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.settings.dto.AvatarUpdateRequest;
import com.bdis.modules.settings.dto.PasswordUpdateRequest;
import com.bdis.modules.settings.dto.ProfileUpdateRequest;
import com.bdis.modules.settings.dto.SettingUpdateRequest;
import com.bdis.modules.settings.service.ProfileSettingsService;
import com.bdis.modules.settings.service.UserPreferenceService;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.settings.vo.ProfileVO;
import com.bdis.modules.settings.vo.SettingNamespaceVO;
import com.bdis.modules.settings.vo.UserSessionVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeSettingsController {

    private final ProfileSettingsService profileSettingsService;

    private final UserSessionService userSessionService;

    private final UserPreferenceService userPreferenceService;

    public MeSettingsController(
            ProfileSettingsService profileSettingsService,
            UserSessionService userSessionService,
            UserPreferenceService userPreferenceService) {
        this.profileSettingsService = profileSettingsService;
        this.userSessionService = userSessionService;
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping("/profile")
    public Result<ProfileVO> profile() {
        return Result.success(profileSettingsService.getProfile());
    }

    @PatchMapping("/profile")
    public Result<ProfileVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.success(profileSettingsService.updateProfile(request));
    }

    @PutMapping("/profile/avatar")
    public Result<ProfileVO> updateAvatar(@Valid @RequestBody AvatarUpdateRequest request) {
        return Result.success(profileSettingsService.updateAvatar(request));
    }

    @DeleteMapping("/profile/avatar")
    public Result<ProfileVO> clearAvatar() {
        return Result.success(profileSettingsService.clearAvatar());
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        profileSettingsService.updatePassword(request);
        return Result.success();
    }

    @GetMapping("/settings")
    public Result<Map<String, SettingNamespaceVO>> settings() {
        return Result.success(userPreferenceService.getAll());
    }

    @GetMapping("/settings/{namespace}")
    public Result<SettingNamespaceVO> setting(@PathVariable String namespace) {
        return Result.success(userPreferenceService.get(namespace));
    }

    @PutMapping("/settings/{namespace}")
    public Result<SettingNamespaceVO> updateSetting(
            @PathVariable String namespace, @Valid @RequestBody SettingUpdateRequest request) {
        return Result.success(userPreferenceService.update(namespace, request));
    }

    @DeleteMapping("/settings/{namespace}")
    public Result<SettingNamespaceVO> resetSetting(@PathVariable String namespace) {
        return Result.success(userPreferenceService.reset(namespace));
    }

    @GetMapping("/sessions")
    public Result<List<UserSessionVO>> sessions() {
        return Result.success(userSessionService.listCurrentUserSessions());
    }

    @DeleteMapping("/sessions/others")
    public Result<Void> revokeOtherSessions() {
        userSessionService.revokeOtherCurrentUserSessions();
        return Result.success();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> revokeSession(@PathVariable String sessionId) {
        userSessionService.revokeCurrentUserSession(sessionId);
        return Result.success();
    }
}
