package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.support.HerbImageAccessService;
import com.bdis.modules.spectrum.config.HerbIdentificationProperties;
import com.bdis.modules.spectrum.dto.HerbIdentificationReviewRequest;
import com.bdis.modules.spectrum.dto.HerbIdentifyRequest;
import com.bdis.modules.spectrum.entity.HerbIdentificationResultEntity;
import com.bdis.modules.spectrum.mapper.HerbIdentificationResultMapper;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbImageMatchMapper;
import com.bdis.modules.spectrum.service.impl.HerbIdentificationServiceImpl;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchVO;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class HerbIdentificationServiceTest {

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private HerbImageFeatureMapper herbImageFeatureMapper;

    @Mock private HerbImageMatchMapper herbImageMatchMapper;

    @Mock private HerbIdentificationResultMapper identificationResultMapper;

    @Mock private HerbFeatureService herbFeatureService;

    @Mock private HerbImageMatchService herbImageMatchService;

    @Mock private HerbRecognitionService herbRecognitionService;

    @Mock private HerbImageAccessService herbImageAccessService;

    private HerbIdentificationService herbIdentificationService;

    @BeforeEach
    void setUp() {
        CurrentUser reviewer =
                new CurrentUser(
                        9L,
                        "reviewer",
                        "Reviewer A",
                        null,
                        null,
                        Set.of("REVIEWER"),
                        Set.of(4L),
                        Set.of("herb:identification:review"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(reviewer, null));
        herbIdentificationService =
                new HerbIdentificationServiceImpl(
                        herbImageMapper,
                        herbSpeciesMapper,
                        herbImageFeatureMapper,
                        herbImageMatchMapper,
                        identificationResultMapper,
                        herbFeatureService,
                        herbImageMatchService,
                        herbRecognitionService,
                        identificationProperties(),
                        new ObjectMapper(),
                        herbImageAccessService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void identifyUsesLocalMatchWhenSimilarityIsHigh() {
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image());
        when(herbImageFeatureMapper.selectLatestSuccessByImageId(1L))
                .thenReturn(new FeatureExtractResultVO());
        when(herbImageMatchService.match(any(), any())).thenReturn(localMatch("0.9100"));

        HerbIdentificationVO result =
                herbIdentificationService.identify(1L, new HerbIdentifyRequest());

        ArgumentCaptor<HerbIdentificationResultEntity> captor =
                ArgumentCaptor.forClass(HerbIdentificationResultEntity.class);
        verify(identificationResultMapper).insertResult(captor.capture());
        assertThat(captor.getValue().getResultSource()).isEqualTo("local_match");
        assertThat(captor.getValue().getMatchResult()).isEqualTo("matched");
        assertThat(captor.getValue().getNeedReview()).isZero();
        assertThat(result.getNeedReview()).isFalse();
        verify(herbRecognitionService, never()).recognizeByDoubao(any());
    }

    @Test
    void identifyKeepsLocalMatchAsPrimaryWhenLocalMatchIsLowConfidence() {
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image());
        when(herbImageFeatureMapper.selectLatestSuccessByImageId(1L))
                .thenReturn(new FeatureExtractResultVO());
        when(herbImageMatchService.match(any(), any())).thenReturn(localMatch("0.5100"));
        when(herbRecognitionService.recognizeByDoubao(1L)).thenReturn(doubaoRecognition());

        HerbIdentificationVO result =
                herbIdentificationService.identify(1L, new HerbIdentifyRequest());

        ArgumentCaptor<HerbIdentificationResultEntity> captor =
                ArgumentCaptor.forClass(HerbIdentificationResultEntity.class);
        verify(identificationResultMapper).insertResult(captor.capture());
        assertThat(captor.getValue().getResultSource()).isEqualTo("local_match");
        assertThat(captor.getValue().getRecognitionId()).isEqualTo(20L);
        assertThat(captor.getValue().getFinalSpeciesName()).isEqualTo("Dangshen");
        assertThat(captor.getValue().getFinalConfidence()).isEqualByComparingTo("0.5100");
        assertThat(captor.getValue().getNeedReview()).isEqualTo(1);
        assertThat(result.getFinalSpeciesName()).isEqualTo("Dangshen");
        assertThat(result.getDoubaoRecognition().getPredictedName()).isEqualTo("Wuzhimaotao");
    }

    @Test
    void identifyExtractsFeatureWhenMissing() {
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image());
        when(herbImageFeatureMapper.selectLatestSuccessByImageId(1L)).thenReturn(null);
        when(herbImageMatchService.match(any(), any())).thenReturn(localMatch("0.9100"));

        herbIdentificationService.identify(1L, new HerbIdentifyRequest());

        verify(herbFeatureService).extractImageFeature(1L);
    }

    @Test
    void identifyKeepsDoubaoFailureReasonWhenLowConfidenceReviewFails() {
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image());
        when(herbImageFeatureMapper.selectLatestSuccessByImageId(1L))
                .thenReturn(new FeatureExtractResultVO());
        when(herbImageMatchService.match(any(), any())).thenReturn(localMatch("0.5100"));
        when(herbRecognitionService.recognizeByDoubao(1L))
                .thenThrow(new RuntimeException("timeout"));

        herbIdentificationService.identify(1L, new HerbIdentifyRequest());

        ArgumentCaptor<HerbIdentificationResultEntity> captor =
                ArgumentCaptor.forClass(HerbIdentificationResultEntity.class);
        verify(identificationResultMapper).insertResult(captor.capture());
        assertThat(captor.getValue().getResultSource()).isEqualTo("local_match");
        assertThat(captor.getValue().getNeedReview()).isEqualTo(1);
        assertThat(captor.getValue().getSuggestion()).contains("timeout");
        assertThat(captor.getValue().getRawSummary()).contains("timeout");
    }

    @Test
    void reviewConfirmsIdentificationResult() {
        HerbIdentificationResultEntity existing = new HerbIdentificationResultEntity();
        existing.setId(9L);
        existing.setImageId(1L);
        when(identificationResultMapper.selectActiveById(9L)).thenReturn(existing);
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image());
        HerbEntity species = new HerbEntity();
        species.setId(2L);
        species.setHerbName("Dangshen");
        when(herbSpeciesMapper.selectActiveById(2L)).thenReturn(species);
        HerbIdentificationReviewRequest request = new HerbIdentificationReviewRequest();
        request.setFinalSpeciesId(2L);
        request.setReviewComment("confirmed");

        herbIdentificationService.review(9L, request);

        ArgumentCaptor<HerbIdentificationResultEntity> captor =
                ArgumentCaptor.forClass(HerbIdentificationResultEntity.class);
        verify(identificationResultMapper).updateReviewResult(captor.capture());
        assertThat(captor.getValue().getResultSource()).isEqualTo("manual_review");
        assertThat(captor.getValue().getNeedReview()).isZero();
        assertThat(captor.getValue().getReviewStatus()).isEqualTo("confirmed");
        assertThat(captor.getValue().getReviewerId()).isEqualTo(9L);
        assertThat(captor.getValue().getReviewerName()).isEqualTo("Reviewer A");
    }

    private HerbIdentificationProperties identificationProperties() {
        HerbIdentificationProperties properties = new HerbIdentificationProperties();
        properties.setHighThreshold(new BigDecimal("0.85"));
        properties.setMiddleThreshold(new BigDecimal("0.60"));
        properties.setTopK(5);
        properties.setEnableDoubaoReview(true);
        return properties;
    }

    private HerbImageEntity image() {
        HerbImageEntity image = new HerbImageEntity();
        image.setId(1L);
        image.setImageNo("IMG_1");
        image.setImageUrl("/herb/image/1.jpg");
        return image;
    }

    private HerbImageMatchVO localMatch(String similarity) {
        HerbImageMatchVO match = new HerbImageMatchVO();
        match.setImageId(1L);
        match.setImageCode("IMG_1");
        match.setBestSpeciesId(2L);
        match.setBestSpeciesName("Dangshen");
        match.setBestSimilarity(new BigDecimal(similarity));
        match.setCandidates(List.of());
        return match;
    }

    private HerbRecognitionVO doubaoRecognition() {
        HerbRecognitionVO recognition = new HerbRecognitionVO();
        recognition.setId(20L);
        recognition.setPredictedSpeciesId(null);
        recognition.setPredictedName("Wuzhimaotao");
        recognition.setConfidence(new BigDecimal("0.7000"));
        return recognition;
    }
}
