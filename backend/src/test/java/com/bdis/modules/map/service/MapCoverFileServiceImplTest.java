package com.bdis.modules.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.support.ImageContentValidator;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.map.service.impl.MapCoverFileServiceImpl;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class MapCoverFileServiceImplTest {

    @Mock private FileResourceService fileResourceService;
    @Mock private FileBusinessService fileBusinessService;
    @Mock private FileResourceMapper fileResourceMapper;
    @Mock private FileBusinessMapper fileBusinessMapper;
    @Mock private ImageContentValidator imageContentValidator;

    private MapCoverFileService service;

    @BeforeEach
    void setUp() {
        CurrentUser user =
                new CurrentUser(
                        10L,
                        "teacher",
                        "Teacher A",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of("map:point:update", "file:resource:update"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        service =
                new MapCoverFileServiceImpl(
                        fileResourceService,
                        fileBusinessService,
                        fileResourceMapper,
                        fileBusinessMapper,
                        imageContentValidator);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesAndBindsOwnedPrivateImage() {
        FileResourceEntity file = privateImage(7L, 10L);
        when(fileResourceService.resolveFileId("/api/files/7/content")).thenReturn(7L);
        when(fileResourceMapper.selectById(7L)).thenReturn(file);
        when(fileBusinessMapper.selectList(any())).thenReturn(List.of());

        String result = service.replaceCover(21L, null, "/api/files/7/content");

        assertThat(result).isEqualTo("/api/public-files/7/content");
        verify(fileResourceService).publishForBusiness(7L, "map_point", 21L);
        verify(imageContentValidator).requireAllowedImage(any(), any());
        ArgumentCaptor<FileBusinessBindDTO> bindCaptor =
                ArgumentCaptor.forClass(FileBusinessBindDTO.class);
        verify(fileBusinessService).bind(bindCaptor.capture());
        assertThat(bindCaptor.getValue().getBizType()).isEqualTo("map_point");
        assertThat(bindCaptor.getValue().getBizId()).isEqualTo(21L);
        assertThat(bindCaptor.getValue().getFileUsage()).isEqualTo("cover");
    }

    @Test
    void rejectsPrivateImageUploadedByAnotherUser() {
        FileResourceEntity file = privateImage(8L, 99L);
        when(fileResourceService.resolveFileId("/api/files/8/content")).thenReturn(8L);
        when(fileResourceMapper.selectById(8L)).thenReturn(file);
        when(fileBusinessMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.replaceCover(21L, null, "/api/files/8/content"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void replacingCoverDeletesPreviousExclusiveFile() {
        FileResourceEntity nextFile = privateImage(9L, 10L);
        when(fileResourceService.resolveFileId("/api/public-files/7/content")).thenReturn(7L);
        when(fileResourceService.resolveFileId("/api/files/9/content")).thenReturn(9L);
        when(fileResourceMapper.selectById(9L)).thenReturn(nextFile);
        when(fileBusinessMapper.selectList(any())).thenReturn(List.of());
        when(fileBusinessMapper.selectCount(any())).thenReturn(1L, 0L);

        service.replaceCover(21L, "/api/public-files/7/content", "/api/files/9/content");

        verify(fileBusinessService).deleteByBusinessAndFile("map_point", 21L, 7L);
        verify(fileResourceService).deleteSystem(7L);
    }

    @Test
    void releasedCoverIsDeletedOnlyAfterTransactionCommit() {
        FileResourceEntity nextFile = privateImage(9L, 10L);
        when(fileResourceService.resolveFileId("/api/public-files/7/content")).thenReturn(7L);
        when(fileResourceService.resolveFileId("/api/files/9/content")).thenReturn(9L);
        when(fileResourceMapper.selectById(9L)).thenReturn(nextFile);
        when(fileBusinessMapper.selectList(any())).thenReturn(List.of());
        when(fileBusinessMapper.selectCount(any())).thenReturn(1L, 0L);
        TransactionSynchronizationManager.initSynchronization();

        service.replaceCover(21L, "/api/public-files/7/content", "/api/files/9/content");

        verify(fileResourceService, org.mockito.Mockito.never()).deleteSystem(7L);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(fileResourceService).deleteSystem(7L);
    }

    private FileResourceEntity privateImage(Long id, Long uploaderId) {
        FileResourceEntity file = new FileResourceEntity();
        file.setId(id);
        file.setFileType("image");
        file.setContentType("image/png");
        file.setAccessLevel("private");
        file.setUploaderId(uploaderId);
        return file;
    }
}
