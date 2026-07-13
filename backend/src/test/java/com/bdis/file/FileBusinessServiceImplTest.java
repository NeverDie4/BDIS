package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.policy.FileBusinessPolicyRegistry;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.impl.FileBusinessServiceImpl;
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

    @Mock private FileBusinessPolicyRegistry policyRegistry;

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
        FileAccessGuard fileAccessGuard = new FileAccessGuard(fileBusinessMapper, policyRegistry);
        fileBusinessService =
                new FileBusinessServiceImpl(
                        fileBusinessMapper, fileResourceMapper, policyRegistry, fileAccessGuard);
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
        privateFile.setStatus(1);
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
        privateFile.setStatus(1);
        when(fileResourceMapper.selectById(78L)).thenReturn(privateFile);

        FileBusinessBindDTO request = new FileBusinessBindDTO();
        request.setFileId(78L);
        request.setBizType("herb_species");
        request.setBizId(1L);

        fileBusinessService.bind(request);

        verify(fileBusinessMapper).insert(any(FileBusinessEntity.class));
    }

    @Test
    void bindRejectsDisabledFile() {
        FileResourceEntity disabledFile = new FileResourceEntity();
        disabledFile.setId(79L);
        disabledFile.setAccessLevel("private");
        disabledFile.setUploaderId(10L);
        disabledFile.setStatus(0);
        when(fileResourceMapper.selectById(79L)).thenReturn(disabledFile);

        FileBusinessBindDTO request = new FileBusinessBindDTO();
        request.setFileId(79L);
        request.setBizType("herb_species");
        request.setBizId(1L);

        assertThatThrownBy(() -> fileBusinessService.bind(request))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class)
                .hasMessage("文件已停用");
        verify(fileBusinessMapper, never()).insert(any(FileBusinessEntity.class));
    }

    @Test
    void publishedFileDeleteRequiresDetachAndPublishForEveryRelation() {
        FileBusinessEntity first = new FileBusinessEntity();
        first.setFileId(80L);
        first.setBizType("herb_image");
        first.setBizId(11L);
        first.setPublicVisible(true);
        FileBusinessEntity second = new FileBusinessEntity();
        second.setFileId(80L);
        second.setBizType("herb_growth_record");
        second.setBizId(12L);
        second.setPublicVisible(false);
        when(fileBusinessMapper.selectList(any())).thenReturn(List.of(first, second));

        fileBusinessService.authorizeDeleteByFileId(80L);

        verify(policyRegistry)
                .require("herb_image", 11L, com.bdis.file.policy.FileBusinessAction.DETACH);
        verify(policyRegistry)
                .require("herb_image", 11L, com.bdis.file.policy.FileBusinessAction.PUBLISH);
        verify(policyRegistry)
                .require("herb_growth_record", 12L, com.bdis.file.policy.FileBusinessAction.DETACH);
        verify(policyRegistry, never())
                .require(
                        "herb_growth_record", 12L, com.bdis.file.policy.FileBusinessAction.PUBLISH);
        verify(policyRegistry, times(3)).require(any(), any(), any());
    }

    @Test
    void detachingOneOfTwoPublishedMapPointsKeepsSharedCoverPublic() {
        FileBusinessEntity current = new FileBusinessEntity();
        current.setId(91L);
        current.setFileId(81L);
        current.setBizType("map_point");
        current.setBizId(101L);
        current.setPublicVisible(true);
        FileResourceEntity sharedCover = new FileResourceEntity();
        sharedCover.setId(81L);
        sharedCover.setFileType("image");
        sharedCover.setAccessLevel("public");
        sharedCover.setFileUrl("/api/public-files/81/content");
        sharedCover.setThumbnailUrl("/api/public-files/81/content");
        when(fileBusinessMapper.selectOne(any())).thenReturn(current, current);
        when(fileBusinessMapper.selectCount(any())).thenReturn(1L);
        when(fileResourceMapper.selectByIdForUpdate(81L)).thenReturn(sharedCover);

        fileBusinessService.setPublicVisibility(81L, "map_point", 101L, false);
        fileBusinessService.deleteByBusinessAndFile("map_point", 101L, 81L);

        assertThat(current.getPublicVisible()).isFalse();
        assertThat(sharedCover.getAccessLevel()).isEqualTo("public");
        assertThat(sharedCover.getFileUrl()).isEqualTo("/api/public-files/81/content");
        verify(fileResourceMapper, never()).updateById(any(FileResourceEntity.class));
    }

    @Test
    void unbindingLastPublishedRelationMakesFilePrivate() {
        FileBusinessEntity relation = new FileBusinessEntity();
        relation.setId(92L);
        relation.setFileId(82L);
        relation.setBizType("map_point");
        relation.setBizId(102L);
        relation.setPublicVisible(true);
        FileResourceEntity cover = new FileResourceEntity();
        cover.setId(82L);
        cover.setFileType("image");
        cover.setAccessLevel("public");
        cover.setFileUrl("/api/public-files/82/content");
        cover.setThumbnailUrl("/api/public-files/82/content");
        when(fileBusinessMapper.selectById(92L)).thenReturn(relation);
        when(fileBusinessMapper.selectCount(any())).thenReturn(0L);
        when(fileResourceMapper.selectByIdForUpdate(82L)).thenReturn(cover);

        fileBusinessService.unbind(92L);

        assertThat(cover.getAccessLevel()).isEqualTo("private");
        assertThat(cover.getFileUrl()).isEqualTo("/api/files/82/content");
        assertThat(cover.getThumbnailUrl()).isEqualTo("/api/files/82/content");
        verify(fileResourceMapper).updateById(cover);
        verify(policyRegistry)
                .require("map_point", 102L, com.bdis.file.policy.FileBusinessAction.DETACH);
        verify(policyRegistry)
                .require("map_point", 102L, com.bdis.file.policy.FileBusinessAction.PUBLISH);
    }
}
