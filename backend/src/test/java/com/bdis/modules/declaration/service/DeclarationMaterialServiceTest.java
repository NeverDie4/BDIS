package com.bdis.modules.declaration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.declaration.dto.DeclarationMaterialRequest;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;
import com.bdis.modules.declaration.mapper.DeclarationArchiveItemMapper;
import com.bdis.modules.declaration.mapper.DeclarationArchiveMapper;
import com.bdis.modules.declaration.mapper.DeclarationMapper;
import com.bdis.modules.declaration.mapper.DeclarationMaterialMapper;
import com.bdis.modules.declaration.service.impl.DeclarationMaterialServiceImpl;
import com.bdis.modules.file.vo.FileResourceVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarationMaterialServiceTest {

    @Mock private DeclarationMapper declarationMapper;
    @Mock private DeclarationMaterialMapper materialMapper;
    @Mock private DeclarationArchiveMapper archiveMapper;
    @Mock private DeclarationArchiveItemMapper archiveItemMapper;
    @Mock private BusinessAccessService accessService;
    @Mock private FileResourceService fileResourceService;
    @Mock private FileBusinessService fileBusinessService;
    @InjectMocks private DeclarationMaterialServiceImpl service;

    @Test
    void validatesFileAccessAndUsesFileModuleMetadataAndBinding() {
        DeclarationEntity declaration = new DeclarationEntity();
        declaration.setId(1L);
        declaration.setApplicantId(7L);
        declaration.setReviewStatus("draft");
        when(declarationMapper.selectById(1L)).thenReturn(declaration);
        when(accessService.currentUserId()).thenReturn(7L);
        FileResourceVO file = new FileResourceVO();
        file.setId(9L);
        file.setOriginalFilename("evidence.pdf");
        file.setFileType("document");
        file.setFileUrl("/api/files/9/content");
        file.setFileSize(128L);
        when(fileResourceService.detail(9L)).thenReturn(file);
        doAnswer(
                        invocation -> {
                            DeclarationMaterialEntity entity = invocation.getArgument(0);
                            entity.setId(3L);
                            inserted = entity;
                            return 1;
                        })
                .when(materialMapper)
                .insert(any(DeclarationMaterialEntity.class));
        when(materialMapper.selectById(3L)).thenAnswer(invocation -> inserted);

        DeclarationMaterialRequest request = new DeclarationMaterialRequest();
        request.setFileId(9L);
        request.setRemark("申报证明");
        DeclarationMaterialEntity result = service.addMaterial(1L, request);

        verify(fileResourceService).detail(9L);
        ArgumentCaptor<FileBusinessBindDTO> bind =
                ArgumentCaptor.forClass(FileBusinessBindDTO.class);
        verify(fileBusinessService).bind(bind.capture());
        assertThat(bind.getValue().getBizType()).isEqualTo("eval_application");
        assertThat(bind.getValue().getBizId()).isEqualTo(1L);
        assertThat(result.getFileName()).isEqualTo("evidence.pdf");
        assertThat(result.getUploaderId()).isEqualTo(7L);
    }

    private DeclarationMaterialEntity inserted;
}
