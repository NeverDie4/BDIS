package com.bdis.modules.declaration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.modules.declaration.dto.DeclarationArchiveItemRequest;
import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.mapper.DeclarationArchiveItemMapper;
import com.bdis.modules.declaration.mapper.DeclarationArchiveMapper;
import com.bdis.modules.declaration.mapper.DeclarationMapper;
import com.bdis.modules.declaration.mapper.DeclarationMaterialMapper;
import com.bdis.modules.declaration.service.impl.DeclarationArchiveServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarationWorkflowServiceTest {

    @Mock private DeclarationMapper declarationMapper;
    @Mock private DeclarationMaterialMapper materialMapper;
    @Mock private DeclarationArchiveMapper archiveMapper;
    @Mock private DeclarationArchiveItemMapper archiveItemMapper;
    @Mock private BusinessAccessService accessService;
    @Mock private BusinessReferenceValidator referenceValidator;
    @Mock private FileResourceService fileResourceService;
    @InjectMocks private DeclarationArchiveServiceImpl archiveService;

    @Test
    void rejectsManualArchiveBeforeDeclarationIsApproved() {
        DeclarationEntity declaration = new DeclarationEntity();
        declaration.setId(1L);
        declaration.setApplicantId(2L);
        declaration.setReviewStatus("submitted");
        when(declarationMapper.selectById(1L)).thenReturn(declaration);

        assertThatThrownBy(() -> archiveService.generateArchive(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("只有审核通过的申报可以生成档案");
    }

    @Test
    void validatesArchiveItemBusinessReference() {
        DeclarationArchiveEntity archive = new DeclarationArchiveEntity();
        archive.setId(4L);
        archive.setApplicationId(1L);
        archive.setOwnerId(2L);
        when(archiveMapper.selectById(4L)).thenReturn(archive);
        DeclarationArchiveItemRequest request = new DeclarationArchiveItemRequest();
        request.setSourceType("eval_attachment");
        request.setSourceId(8L);

        archiveService.addArchiveItem(4L, request);

        verify(referenceValidator).validate("eval_attachment", 8L);
    }
}
