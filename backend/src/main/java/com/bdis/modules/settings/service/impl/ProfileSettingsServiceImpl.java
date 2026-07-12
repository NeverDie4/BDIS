package com.bdis.modules.settings.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.PasswordVerificationException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.settings.dto.AvatarUpdateRequest;
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
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProfileSettingsServiceImpl implements ProfileSettingsService {

    private final UserMapper userMapper;

    private final OrganizationMapper organizationMapper;

    private final DepartmentMapper departmentMapper;

    private final FileResourceMapper fileResourceMapper;

    private final PasswordEncoder passwordEncoder;

    private final UserSessionService userSessionService;

    public ProfileSettingsServiceImpl(
            UserMapper userMapper,
            OrganizationMapper organizationMapper,
            DepartmentMapper departmentMapper,
            FileResourceMapper fileResourceMapper,
            PasswordEncoder passwordEncoder,
            UserSessionService userSessionService) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.departmentMapper = departmentMapper;
        this.fileResourceMapper = fileResourceMapper;
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
    public ProfileVO updateAvatar(AvatarUpdateRequest request) {
        UserEntity user = requireCurrentUser();
        FileResourceEntity file = fileResourceMapper.selectById(request.getFileId());
        if (file == null || file.getStatus() == null || file.getStatus() != 1) {
            throw new ResourceNotFoundException("头像文件不存在");
        }
        if (!user.getId().equals(file.getUploaderId())) {
            throw new ForbiddenException("只能使用本人上传的图片作为头像");
        }
        if (!"image".equalsIgnoreCase(file.getFileType())
                || !StringUtils.hasText(file.getContentType())
                || !file.getContentType().toLowerCase().startsWith("image/")) {
            throw new BusinessException("头像文件必须是图片");
        }
        user.setAvatarUrl("/api/files/" + file.getId() + "/content");
        user.setUpdatedBy(user.getId());
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    @Transactional
    public ProfileVO clearAvatar() {
        UserEntity user = requireCurrentUser();
        user.setAvatarUrl(null);
        user.setUpdatedBy(user.getId());
        userMapper.updateById(user);
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
        vo.setAvatarUrl(user.getAvatarUrl());
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
}
