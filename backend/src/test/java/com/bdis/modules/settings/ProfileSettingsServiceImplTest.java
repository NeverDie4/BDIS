package com.bdis.modules.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.settings.service.impl.ProfileSettingsServiceImpl;
import com.bdis.modules.settings.vo.ProfileVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.DepartmentMapper;
import com.bdis.modules.user.mapper.OrganizationMapper;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ProfileSettingsServiceImplTest {

    @Mock private UserMapper userMapper;

    @Mock private OrganizationMapper organizationMapper;

    @Mock private DepartmentMapper departmentMapper;

    @Mock private FileResourceService fileResourceService;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private UserSessionService userSessionService;

    @InjectMocks private ProfileSettingsServiceImpl service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        CurrentUser currentUser =
                new CurrentUser(
                        9L,
                        "teacher02",
                        "教师二",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
        user = new UserEntity();
        user.setId(9L);
        user.setUsername("teacher02");
        user.setAvatarUrl("/api/files/10/content");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadingAvatarUsesPrivateFileModuleAndRemovesPreviousFile() {
        MockMultipartFile avatar =
                new MockMultipartFile(
                        "file",
                        "avatar.png",
                        "image/png",
                        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        FileResourceVO uploaded = new FileResourceVO();
        uploaded.setId(11L);
        uploaded.setFileUrl("/api/files/11/content");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(fileResourceService.resolveFileId("/api/files/10/content")).thenReturn(10L);
        when(fileResourceService.uploadOwnedPrivateImage(avatar, "用户头像")).thenReturn(uploaded);

        ProfileVO profile = service.updateAvatar(avatar);

        assertThat(user.getAvatarUrl()).isEqualTo("/api/files/11/content");
        assertThat(profile.getAvatarUrl()).isEqualTo("/api/me/profile/avatar/content");
        verify(userMapper).updateById(user);
        verify(fileResourceService).delete(10L);
    }

    @Test
    void invalidImageSignatureIsRejectedBeforeStorage() {
        MockMultipartFile avatar =
                new MockMultipartFile(
                        "file", "avatar.png", "image/png", new byte[] {1, 2, 3, 4, 5});

        assertThatThrownBy(() -> service.updateAvatar(avatar))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("有效图片");

        verify(fileResourceService, never()).uploadOwnedPrivateImage(avatar, "用户头像");
        verify(userMapper, never()).updateById(user);
    }
}
