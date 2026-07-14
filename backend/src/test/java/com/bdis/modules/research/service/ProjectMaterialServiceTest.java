package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileBusinessService;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ProjectMaterialBindRequest;
import com.bdis.modules.research.service.impl.ProjectMaterialServiceImpl;
import com.bdis.modules.research.vo.ProjectMaterialVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMaterialServiceTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private FileBusinessMapper businessMapper;
    @Mock private FileResourceMapper resourceMapper;
    @Mock private FileBusinessService fileBusinessService;
    @Mock private AuditLogService auditLogService;
    private ProjectMaterialServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ProjectMaterialServiceImpl(
                        projectMapper,
                        businessMapper,
                        resourceMapper,
                        fileBusinessService,
                        auditLogService);
    }

    @Test
    void bindUsesFileBusinessServiceAndWritesAudit() {
        when(projectMapper.selectById(10L)).thenReturn(project("ongoing"));
        when(resourceMapper.selectById(20L)).thenReturn(activeFile(20L));
        when(businessMapper.selectOne(any())).thenReturn(null);
        when(fileBusinessService.bind(any()))
                .thenAnswer(
                        invocation -> {
                            com.bdis.file.vo.FileBusinessVO result =
                                    new com.bdis.file.vo.FileBusinessVO();
                            result.setId(30L);
                            return result;
                        });

        Long relationId = service.bind(10L, bindRequest(20L, "document"));

        assertThat(relationId).isEqualTo(30L);
        verify(fileBusinessService).bind(any());
        verify(auditLogService).record(any());
    }

    @Test
    void duplicateBindingUsesExistingFileBusinessDimension() {
        when(projectMapper.selectById(10L)).thenReturn(project("ongoing"));
        when(resourceMapper.selectById(20L)).thenReturn(activeFile(20L));
        when(businessMapper.selectOne(any())).thenReturn(new FileBusinessEntity());

        assertThatThrownBy(() -> service.bind(10L, bindRequest(20L, "dataset")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already bound");
        verify(fileBusinessService, never()).bind(any());
    }

    @Test
    void completedProjectCannotBindOrUnbindMaterial() {
        when(projectMapper.selectById(10L)).thenReturn(project("completed"));

        assertThatThrownBy(() -> service.bind(10L, bindRequest(20L, "document")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.unbind(10L, 20L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void listBatchesFileMetadataAndFiltersInactiveFiles() {
        when(projectMapper.selectById(10L)).thenReturn(project("ongoing"));
        FileBusinessEntity relation = new FileBusinessEntity();
        relation.setId(30L);
        relation.setBizId(10L);
        relation.setFileId(20L);
        relation.setFileUsage("document");
        when(businessMapper.selectList(any())).thenReturn(List.of(relation));
        when(resourceMapper.selectBatchIds(List.of(20L))).thenReturn(List.of(activeFile(20L)));

        List<ProjectMaterialVO> result = service.list(10L, "document");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBindingId()).isEqualTo(30L);
        assertThat(result.get(0).getFileName()).isEqualTo("test.pdf");
    }

    @Test
    void unbindOnlyRemovesBusinessRelation() {
        when(projectMapper.selectById(10L)).thenReturn(project("ongoing"));
        FileBusinessEntity relation = new FileBusinessEntity();
        relation.setId(30L);
        relation.setFileId(20L);
        relation.setBizId(10L);
        when(businessMapper.selectOne(any())).thenReturn(relation);

        service.unbind(10L, 20L);

        verify(fileBusinessService).unbind(30L);
        verify(resourceMapper, never()).deleteById(any());
        verify(auditLogService).record(any());
    }

    private ProjectMaterialBindRequest bindRequest(Long fileId, String usage) {
        ProjectMaterialBindRequest request = new ProjectMaterialBindRequest();
        request.setFileId(fileId);
        request.setFileUsage(usage);
        return request;
    }

    private ResearchProjectEntity project(String status) {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(10L);
        project.setProjectStatus(status);
        project.setStatus(1);
        project.setIsDeleted(0);
        return project;
    }

    private FileResourceEntity activeFile(Long id) {
        FileResourceEntity file = new FileResourceEntity();
        file.setId(id);
        file.setStatus(1);
        file.setIsDeleted(0);
        file.setFileName("test.pdf");
        file.setFileNo("F-001");
        return file;
    }
}
