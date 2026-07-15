package com.bdis.modules.mobile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchStatusService;
import com.bdis.modules.collection.service.HerbBatchSummaryService;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.herb.service.HerbImageService;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.mobile.dto.MobileBatchImageUploadRequest;
import com.bdis.modules.mobile.service.impl.MobileBatchAutoIdentificationExecutor;
import com.bdis.modules.mobile.service.impl.MobileHerbBatchServiceImpl;
import com.bdis.modules.mobile.vo.MobileImageIdentificationVO;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class MobileHerbBatchServiceImplTest {

    @Mock private HerbBatchMapper herbBatchMapper;
    @Mock private HerbBatchImageMapper herbBatchImageMapper;
    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;
    @Mock private HerbImageService herbImageService;
    @Mock private HerbBatchImageService herbBatchImageService;
    @Mock private HerbIdentificationService herbIdentificationService;
    @Mock private HerbBatchSummaryService herbBatchSummaryService;
    @Mock private HerbBatchStatusService herbBatchStatusService;
    @Mock private MobileBatchAutoIdentificationExecutor autoIdentificationExecutor;
    @Mock private GrowthRecordService growthRecordService;

    @InjectMocks private MobileHerbBatchServiceImpl service;

    @Test
    void uploadRequestEnablesAutoIdentifyByDefault() {
        MobileBatchImageUploadRequest request = new MobileBatchImageUploadRequest();

        assertThat(request.getAutoIdentify()).isTrue();
    }

    @Test
    void autoIdentificationExecutorUsesIndependentTransaction() throws Exception {
        var method =
                MobileBatchAutoIdentificationExecutor.class.getMethod(
                        "identifyBoundImage",
                        Long.class,
                        Long.class,
                        com.bdis.modules.mobile.dto.MobileBatchIdentifyRequest.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        Async async = method.getAnnotation(Async.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("mobileAutoIdentificationExecutor");
    }

    @Test
    void latestIdentificationIncludesCompleteImageInformation() throws Exception {
        HerbIdentificationVO identification = new HerbIdentificationVO();
        identification.setImageId(1L);
        identification.setImageCode("IMG_1");
        identification.setImageUrl("/api/files/1/content");

        LocalDateTime collectTime = LocalDateTime.of(2026, 7, 12, 13, 38, 33);
        HerbImageVO image = new HerbImageVO();
        image.setId(1L);
        image.setImageName("wuzhimaotao.jpg");
        image.setImageType("whole");
        image.setCollectPlace("广东省河源市");
        image.setCollectTime(collectTime);

        when(herbIdentificationService.latest(1L)).thenReturn(identification);
        when(herbImageService.getById(1L)).thenReturn(image);

        MobileImageIdentificationVO result = service.latestIdentification(1L);

        assertThat(fieldValue(result, "imageName")).isEqualTo("wuzhimaotao.jpg");
        assertThat(fieldValue(result, "imageRole")).isEqualTo("whole");
        assertThat(fieldValue(result, "collectPlace")).isEqualTo("广东省河源市");
        assertThat(fieldValue(result, "collectTime")).isEqualTo(collectTime);
    }

    @Test
    void submitBatchAlsoSubmitsItsDraftGrowthRecord() {
        Long batchId = 7L;
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(batchId);
        batch.setTaskId(3L);
        batch.setBatchStatus(HerbBatchStatusConstants.COLLECTING);

        GrowthRecordVO growthRecord = new GrowthRecordVO();
        growthRecord.setId(11L);
        growthRecord.setReviewStatus("draft");

        HerbBatchVO detail = new HerbBatchVO();
        detail.setId(batchId);
        detail.setTaskId(3L);
        detail.setBatchStatus(HerbBatchStatusConstants.SUBMITTED);

        when(herbBatchMapper.selectById(batchId)).thenReturn(batch);
        when(growthRecordService.getByBatchId(batchId)).thenReturn(growthRecord);
        when(herbBatchMapper.selectDetailById(batchId)).thenReturn(detail);
        when(herbBatchImageService.listByBatch(any(), any())).thenReturn(List.of());

        service.submit(batchId, null);

        var ordered = inOrder(growthRecordService, herbBatchStatusService);
        ordered.verify(growthRecordService).submit(11L);
        ordered.verify(herbBatchStatusService).submit(batchId);
    }

    @Test
    void submitBatchRejectsMissingGrowthRecordWithoutChangingBatchStatus() {
        Long batchId = 7L;
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(batchId);
        batch.setTaskId(3L);
        batch.setBatchStatus(HerbBatchStatusConstants.COLLECTING);

        when(herbBatchMapper.selectById(batchId)).thenReturn(batch);
        when(growthRecordService.getByBatchId(batchId)).thenReturn(null);

        assertThatThrownBy(() -> service.submit(batchId, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先填写本次生长记录后再提交审核");
        verify(herbBatchStatusService, never()).submit(batchId);
    }

    private Object fieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
