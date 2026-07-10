package com.bdis.modules.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchCancelRequest;
import com.bdis.modules.collection.dto.HerbBatchConfirmStatusRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.service.impl.HerbBatchStatusServiceImpl;
import com.bdis.modules.collection.vo.HerbBatchStatusVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbBatchStatusServiceTest {

    @Mock private HerbBatchMapper herbBatchMapper;

    private HerbBatchStatusService herbBatchStatusService;

    @BeforeEach
    void setUp() {
        herbBatchStatusService = new HerbBatchStatusServiceImpl(herbBatchMapper);
    }

    @Test
    void submitRejectsBatchWithoutBoundImages() {
        when(herbBatchMapper.selectById(1L)).thenReturn(batch(HerbBatchStatusConstants.COLLECTING));
        when(herbBatchMapper.countBoundImagesByBatchId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> herbBatchStatusService.submit(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该批次尚未绑定采集图片，不能提交");
    }

    @Test
    void startCollectionUpdatesDraftBatchToCollecting() {
        when(herbBatchMapper.selectById(1L)).thenReturn(batch(HerbBatchStatusConstants.DRAFT));
        when(herbBatchMapper.updateStatusById(any())).thenReturn(1);
        when(herbBatchMapper.selectDetailById(1L))
                .thenReturn(vo(HerbBatchStatusConstants.COLLECTING));

        HerbBatchVO result = herbBatchStatusService.startCollection(1L);

        ArgumentCaptor<HerbBatchEntity> captor = ArgumentCaptor.forClass(HerbBatchEntity.class);
        verify(herbBatchMapper).updateStatusById(captor.capture());
        assertThat(captor.getValue().getBatchStatus())
                .isEqualTo(HerbBatchStatusConstants.COLLECTING);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.COLLECTING);
    }

    @Test
    void confirmStatusRejectsPendingReviewWhenForceIsFalse() {
        HerbBatchConfirmStatusRequest request = new HerbBatchConfirmStatusRequest();
        request.setForce(false);
        when(herbBatchMapper.selectById(1L))
                .thenReturn(
                        batch(
                                HerbBatchStatusConstants.REVIEWING,
                                3,
                                2,
                                1,
                                "Huanglian",
                                "good",
                                "summary"));

        assertThatThrownBy(() -> herbBatchStatusService.confirmStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仍存在未识别或待复核图片");
    }

    @Test
    void forceConfirmAllowsPendingReviewAndRecordsRemark() {
        HerbBatchConfirmStatusRequest request = new HerbBatchConfirmStatusRequest();
        request.setForce(true);
        request.setRemark("人工确认");
        when(herbBatchMapper.selectById(1L))
                .thenReturn(
                        batch(
                                HerbBatchStatusConstants.REVIEWING,
                                3,
                                2,
                                1,
                                "Huanglian",
                                "good",
                                "summary"));
        when(herbBatchMapper.updateStatusById(any())).thenReturn(1);
        when(herbBatchMapper.selectDetailById(1L))
                .thenReturn(vo(HerbBatchStatusConstants.CONFIRMED));

        herbBatchStatusService.confirmStatus(1L, request);

        ArgumentCaptor<HerbBatchEntity> captor = ArgumentCaptor.forClass(HerbBatchEntity.class);
        verify(herbBatchMapper).updateStatusById(captor.capture());
        assertThat(captor.getValue().getBatchStatus())
                .isEqualTo(HerbBatchStatusConstants.CONFIRMED);
        assertThat(captor.getValue().getRemark()).contains("人工强制确认").contains("人工确认");
    }

    @Test
    void archiveRequiresConfirmedEvaluationFields() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(
                        batch(
                                HerbBatchStatusConstants.CONFIRMED,
                                1,
                                1,
                                0,
                                null,
                                "good",
                                "summary"));

        assertThatThrownBy(() -> herbBatchStatusService.archive(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("final species name");
    }

    @Test
    void cancelRejectsArchivedBatch() {
        HerbBatchCancelRequest request = new HerbBatchCancelRequest();
        request.setReason("采集任务取消");
        when(herbBatchMapper.selectById(1L)).thenReturn(batch(HerbBatchStatusConstants.ARCHIVED));

        assertThatThrownBy(() -> herbBatchStatusService.cancel(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Archived batch cannot be cancelled");
    }

    @Test
    void statusReturnsAllowedActionsAndStatistics() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(batch(HerbBatchStatusConstants.COLLECTING, 3, 0, 0, null, null, null));

        HerbBatchStatusVO result = herbBatchStatusService.status(1L);

        assertThat(result.getBatchId()).isEqualTo(1L);
        assertThat(result.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.COLLECTING);
        assertThat(result.getImageCount()).isEqualTo(3);
        assertThat(result.getAllowedActions()).containsExactly("submit", "cancel");
    }

    private HerbBatchEntity batch(String status) {
        return batch(status, 0, 0, 0, null, null, null);
    }

    private HerbBatchEntity batch(
            String status,
            Integer imageCount,
            Integer identifiedCount,
            Integer needReviewCount,
            String finalSpeciesName,
            String qualityLevel,
            String evaluationSummary) {
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(1L);
        batch.setBatchCode("BATCH_20260710_001");
        batch.setBatchName("Huanglian batch 001");
        batch.setBatchStatus(status);
        batch.setImageCount(imageCount);
        batch.setIdentifiedCount(identifiedCount);
        batch.setReviewedCount(identifiedCount == null ? 0 : identifiedCount);
        batch.setNeedReviewCount(needReviewCount);
        batch.setFinalSpeciesName(finalSpeciesName);
        batch.setQualityLevel(qualityLevel);
        batch.setEvaluationSummary(evaluationSummary);
        batch.setIsDeleted(0);
        return batch;
    }

    private HerbBatchVO vo(String status) {
        HerbBatchVO vo = new HerbBatchVO();
        vo.setId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch 001");
        vo.setBatchStatus(status);
        return vo;
    }
}
