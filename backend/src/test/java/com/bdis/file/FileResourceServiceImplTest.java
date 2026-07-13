package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.service.impl.FileResourceServiceImpl;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
        CurrentUser user =
                new CurrentUser(
                        8L,
                        "teacher01",
                        "教师一",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of("file:resource:view"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageByBusinessReturnsOnlyRequestedSlice() {
        when(fileBusinessService.pageByBusiness("herb_image", 9L, 2L, 2L))
                .thenReturn(new PageResult<>(List.of(file(3L), file(4L)), 2L, 2L, 5L));
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
    void uploadPersistsOnlyControlledContentUrl() {
        MockMultipartFile multipart =
                new MockMultipartFile(
                        "file", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50});
        when(fileStorageService.save(multipart))
                .thenReturn(
                        new FileStorageService.StoredFile("stored.png", "uploads/day/stored.png"));
        doAnswer(
                        invocation -> {
                            FileResourceEntity entity = invocation.getArgument(0);
                            entity.setId(42L);
                            return 1;
                        })
                .when(fileResourceMapper)
                .insert(any(FileResourceEntity.class));
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFile(multipart);
        dto.setAccessLevel("private");

        FileResourceVO result = fileResourceService.upload(dto);

        assertThat(result.getFileUrl()).isEqualTo("/api/files/42/content");
        ArgumentCaptor<FileResourceEntity> entityCaptor =
                ArgumentCaptor.forClass(FileResourceEntity.class);
        verify(fileResourceMapper).updateById(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getFileUrl()).isEqualTo("/api/files/42/content");
        assertThat(entityCaptor.getValue().getStoragePath()).isEqualTo("uploads/day/stored.png");
        assertThat(entityCaptor.getValue().getFileUrl()).doesNotContain("uploads/day");
    }

    @Test
    void uploadRejectsDirectPublicVisibility() {
        MockMultipartFile multipart =
                new MockMultipartFile("file", "public.png", "image/png", new byte[] {1, 2});
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFile(multipart);
        dto.setAccessLevel("public");

        assertThatThrownBy(() -> fileResourceService.upload(dto))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getResultCode())
                                        .isEqualTo(ResultCodeEnum.VALIDATION_ERROR));

        verify(fileStorageService, never())
                .save(any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    void uploadRejectsPartialBusinessReferenceBeforeWritingStorage() {
        MockMultipartFile multipart =
                new MockMultipartFile("file", "cover.png", "image/png", new byte[] {1, 2});
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFile(multipart);
        dto.setBizType("map_point");

        assertThatThrownBy(() -> fileResourceService.upload(dto))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getResultCode())
                                        .isEqualTo(ResultCodeEnum.VALIDATION_ERROR));

        verify(fileStorageService, never())
                .save(any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    void publishRequiresPolicyAndExistingBusinessBinding() {
        FileResourceEntity entity = resource(43L, "private", "uploads/day/reviewed.png");
        when(fileResourceMapper.selectById(43L)).thenReturn(entity);

        fileResourceService.publishForBusiness(43L, "herb_image", 9L);

        verify(fileBusinessService).setPublicVisibility(43L, "herb_image", 9L, true);
    }

    @Test
    void authorizedPreviewUsesStoragePathAndRecordsAccess() {
        FileResourceEntity entity = resource(21L, "private", "uploads/day/a.pdf");
        FileContentVO expected = new FileContentVO();
        when(fileResourceMapper.selectById(21L)).thenReturn(entity);
        when(fileStorageService.load("uploads/day/a.pdf", "a.pdf", "application/pdf", 20L))
                .thenReturn(expected);

        assertThat(fileResourceService.content(21L, "inline")).isSameAs(expected);

        verify(fileAccessGuard).requireAuthenticatedAccess(entity);
        ArgumentCaptor<FileAccessRecordDTO> recordCaptor =
                ArgumentCaptor.forClass(FileAccessRecordDTO.class);
        verify(fileAccessLogService).record(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getFileId()).isEqualTo(21L);
        assertThat(recordCaptor.getValue().getAccessType()).isEqualTo("PREVIEW");
        verify(auditLogService).record(any());
    }

    @Test
    void unauthorizedPrivateContentNeverReadsAndRecordsFailedAccess() {
        FileResourceEntity entity = resource(22L, "private", "uploads/day/secret.pdf");
        when(fileResourceMapper.selectById(22L)).thenReturn(entity);
        doThrow(new ForbiddenException("无权访问该文件"))
                .when(fileAccessGuard)
                .requireAuthenticatedAccess(entity);

        assertThatThrownBy(() -> fileResourceService.content(22L, "inline"))
                .isInstanceOf(ForbiddenException.class);

        verify(fileStorageService, never()).load(any(), any(), any(), any());
        ArgumentCaptor<FileAccessRecordDTO> recordCaptor =
                ArgumentCaptor.forClass(FileAccessRecordDTO.class);
        verify(fileAccessLogService).record(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAccessResult()).isEqualTo("FAILED");
    }

    @Test
    void publicContentRejectsPrivateResourceWithoutReadingStorage() {
        when(fileResourceMapper.selectById(23L))
                .thenReturn(resource(23L, "private", "uploads/day/secret.pdf"));

        assertThatThrownBy(() -> fileResourceService.publicContent(23L))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class);

        verify(fileStorageService, never()).load(any(), any(), any(), any());
    }

    @Test
    void publicContentRecordsControlledAccess() {
        FileResourceEntity entity = resource(24L, "public", "uploads/day/public.pdf");
        FileContentVO expected = new FileContentVO();
        when(fileResourceMapper.selectById(24L)).thenReturn(entity);
        when(fileStorageService.load("uploads/day/public.pdf", "a.pdf", "application/pdf", 20L))
                .thenReturn(expected);

        assertThat(fileResourceService.publicContent(24L)).isSameAs(expected);

        verify(fileAccessLogService).record(any(FileAccessRecordDTO.class));
    }

    @Test
    void resolveControlledUrlUsesPrivateStoragePath() {
        FileResourceEntity entity = resource(25L, "private", "uploads/day/model.bin");
        Path expected = Path.of("/tmp/storage/model.bin");
        when(fileResourceMapper.selectById(25L)).thenReturn(entity);
        when(fileStorageService.resolve("uploads/day/model.bin")).thenReturn(expected);

        assertThat(fileResourceService.resolveLocalPath("/api/files/25/content"))
                .isEqualTo(expected);

        verify(fileStorageService).resolve("uploads/day/model.bin");
        verify(fileStorageService, never()).resolve(eq("/api/files/25/content"));
    }

    @Test
    void resolveLegacyPhysicalUrlIsRejected() {
        assertThatThrownBy(
                        () ->
                                fileResourceService.resolveLocalPath(
                                        "/api/files/uploads/day/model.bin"))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class)
                .hasMessage("文件地址不是受控内容地址");

        verify(fileStorageService, never()).resolve(any(String.class));
    }

    @Test
    void deletingPublishedBusinessFileRequiresDetachAndPublishAuthorization() {
        FileResourceEntity entity = resource(26L, "public", "uploads/day/public.pdf");
        when(fileResourceMapper.selectById(26L)).thenReturn(entity);

        fileResourceService.delete(26L);

        verify(fileBusinessService).authorizeDeleteByFileId(26L);
        verify(fileBusinessService).deleteByFileId(26L);
        verify(fileResourceMapper).deleteById(26L);
    }

    private FileResourceVO file(Long id) {
        FileResourceVO file = new FileResourceVO();
        file.setId(id);
        return file;
    }

    private FileResourceEntity resource(Long id, String accessLevel, String storagePath) {
        FileResourceEntity entity = new FileResourceEntity();
        entity.setId(id);
        entity.setAccessLevel(accessLevel);
        entity.setStoragePath(storagePath);
        entity.setOriginalFilename("a.pdf");
        entity.setContentType("application/pdf");
        entity.setFileSize(20L);
        entity.setUploaderId(8L);
        entity.setStatus(1);
        return entity;
    }
}
