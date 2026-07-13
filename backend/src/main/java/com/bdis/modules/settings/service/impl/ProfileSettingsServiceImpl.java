package com.bdis.modules.settings.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.FileStorageException;
import com.bdis.common.exception.PasswordVerificationException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.settings.dto.PasswordUpdateRequest;
import com.bdis.modules.settings.dto.ProfileUpdateRequest;
import com.bdis.modules.settings.service.ProfileSettingsService;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.settings.vo.ProfileVO;
import com.bdis.modules.user.entity.DepartmentEntity;
import com.bdis.modules.user.entity.OrganizationEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.DepartmentMapper;
import com.bdis.modules.user.mapper.OrganizationMapper;
import com.bdis.modules.user.mapper.UserMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileSettingsServiceImpl implements ProfileSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileSettingsServiceImpl.class);
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Set<String> AVATAR_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserMapper userMapper;

    private final OrganizationMapper organizationMapper;

    private final DepartmentMapper departmentMapper;

    private final FileResourceService fileResourceService;

    private final PasswordEncoder passwordEncoder;

    private final UserSessionService userSessionService;

    public ProfileSettingsServiceImpl(
            UserMapper userMapper,
            OrganizationMapper organizationMapper,
            DepartmentMapper departmentMapper,
            FileResourceService fileResourceService,
            PasswordEncoder passwordEncoder,
            UserSessionService userSessionService) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.departmentMapper = departmentMapper;
        this.fileResourceService = fileResourceService;
        this.passwordEncoder = passwordEncoder;
        this.userSessionService = userSessionService;
    }

    @Override
    public ProfileVO getProfile() {
        return toVO(requireCurrentUser());
    }

    @Override
    @Transactional
    public ProfileVO updateProfile(ProfileUpdateRequest request) {
        UserEntity user = requireCurrentUser();
        if (request.getRealName() != null) {
            user.setRealName(normalizeNullable(request.getRealName()));
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(normalizeNullable(request.getPhoneNumber()));
        }
        if (request.getEmail() != null) {
            user.setEmail(normalizeNullable(request.getEmail()));
        }
        user.setUpdatedBy(user.getId());
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    @Transactional
    public ProfileVO updateAvatar(MultipartFile file) {
        validateAvatar(file);
        UserEntity user = requireCurrentUser();
        Long previousFileId = fileResourceService.resolveFileId(user.getAvatarUrl());
        var uploaded = fileResourceService.uploadOwnedPrivateImage(file, "用户头像");
        user.setAvatarUrl(uploaded.getFileUrl());
        user.setUpdatedBy(user.getId());
        userMapper.updateById(user);
        cleanupPreviousAvatar(previousFileId, uploaded.getId());
        return toVO(user);
    }

    @Override
    public FileContentVO avatarContent() {
        UserEntity user = requireCurrentUser();
        Long fileId = fileResourceService.resolveFileId(user.getAvatarUrl());
        if (fileId == null) {
            throw new ResourceNotFoundException("头像不存在");
        }
        return fileResourceService.content(fileId, "inline");
    }

    @Override
    @Transactional
    public ProfileVO clearAvatar() {
        UserEntity user = requireCurrentUser();
        Long previousFileId = fileResourceService.resolveFileId(user.getAvatarUrl());
        user.setAvatarUrl(null);
        user.setUpdatedBy(user.getId());
        userMapper.updateById(user);
        cleanupPreviousAvatar(previousFileId, null);
        return toVO(user);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordUpdateRequest request) {
        UserEntity user = requireCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new PasswordVerificationException();
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        user.setUpdatedBy(user.getId());
        userMapper.updateById(user);
        userSessionService.revokeAllForUser(user.getId(), "password_changed");
    }

    private UserEntity requireCurrentUser() {
        Long userId = SecurityUtils.currentUser().getUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return user;
    }

    private ProfileVO toVO(UserEntity user) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        ProfileVO vo = new ProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhoneNumber(user.getPhoneNumber());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(
                StringUtils.hasText(user.getAvatarUrl()) ? "/api/me/profile/avatar/content" : null);
        vo.setOrganizationId(user.getOrganizationId());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setRoleCodes(currentUser.getRoleCodes());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setPasswordChangedAt(user.getPasswordChangedAt());
        vo.setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()));
        if (user.getOrganizationId() != null) {
            OrganizationEntity organization =
                    organizationMapper.selectById(user.getOrganizationId());
            vo.setOrganizationName(
                    organization == null ? null : organization.getOrganizationName());
        }
        if (user.getDepartmentId() != null) {
            DepartmentEntity department = departmentMapper.selectById(user.getDepartmentId());
            vo.setDepartmentName(department == null ? null : department.getDepartmentName());
        }
        return vo;
    }

    private String normalizeNullable(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像文件不能超过5MB");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !AVATAR_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("头像仅支持 JPG、PNG、GIF 或 WebP 图片");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("头像文件类型不正确");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (!matchesImageSignature(header)) {
                throw new BusinessException("头像文件内容不是有效图片");
            }
        } catch (IOException exception) {
            throw new FileStorageException("头像文件读取失败", exception);
        }
    }

    private boolean matchesImageSignature(byte[] value) {
        boolean jpeg =
                value.length >= 3
                        && (value[0] & 0xFF) == 0xFF
                        && (value[1] & 0xFF) == 0xD8
                        && (value[2] & 0xFF) == 0xFF;
        boolean png =
                value.length >= 8
                        && (value[0] & 0xFF) == 0x89
                        && value[1] == 0x50
                        && value[2] == 0x4E
                        && value[3] == 0x47;
        boolean gif = value.length >= 6 && value[0] == 'G' && value[1] == 'I' && value[2] == 'F';
        boolean webp =
                value.length >= 12
                        && value[0] == 'R'
                        && value[1] == 'I'
                        && value[2] == 'F'
                        && value[3] == 'F'
                        && value[8] == 'W'
                        && value[9] == 'E'
                        && value[10] == 'B'
                        && value[11] == 'P';
        return jpeg || png || gif || webp;
    }

    private void cleanupPreviousAvatar(Long previousFileId, Long currentFileId) {
        if (previousFileId == null || previousFileId.equals(currentFileId)) {
            return;
        }
        try {
            fileResourceService.delete(previousFileId);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to clean up replaced avatar file {}", previousFileId, exception);
        }
    }
}
