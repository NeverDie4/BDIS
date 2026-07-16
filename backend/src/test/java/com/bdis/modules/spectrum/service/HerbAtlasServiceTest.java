package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.client.HerbFeatureVectorClient;
import com.bdis.modules.spectrum.dto.HerbAtlasQueryRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUpdateRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUploadRequest;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.entity.SpectrumTagEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasTagMapper;
import com.bdis.modules.spectrum.service.impl.HerbAtlasServiceImpl;
import com.bdis.modules.spectrum.vo.HerbAtlasVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class HerbAtlasServiceTest {
    @Mock private HerbAtlasMapper herbAtlasMapper;

    @Mock private HerbAtlasTagMapper herbAtlasTagMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private FileResourceService fileResourceService;

    @Mock private FileBusinessService fileBusinessService;

    @Mock private HerbFeatureVectorClient featureVectorClient;

    private HerbAtlasService herbAtlasService;

    @BeforeEach
    void setUp() {
        herbAtlasService =
                new HerbAtlasServiceImpl(
                        herbAtlasMapper,
                        herbAtlasTagMapper,
                        herbSpeciesMapper,
                        fileResourceService,
                        fileBusinessService,
                        featureVectorClient,
                        new ObjectMapper());
    }

    @Test
    void uploadAtlasRejectsMissingSpecies() {
        HerbAtlasUploadRequest request = uploadRequest();
        MockMultipartFile file = imageFile("leaf.png");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(null);

        assertThatThrownBy(() -> herbAtlasService.upload(file, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb species not found");

        verify(herbAtlasMapper, never()).insertAtlas(any());
    }

    @Test
    void uploadAtlasRejectsUnsupportedImageFormat() {
        HerbAtlasUploadRequest request = uploadRequest();
        HerbEntity species = new HerbEntity();
        species.setId(1L);
        species.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(species);
        MockMultipartFile file =
                new MockMultipartFile("file", "leaf.gif", "image/gif", new byte[] {1});

        assertThatThrownBy(() -> herbAtlasService.upload(file, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported image format");
    }

    @Test
    void uploadAtlasSavesFileAndTags() throws Exception {
        HerbAtlasUploadRequest request = uploadRequest();
        request.setTags("leaf, healthy, leaf, standard");
        HerbEntity species = new HerbEntity();
        species.setId(1L);
        species.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(species);
        FileResourceVO fileResource = new FileResourceVO();
        fileResource.setId(21L);
        fileResource.setFileUrl("/api/files/21/content");
        when(fileResourceService.upload(any())).thenReturn(fileResource);
        when(featureVectorClient.extract(any())).thenReturn(List.of(0.1D, 0.2D, 0.3D));
        MockMultipartFile file = imageFile("leaf.png");

        HerbAtlasVO result = herbAtlasService.upload(file, request);

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        SpectrumEntity inserted = atlasCaptor.getValue();
        assertThat(inserted.getAtlasNo()).startsWith("ATLAS_");
        assertThat(inserted.getImageUrl()).isEqualTo("/api/files/21/content");
        assertThat(inserted.getFeatureVector()).isEqualTo("[0.1,0.2,0.3]");
        assertThat(inserted.getFeatureDim()).isEqualTo(3);
        assertThat(inserted.getStatus()).isEqualTo(1);
        verify(fileBusinessService).bind(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SpectrumTagEntity>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(herbAtlasTagMapper).insertTags(tagsCaptor.capture());
        assertThat(tagsCaptor.getValue())
                .extracting(SpectrumTagEntity::getTagName)
                .containsExactly("leaf", "healthy", "standard");
        assertThat(result.getHerbName()).isEqualTo("Huanglian");
    }

    @Test
    void deleteAtlasDeletesAtlasAndTagsLogically() {
        SpectrumEntity atlas = new SpectrumEntity();
        atlas.setId(2L);
        when(herbAtlasMapper.selectActiveById(2L)).thenReturn(atlas);
        when(herbAtlasMapper.logicalDeleteById(any(SpectrumEntity.class))).thenReturn(1);

        herbAtlasService.delete(2L);

        verify(herbAtlasMapper).logicalDeleteById(any(SpectrumEntity.class));
        verify(herbAtlasTagMapper).logicalDeleteByAtlasId(any(SpectrumTagEntity.class));
    }

    @Test
    void updateAtlasReplacesTagsWhenProvided() {
        SpectrumEntity atlas = new SpectrumEntity();
        atlas.setId(2L);
        when(herbAtlasMapper.selectActiveById(2L)).thenReturn(atlas);
        HerbAtlasUpdateRequest request = new HerbAtlasUpdateRequest();
        request.setImageType("root");
        request.setTags("root,standard");

        herbAtlasService.update(2L, request);

        verify(herbAtlasMapper).updateAtlas(any(SpectrumEntity.class));
        verify(herbAtlasTagMapper).logicalDeleteByAtlasId(any(SpectrumTagEntity.class));
        verify(herbAtlasTagMapper).insertTags(any());
    }

    @Test
    void pageAtlasNormalizesInvalidPageParameters() {
        HerbAtlasQueryRequest request = new HerbAtlasQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        when(herbAtlasMapper.countPage(request)).thenReturn(1L);
        when(herbAtlasMapper.selectPage(request, 0L, 10)).thenReturn(List.of(new HerbAtlasVO()));

        PageResult<HerbAtlasVO> result = herbAtlasService.page(request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    private HerbAtlasUploadRequest uploadRequest() {
        HerbAtlasUploadRequest request = new HerbAtlasUploadRequest();
        request.setSpeciesId(1L);
        request.setImageType("leaf");
        request.setGrowthStage("mature");
        return request;
    }

    private MockMultipartFile imageFile(String filename) {
        return new MockMultipartFile("file", filename, "image/png", new byte[] {1, 2, 3});
    }
}
