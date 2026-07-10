package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.client.HerbRecognitionClient;
import com.bdis.modules.spectrum.client.HerbRecognitionClientResponse;
import com.bdis.modules.spectrum.dto.HerbRecognitionQueryRequest;
import com.bdis.modules.spectrum.entity.ImageRecognitionEntity;
import com.bdis.modules.spectrum.entity.SpectrumComparisonEntity;
import com.bdis.modules.spectrum.mapper.ImageRecognitionMapper;
import com.bdis.modules.spectrum.mapper.SpectrumComparisonMapper;
import com.bdis.modules.spectrum.service.impl.HerbRecognitionServiceImpl;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbRecognitionServiceTest {

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private ImageRecognitionMapper imageRecognitionMapper;

    @Mock private HerbRecognitionClient herbRecognitionClient;

    @Mock private HerbAtlasMatchService herbAtlasMatchService;

    @Mock private SpectrumComparisonMapper spectrumComparisonMapper;

    private HerbRecognitionService herbRecognitionService;

    @BeforeEach
    void setUp() {
        herbRecognitionService =
                new HerbRecognitionServiceImpl(
                        herbImageMapper,
                        herbSpeciesMapper,
                        imageRecognitionMapper,
                        herbRecognitionClient,
                        herbAtlasMatchService,
                        spectrumComparisonMapper);
    }

    @Test
    void recognizeRejectsMissingImage() {
        when(herbImageMapper.selectActiveById(1L)).thenReturn(null);

        assertThatThrownBy(() -> herbRecognitionService.recognize(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb image not found");
    }

    @Test
    void recognizeRejectsImageAlreadyRecognizing() {
        HerbImageEntity image = image("recognizing");
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);

        assertThatThrownBy(() -> herbRecognitionService.recognize(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Herb image is recognizing");

        verify(herbAtlasMatchService, never()).matchTopN(any(), any(Integer.class));
        verify(herbRecognitionClient, never()).recognize(any());
    }

    @Test
    void recognizeUsesLocalAtlasMatchDirectlyWhenSimilarityIsHigh() {
        HerbImageEntity image = image("uploaded");
        HerbEntity species = new HerbEntity();
        species.setId(2L);
        species.setHerbName("Huanglian");
        HerbAtlasMatchResult match = match(10L, 2L, "Huanglian", new BigDecimal("0.90"), 1);
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbAtlasMatchService.matchTopN(image, 5)).thenReturn(List.of(match));
        when(herbSpeciesMapper.selectByNameOrAlias("Huanglian")).thenReturn(species);

        HerbRecognitionVO result = herbRecognitionService.recognize(1L);

        ArgumentCaptor<HerbImageEntity> imageCaptor =
                ArgumentCaptor.forClass(HerbImageEntity.class);
        verify(herbImageMapper, times(2)).updateProcessStatus(imageCaptor.capture());
        assertThat(imageCaptor.getAllValues())
                .extracting(HerbImageEntity::getProcessStatus)
                .containsExactly("recognizing", "recognized");
        ArgumentCaptor<ImageRecognitionEntity> recognitionCaptor =
                ArgumentCaptor.forClass(ImageRecognitionEntity.class);
        verify(imageRecognitionMapper).insertRecognition(recognitionCaptor.capture());
        ImageRecognitionEntity inserted = recognitionCaptor.getValue();
        assertThat(inserted.getImageId()).isEqualTo(1L);
        assertThat(inserted.getSpeciesId()).isEqualTo(2L);
        assertThat(inserted.getRecognizedHerbName()).isEqualTo("Huanglian");
        assertThat(inserted.getIsUncertain()).isZero();
        assertThat(inserted.getConfidence()).isEqualByComparingTo("0.90");
        assertThat(result.getPredictedSpeciesName()).isEqualTo("Huanglian");
        assertThat(result.getRecognitionStatus()).isEqualTo("success");
        assertThat(result.getNeedReview()).isFalse();
        assertThat(result.getRecognitionSource()).isEqualTo("local_atlas");
        verify(spectrumComparisonMapper).insert(any(SpectrumComparisonEntity.class));
        verify(herbRecognitionClient, never()).recognize(any());
    }

    @Test
    void recognizeReturnsCandidatesAndRequiresReviewWhenSimilarityIsMedium() {
        HerbImageEntity image = image("uploaded");
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbAtlasMatchService.matchTopN(image, 5))
                .thenReturn(
                        List.of(
                                match(10L, 2L, "Huanglian", new BigDecimal("0.84"), 1),
                                match(11L, 3L, "Dangshen", new BigDecimal("0.70"), 2)));
        HerbEntity species = new HerbEntity();
        species.setId(2L);
        species.setHerbName("Huanglian");
        when(herbSpeciesMapper.selectByNameOrAlias("Huanglian")).thenReturn(species);

        HerbRecognitionVO result = herbRecognitionService.recognize(1L);

        assertThat(result.getPredictedName()).isEqualTo("Huanglian");
        assertThat(result.getConfidence()).isEqualByComparingTo("0.84");
        assertThat(result.getNeedReview()).isTrue();
        assertThat(result.getRecognitionSource()).isEqualTo("local_atlas");
        assertThat(result.getCandidates()).hasSize(2);
        verify(herbImageMapper, times(2)).updateProcessStatus(any());
        verify(spectrumComparisonMapper, times(2)).insert(any(SpectrumComparisonEntity.class));
        verify(herbRecognitionClient, never()).recognize(any());
    }

    @Test
    void recognizeCallsDoubaoOnlyWhenLocalSimilarityIsLow() {
        HerbImageEntity image = image("uploaded");
        HerbAtlasMatchResult lowMatch = match(10L, 2L, "Huanglian", new BigDecimal("0.50"), 1);
        HerbRecognitionClientResponse auxiliaryResponse = new HerbRecognitionClientResponse();
        auxiliaryResponse.setSuccess(true);
        auxiliaryResponse.setPredictedName("Wuzhimaotao");
        auxiliaryResponse.setConfidence(new BigDecimal("0.70"));
        auxiliaryResponse.setRawResult("{\"success\":true}");
        HerbEntity species = new HerbEntity();
        species.setId(9L);
        species.setHerbName("Wuzhimaotao");
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbAtlasMatchService.matchTopN(image, 5)).thenReturn(List.of(lowMatch));
        when(herbRecognitionClient.recognize(image)).thenReturn(auxiliaryResponse);
        when(herbSpeciesMapper.selectByNameOrAlias("Wuzhimaotao")).thenReturn(species);

        HerbRecognitionVO result = herbRecognitionService.recognize(1L);

        assertThat(result.getPredictedName()).isEqualTo("Wuzhimaotao");
        assertThat(result.getPredictedSpeciesId()).isEqualTo(9L);
        assertThat(result.getConfidence()).isEqualByComparingTo("0.70");
        assertThat(result.getNeedReview()).isTrue();
        assertThat(result.getRecognitionSource()).isEqualTo("doubao_auxiliary");
        assertThat(result.getCandidates()).hasSize(1);
        verify(herbRecognitionClient).recognize(image);
    }

    @Test
    void recognizeMarksImageFailedWhenClientThrowsException() {
        HerbImageEntity image = image("uploaded");
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbAtlasMatchService.matchTopN(image, 5))
                .thenReturn(List.of(match(10L, 2L, "Huanglian", new BigDecimal("0.50"), 1)));
        when(herbRecognitionClient.recognize(image)).thenThrow(new BusinessException("timeout"));

        assertThatThrownBy(() -> herbRecognitionService.recognize(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("timeout");

        ArgumentCaptor<HerbImageEntity> imageCaptor =
                ArgumentCaptor.forClass(HerbImageEntity.class);
        verify(herbImageMapper, times(2)).updateProcessStatus(imageCaptor.capture());
        assertThat(imageCaptor.getAllValues())
                .extracting(HerbImageEntity::getProcessStatus)
                .containsExactly("recognizing", "failed");
    }

    @Test
    void latestReturnsLatestRecognitionOrNull() {
        HerbRecognitionVO vo = new HerbRecognitionVO();
        vo.setId(3L);
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image("recognized"));
        when(imageRecognitionMapper.selectLatestByImageId(1L)).thenReturn(vo);

        assertThat(herbRecognitionService.latest(1L).getId()).isEqualTo(3L);
    }

    @Test
    void historyReturnsAllRecognitionsForImage() {
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image("recognized"));
        when(imageRecognitionMapper.selectByImageId(1L))
                .thenReturn(List.of(new HerbRecognitionVO()));

        assertThat(herbRecognitionService.history(1L)).hasSize(1);
    }

    @Test
    void pageNormalizesInvalidPageParameters() {
        HerbRecognitionQueryRequest request = new HerbRecognitionQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        when(imageRecognitionMapper.countPage(request)).thenReturn(1L);
        when(imageRecognitionMapper.selectPage(request, 0L, 10))
                .thenReturn(List.of(new HerbRecognitionVO()));

        PageResult<HerbRecognitionVO> result = herbRecognitionService.page(request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    private HerbImageEntity image(String processStatus) {
        HerbImageEntity image = new HerbImageEntity();
        image.setId(1L);
        image.setImageNo("IMG_1");
        image.setImageUrl("/api/files/uploads/2026-07-08/IMG_1.jpg");
        image.setOriginalFilename("IMG_1.jpg");
        image.setProcessStatus(processStatus);
        image.setIsDeleted(0);
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());
        return image;
    }

    private HerbAtlasMatchResult match(
            Long atlasId, Long speciesId, String speciesName, BigDecimal similarity, int rank) {
        HerbAtlasMatchResult match = new HerbAtlasMatchResult();
        match.setAtlasId(atlasId);
        match.setSpeciesId(speciesId);
        match.setSpeciesName(speciesName);
        match.setSimilarity(similarity);
        match.setRank(rank);
        return match;
    }
}
