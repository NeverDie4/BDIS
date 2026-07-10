package com.bdis.modules.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.constant.HerbQualityLevelConstants;
import com.bdis.modules.collection.dto.HerbBatchConfirmRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.service.impl.HerbBatchSummaryServiceImpl;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbBatchSummaryServiceTest {

    @Mock private HerbBatchMapper herbBatchMapper;

    @Mock private HerbBatchImageMapper herbBatchImageMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    private HerbBatchSummaryService herbBatchSummaryService;

    @BeforeEach
    void setUp() {
        herbBatchSummaryService =
                new HerbBatchSummaryServiceImpl(
                        herbBatchMapper, herbBatchImageMapper, herbSpeciesMapper);
    }

    @Test
    void refreshSummaryConfirmsBatchWhenAllImagesAgreeAndReviewed() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.REVIEWING));
        when(herbBatchImageMapper.selectBatchImagesWithIdentification(any(), any()))
                .thenReturn(
                        List.of(
                                item(12L, 1L, "Huanglian", "0.9123", false, "confirmed"),
                                item(13L, 1L, "Huanglian", "0.8627", false, "confirmed")));
        when(herbBatchMapper.updateStatisticsById(any())).thenReturn(1);

        HerbBatchSummaryVO result = herbBatchSummaryService.refreshSummary(1L);

        assertThat(result.getImageCount()).isEqualTo(2);
        assertThat(result.getIdentifiedCount()).isEqualTo(2);
        assertThat(result.getReviewedCount()).isEqualTo(2);
        assertThat(result.getNeedReviewCount()).isZero();
        assertThat(result.getFinalSpeciesId()).isEqualTo(1L);
        assertThat(result.getMainSpeciesRatio()).isEqualByComparingTo("1.0000");
        assertThat(result.getAvgSimilarity()).isEqualByComparingTo("0.8875");
        assertThat(result.getQualityScore()).isEqualByComparingTo("88.75");
        assertThat(result.getQualityLevel()).isEqualTo(HerbQualityLevelConstants.GOOD);
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.CONFIRMED);

        ArgumentCaptor<HerbBatchEntity> captor = ArgumentCaptor.forClass(HerbBatchEntity.class);
        verify(herbBatchMapper).updateStatisticsById(captor.capture());
        assertThat(captor.getValue().getFinalSpeciesName()).isEqualTo("Huanglian");
        assertThat(captor.getValue().getBatchStatus())
                .isEqualTo(HerbBatchStatusConstants.CONFIRMED);
    }

    @Test
    void refreshSummarySetsIdentifyingWhenSomeImagesAreMissingResults() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.SUBMITTED));
        when(herbBatchImageMapper.selectBatchImagesWithIdentification(any(), any()))
                .thenReturn(
                        List.of(
                                item(12L, 1L, "Huanglian", "0.9000", false, "confirmed"),
                                item(13L)));
        when(herbBatchMapper.updateStatisticsById(any())).thenReturn(1);

        HerbBatchSummaryVO result = herbBatchSummaryService.refreshSummary(1L);

        assertThat(result.getImageCount()).isEqualTo(2);
        assertThat(result.getIdentifiedCount()).isEqualTo(1);
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.IDENTIFYING);
        assertThat(result.getEvaluationSummary()).contains("仍有图片未生成识别结论");
    }

    @Test
    void refreshSummaryKeepsCollectingBatchStatusBeforeSubmit() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.COLLECTING));
        when(herbBatchImageMapper.selectBatchImagesWithIdentification(any(), any()))
                .thenReturn(List.of(item(12L, 1L, "Huanglian", "0.9000", true, "pending")));
        when(herbBatchMapper.updateStatisticsById(any())).thenReturn(1);

        HerbBatchSummaryVO result = herbBatchSummaryService.refreshSummary(1L);

        assertThat(result.getImageCount()).isEqualTo(1);
        assertThat(result.getIdentifiedCount()).isEqualTo(1);
        assertThat(result.getNeedReviewCount()).isEqualTo(1);
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.COLLECTING);
        ArgumentCaptor<HerbBatchEntity> captor = ArgumentCaptor.forClass(HerbBatchEntity.class);
        verify(herbBatchMapper).updateStatisticsById(captor.capture());
        assertThat(captor.getValue().getBatchStatus())
                .isEqualTo(HerbBatchStatusConstants.COLLECTING);
    }

    @Test
    void refreshSummarySetsReviewingWhenMainSpeciesRatioIsLow() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.REVIEWING));
        when(herbBatchImageMapper.selectBatchImagesWithIdentification(any(), any()))
                .thenReturn(
                        List.of(
                                item(12L, 1L, "Huanglian", "0.9000", false, "confirmed"),
                                item(13L, 2L, "Jinyinhua", "0.8800", false, "confirmed")));
        when(herbBatchMapper.updateStatisticsById(any())).thenReturn(1);

        HerbBatchSummaryVO result = herbBatchSummaryService.refreshSummary(1L);

        assertThat(result.getMainSpeciesRatio()).isEqualByComparingTo("0.5000");
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.REVIEWING);
        assertThat(result.getEvaluationSummary()).contains("识别结果存在不一致");
    }

    @Test
    void refreshSummaryRejectsArchivedBatch() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.ARCHIVED));

        assertThatThrownBy(() -> herbBatchSummaryService.refreshSummary(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot refresh summary");
    }

    @Test
    void confirmRejectsCancelledBatch() {
        HerbBatchConfirmRequest request = new HerbBatchConfirmRequest();
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.CANCELLED));

        assertThatThrownBy(() -> herbBatchSummaryService.confirm(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Archived or cancelled batch cannot confirm summary");
    }

    @Test
    void confirmBatchValidatesSpeciesAndUpdatesConfirmedFields() {
        HerbBatchConfirmRequest request = new HerbBatchConfirmRequest();
        request.setFinalSpeciesId(1L);
        request.setQualityLevel(HerbQualityLevelConstants.GOOD);
        request.setQualityScore(new BigDecimal("88.75"));
        request.setEvaluationSummary("人工确认该批次为黄连，批次质量良好。");
        request.setReviewerName("Admin");
        request.setRemark("批次人工确认");
        HerbEntity species = new HerbEntity();
        species.setId(1L);
        species.setHerbName("Huanglian");
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.REVIEWING));
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(species);
        when(herbBatchImageMapper.selectBatchImagesWithIdentification(any(), any()))
                .thenReturn(List.of());
        when(herbBatchMapper.updateStatisticsById(any())).thenReturn(1);

        HerbBatchSummaryVO result = herbBatchSummaryService.confirm(1L, request);

        assertThat(result.getFinalSpeciesName()).isEqualTo("Huanglian");
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.CONFIRMED);
        ArgumentCaptor<HerbBatchEntity> captor = ArgumentCaptor.forClass(HerbBatchEntity.class);
        verify(herbBatchMapper).updateStatisticsById(captor.capture());
        assertThat(captor.getValue().getRemark()).contains("Admin");
    }

    @Test
    void confirmBatchRejectsInvalidQualityLevel() {
        HerbBatchConfirmRequest request = new HerbBatchConfirmRequest();
        request.setQualityLevel("bad-level");
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.REVIEWING));

        assertThatThrownBy(() -> herbBatchSummaryService.confirm(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid quality level");
    }

    private HerbBatchEntity activeBatch(String status) {
        HerbBatchEntity entity = new HerbBatchEntity();
        entity.setId(1L);
        entity.setBatchCode("BATCH_20260710_001");
        entity.setBatchName("Huanglian batch");
        entity.setBatchStatus(status);
        return entity;
    }

    private HerbBatchImageVO item(Long imageId) {
        HerbBatchImageVO vo = new HerbBatchImageVO();
        vo.setId(imageId);
        vo.setBatchId(1L);
        vo.setImageId(imageId);
        vo.setImageCode("IMG_" + imageId);
        vo.setImageUrl("/image/" + imageId + ".jpg");
        vo.setImageRole("leaf");
        return vo;
    }

    private HerbBatchImageVO item(
            Long imageId,
            Long speciesId,
            String speciesName,
            String confidence,
            boolean needReview,
            String reviewStatus) {
        HerbBatchImageVO vo = item(imageId);
        vo.setIdentificationResultId(imageId + 100L);
        vo.setFinalSpeciesId(speciesId);
        vo.setFinalSpeciesName(speciesName);
        vo.setFinalConfidence(new BigDecimal(confidence));
        vo.setNeedReview(needReview);
        vo.setReviewStatus(reviewStatus);
        return vo;
    }
}
