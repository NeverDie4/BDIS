package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.support.HerbImageAccessService;
import com.bdis.modules.spectrum.dto.HerbImageMatchRequest;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbImageMatchMapper;
import com.bdis.modules.spectrum.service.impl.HerbImageMatchServiceImpl;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import com.bdis.modules.spectrum.vo.HerbAtlasFeatureCandidateVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchPageVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbImageMatchServiceTest {

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private HerbImageFeatureMapper herbImageFeatureMapper;

    @Mock private HerbImageMatchMapper herbImageMatchMapper;

    @Mock private HerbImageAccessService herbImageAccessService;

    private HerbImageMatchService herbImageMatchService;

    @BeforeEach
    void setUp() {
        herbImageMatchService =
                new HerbImageMatchServiceImpl(
                        herbImageMapper,
                        herbImageFeatureMapper,
                        herbImageMatchMapper,
                        new ObjectMapper(),
                        herbImageAccessService);
    }

    @Test
    void matchSavesTopCandidatesAndReturnsMatchedResult() {
        HerbImageEntity image = image();
        HerbImageMatchRequest request = new HerbImageMatchRequest();
        request.setTopK(2);
        request.setForceRefresh(true);
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbImageFeatureMapper.selectLatestSuccessByImageId(1L))
                .thenReturn(imageFeature("[1.0,0.0]"));
        when(herbImageMatchMapper.selectAtlasFeatureCandidates(null))
                .thenReturn(List.of(atlas(10L, "[1.0,0.0]"), atlas(11L, "[0.0,1.0]")));
        when(herbImageMatchMapper.selectLatestByImageId(1L))
                .thenReturn(List.of(matchPage(10L, 1, "matched", "1.0000")));

        HerbImageMatchVO result = herbImageMatchService.match(1L, request);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(herbImageMatchMapper).batchInsertMatches(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(result.getMatchResult()).isEqualTo("matched");
        assertThat(result.getNeedReview()).isFalse();
        assertThat(result.getBestSimilarity()).isEqualByComparingTo("1.0000");
    }

    @Test
    void matchReturnsLatestWhenExistingAndForceRefreshIsFalse() {
        HerbImageEntity image = image();
        HerbImageMatchRequest request = new HerbImageMatchRequest();
        request.setForceRefresh(false);
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbImageMatchMapper.countActiveByImageId(1L)).thenReturn(1);
        when(herbImageMatchMapper.selectLatestByImageId(1L))
                .thenReturn(List.of(matchPage(10L, 1, "uncertain", "0.7000")));

        HerbImageMatchVO result = herbImageMatchService.match(1L, request);

        assertThat(result.getMatchResult()).isEqualTo("uncertain");
        verify(herbImageFeatureMapper, never()).selectLatestSuccessByImageId(any());
        verify(herbImageMatchMapper, never()).batchInsertMatches(any());
    }

    @Test
    void matchSkipsInvalidAtlasVectorAndKeepsValidCandidate() {
        HerbImageEntity image = image();
        HerbImageMatchRequest request = new HerbImageMatchRequest();
        request.setForceRefresh(true);
        when(herbImageMapper.selectActiveById(1L)).thenReturn(image);
        when(herbImageFeatureMapper.selectLatestSuccessByImageId(1L))
                .thenReturn(imageFeature("[1.0,0.0]"));
        when(herbImageMatchMapper.selectAtlasFeatureCandidates(null))
                .thenReturn(List.of(atlas(10L, "[1.0]"), atlas(11L, "[1.0,0.0]")));
        when(herbImageMatchMapper.selectLatestByImageId(1L))
                .thenReturn(List.of(matchPage(11L, 1, "matched", "1.0000")));

        HerbImageMatchVO result = herbImageMatchService.match(1L, request);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(herbImageMatchMapper).batchInsertMatches(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(result.getBestAtlasId()).isEqualTo(11L);
    }

    private HerbImageEntity image() {
        HerbImageEntity image = new HerbImageEntity();
        image.setId(1L);
        image.setImageNo("IMG_1");
        return image;
    }

    private FeatureExtractResultVO imageFeature(String vector) {
        FeatureExtractResultVO feature = new FeatureExtractResultVO();
        feature.setImageId(1L);
        feature.setFeatureVector(vector);
        return feature;
    }

    private HerbAtlasFeatureCandidateVO atlas(Long atlasId, String vector) {
        HerbAtlasFeatureCandidateVO atlas = new HerbAtlasFeatureCandidateVO();
        atlas.setAtlasId(atlasId);
        atlas.setAtlasCode("ATLAS_" + atlasId);
        atlas.setAtlasImageUrl("/herb/atlas/" + atlasId + ".jpg");
        atlas.setSpeciesId(2L);
        atlas.setSpeciesName("Dangshen");
        atlas.setFeatureVector(vector);
        return atlas;
    }

    private HerbImageMatchPageVO matchPage(
            Long atlasId, Integer rank, String matchResult, String similarity) {
        HerbImageMatchPageVO page = new HerbImageMatchPageVO();
        page.setImageId(1L);
        page.setImageCode("IMG_1");
        page.setAtlasId(atlasId);
        page.setAtlasCode("ATLAS_" + atlasId);
        page.setAtlasImageUrl("/herb/atlas/" + atlasId + ".jpg");
        page.setSpeciesId(2L);
        page.setSpeciesName("Dangshen");
        page.setMatchRank(rank);
        page.setMatchResult(matchResult);
        page.setSimilarityScore(new BigDecimal(similarity));
        return page;
    }
}
