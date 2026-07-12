package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.impl.FileBusinessServiceImpl;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FileBusinessServiceImplTest {

    @Mock private FileBusinessMapper fileBusinessMapper;

    @Mock private FileResourceMapper fileResourceMapper;

    @Mock private BusinessReferenceValidator businessReferenceValidator;

    @Mock private AuditLogService auditLogService;

    private FileBusinessService fileBusinessService;

    @BeforeEach
    void setUp() {
        CurrentUser teacher =
                new CurrentUser(
                        10L,
                        "teacher",
                        "Teacher A",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of("file:resource:update"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(teacher, null));
        FileAccessGuard fileAccessGuard =
                new FileAccessGuard(fileBusinessMapper, businessReferenceValidator);
        fileBusinessService =
                new FileBusinessServiceImpl(
                        fileBusinessMapper,
                        fileResourceMapper,
                        businessReferenceValidator,
                        fileAccessGuard,
                        auditLogService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bindRejectsAnotherUsersPrivateFile() {
        FileResourceEntity privateFile = new FileResourceEntity();
        privateFile.setId(77L);
        privateFile.setAccessLevel("private");
        privateFile.setUploaderId(99L);
        when(fileResourceMapper.selectById(77L)).thenReturn(privateFile);
        when(fileBusinessMapper.selectList(any())).thenReturn(List.of());

        FileBusinessBindDTO request = new FileBusinessBindDTO();
        request.setFileId(77L);
        request.setBizType("herb_species");
        request.setBizId(1L);

        assertThatThrownBy(() -> fileBusinessService.bind(request))
                .isInstanceOfSatisfying(
                        ForbiddenException.class,
                        exception ->
                                assertThat(exception.getResultCode())
                                        .isEqualTo(ResultCodeEnum.FORBIDDEN));
        verify(fileBusinessMapper, never()).insert(any(FileBusinessEntity.class));
    }

    @Test
    void bindAllowsCurrentUsersPrivateFile() {
        FileResourceEntity privateFile = new FileResourceEntity();
        privateFile.setId(78L);
        privateFile.setAccessLevel("private");
        privateFile.setUploaderId(10L);
        when(fileResourceMapper.selectById(78L)).thenReturn(privateFile);

        FileBusinessBindDTO request = new FileBusinessBindDTO();
        request.setFileId(78L);
        request.setBizType("herb_species");
        request.setBizId(1L);

        fileBusinessService.bind(request);

        verify(fileBusinessMapper).insert(any(FileBusinessEntity.class));
    }
}
