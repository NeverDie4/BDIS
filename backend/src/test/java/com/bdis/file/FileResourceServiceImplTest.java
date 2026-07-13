package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.service.impl.FileResourceServiceImpl;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
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
class FileResourceServiceImplTest {

    @Mock private FileResourceMapper fileResourceMapper;

    @Mock private FileStorageService fileStorageService;

    @Mock private FileAccessLogService fileAccessLogService;

    @Mock private AuditLogService auditLogService;

    @Mock private FileBusinessService fileBusinessService;

    @Mock private FileAccessGuard fileAccessGuard;

    private FileResourceService fileResourceService;

    @BeforeEach
    void setUp() {
        fileResourceService =
                new FileResourceServiceImpl(
                        fileResourceMapper,
                        fileStorageService,
                        fileAccessLogService,
                        auditLogService,
                        fileBusinessService,
                        fileAccessGuard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageByBusinessReturnsOnlyRequestedSlice() {
        when(fileBusinessService.listByBusiness("herb_image", 9L))
                .thenReturn(List.of(file(1L), file(2L), file(3L), file(4L), file(5L)));
        FileResourceQuery query = new FileResourceQuery();
        query.setBizType("herb_image");
        query.setBizId(9L);
        query.setPage(2);
        query.setSize(2);

        PageResult<FileResourceVO> result = fileResourceService.page(query);

        assertThat(result.getRecords()).extracting(FileResourceVO::getId).containsExactly(3L, 4L);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(5);
    }

    @Test
    void deletesCurrentUsersPrivateUnboundUpload() {
        authenticate(10L);
        FileResourceEntity file = privateFile(7L, 10L);
        when(fileResourceMapper.selectById(7L)).thenReturn(file);
        when(fileBusinessService.countByFileId(7L)).thenReturn(0L);

        fileResourceService.deleteOwnUnboundUpload(7L);

        verify(fileBusinessService).deleteByFileId(7L);
        verify(fileResourceMapper).deleteById(7L);
        verify(fileStorageService).delete(file.getStoragePath());
    }

    @Test
    void refusesToDeleteBoundUploadThroughTemporaryCleanup() {
        authenticate(10L);
        when(fileResourceMapper.selectById(7L)).thenReturn(privateFile(7L, 10L));
        when(fileBusinessService.countByFileId(7L)).thenReturn(1L);

        assertThatThrownBy(() -> fileResourceService.deleteOwnUnboundUpload(7L))
                .isInstanceOf(ForbiddenException.class);
        verify(fileResourceMapper, org.mockito.Mockito.never()).deleteById(any());
    }

    private void authenticate(Long userId) {
        CurrentUser user =
                new CurrentUser(
                        userId,
                        "teacher",
                        "Teacher",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of("file:resource:upload"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    private FileResourceEntity privateFile(Long id, Long uploaderId) {
        FileResourceEntity file = new FileResourceEntity();
        file.setId(id);
        file.setUploaderId(uploaderId);
        file.setAccessLevel("private");
        file.setStoragePath("2026/07/file.png");
        return file;
    }

    private FileResourceVO file(Long id) {
        FileResourceVO file = new FileResourceVO();
        file.setId(id);
        return file;
    }
}
