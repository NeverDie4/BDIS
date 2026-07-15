package com.bdis.modules.herb.service;

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
import com.bdis.modules.herb.service.impl.HerbSpeciesCoverFileServiceImpl;
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

@ExtendWith(MockitoExtension.class)
class HerbSpeciesCoverFileServiceImplTest {

    @Mock private FileResourceService fileResourceService;
    @Mock private FileBusinessService fileBusinessService;
    @Mock private FileResourceMapper fileResourceMapper;
    @Mock private FileBusinessMapper fileBusinessMapper;
    @Mock private ImageContentValidator imageContentValidator;

    private HerbSpeciesCoverFileService service;

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
                        Set.of("herb:species:update"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        service =
                new HerbSpeciesCoverFileServiceImpl(
                        fileResourceService,
                        fileBusinessService,
                        fileResourceMapper,
                        fileBusinessMapper,
                        imageContentValidator);
    }

    @AfterEach
    void tearDown() {
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
        verify(fileResourceService).publishForBusiness(7L, "herb_species", 21L);
        verify(imageContentValidator).requireAllowedImage(any(), any());
        ArgumentCaptor<FileBusinessBindDTO> bindCaptor =
                ArgumentCaptor.forClass(FileBusinessBindDTO.class);
        verify(fileBusinessService).bind(bindCaptor.capture());
        assertThat(bindCaptor.getValue().getBizType()).isEqualTo("herb_species");
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
