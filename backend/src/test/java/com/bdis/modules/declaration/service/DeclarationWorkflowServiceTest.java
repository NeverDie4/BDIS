package com.bdis.modules.declaration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
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
}
