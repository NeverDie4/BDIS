package com.bdis.modules.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchImageStatusConstants;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchImageBatchBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageUpdateRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbBatchImageEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.service.impl.HerbBatchImageServiceImpl;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbBatchImageBindResultVO;
import com.bdis.modules.collection.vo.HerbBatchImageStatisticsVO;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.spectrum.entity.HerbIdentificationResultEntity;
import com.bdis.modules.spectrum.mapper.HerbIdentificationResultMapper;
import com.bdis.modules.spectrum.vo.HerbIdentificationPageVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbBatchImageServiceTest {

    @Mock private HerbBatchImageMapper herbBatchImageMapper;

    @Mock private HerbBatchMapper herbBatchMapper;

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private HerbIdentificationResultMapper herbIdentificationResultMapper;

    @Mock private CollectionAccessService collectionAccessService;

    private HerbBatchImageService herbBatchImageService;

    @BeforeEach
    void setUp() {
        herbBatchImageService =
                new HerbBatchImageServiceImpl(
                        herbBatchImageMapper,
                        herbBatchMapper,
                        herbImageMapper,
                        herbIdentificationResultMapper,
                        collectionAccessService);
    }

    @Test
    void bindImageSetsDefaultsAndRefreshesImageCount() {
        HerbBatchImageBindRequest request = bindRequest();
        request.setIdentificationResultId(null);
        request.setImageRole(null);
        request.setIsPrimary(1);

        HerbIdentificationPageVO latest = new HerbIdentificationPageVO();
        latest.setId(5L);
        latest.setImageId(12L);
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbImageMapper.selectActiveById(12L)).thenReturn(activeImage());
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L)).thenReturn(null);
        when(herbIdentificationResultMapper.selectLatestByImageId(12L)).thenReturn(latest);
        when(herbBatchImageMapper.countBoundByBatchId(1L)).thenReturn(1L);
        when(herbBatchImageMapper.selectDetailByBatchIdAndImageId(1L, 12L)).thenReturn(activeVO());

        HerbBatchImageVO result = herbBatchImageService.bind(1L, request);

        ArgumentCaptor<HerbBatchImageEntity> captor =
                ArgumentCaptor.forClass(HerbBatchImageEntity.class);
        verify(herbBatchImageMapper).clearPrimaryByBatchId(1L);
        verify(herbBatchImageMapper).insert(captor.capture());
        HerbBatchImageEntity inserted = captor.getValue();
        assertThat(inserted.getImageRole()).isEqualTo("other");
        assertThat(inserted.getBindStatus()).isEqualTo(HerbBatchImageStatusConstants.BOUND);
        assertThat(inserted.getIdentificationResultId()).isEqualTo(5L);
        assertThat(inserted.getIsDeleted()).isZero();
        assertThat(result.getImageId()).isEqualTo(12L);
        verify(herbBatchMapper).updateStatisticsById(any(HerbBatchEntity.class));
    }

    @Test
    void bindImageRejectsArchivedBatch() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.ARCHIVED));

        assertThatThrownBy(() -> herbBatchImageService.bind(1L, bindRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot maintain images");
    }

    @Test
    void bindImageRejectsDuplicateBinding() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbImageMapper.selectActiveById(12L)).thenReturn(activeImage());
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L))
                .thenReturn(new HerbBatchImageEntity());

        assertThatThrownBy(() -> herbBatchImageService.bind(1L, bindRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already bound");
    }

    @Test
    void bindImageRejectsIdentificationResultFromAnotherImage() {
        HerbBatchImageBindRequest request = bindRequest();
        request.setIdentificationResultId(5L);
        HerbIdentificationResultEntity result = new HerbIdentificationResultEntity();
        result.setId(5L);
        result.setImageId(99L);
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbImageMapper.selectActiveById(12L)).thenReturn(activeImage());
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L)).thenReturn(null);
        when(herbIdentificationResultMapper.selectActiveById(5L)).thenReturn(result);

        assertThatThrownBy(() -> herbBatchImageService.bind(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to image");
    }

    @Test
    void batchBindKeepsOnlyFirstPrimaryImage() {
        HerbBatchImageBatchBindRequest request = new HerbBatchImageBatchBindRequest();
        HerbBatchImageBindRequest first = bindRequest();
        first.setImageId(12L);
        first.setIsPrimary(1);
        HerbBatchImageBindRequest second = bindRequest();
        second.setImageId(13L);
        second.setIsPrimary(1);
        request.setImages(List.of(first, second));

        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbImageMapper.selectActiveById(12L)).thenReturn(activeImage(12L));
        when(herbImageMapper.selectActiveById(13L)).thenReturn(activeImage(13L));
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L)).thenReturn(null);
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 13L)).thenReturn(null);
        when(herbBatchImageMapper.countBoundByBatchId(1L)).thenReturn(2L);

        HerbBatchImageBindResultVO result = herbBatchImageService.batchBind(1L, request);

        ArgumentCaptor<List<HerbBatchImageEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(herbBatchImageMapper).batchInsert(captor.capture());
        assertThat(captor.getValue())
                .extracting(HerbBatchImageEntity::getIsPrimary)
                .containsExactly(1, 0);
        assertThat(result.getSuccessCount()).isEqualTo(2);
    }

    @Test
    void unbindImageMarksBindingRemovedAndRefreshesImageCount() {
        HerbBatchImageEntity binding = activeBinding();
        binding.setIsPrimary(1);
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L)).thenReturn(binding);
        when(herbBatchImageMapper.logicDeleteByBatchIdAndImageId(1L, 12L)).thenReturn(1);
        when(herbBatchImageMapper.countBoundByBatchId(1L)).thenReturn(0L);

        herbBatchImageService.unbind(1L, 12L);

        verify(herbBatchImageMapper).logicDeleteByBatchIdAndImageId(1L, 12L);
        verify(herbBatchMapper).updateStatisticsById(any(HerbBatchEntity.class));
    }

    @Test
    void setPrimaryClearsOtherPrimaryBindings() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L)).thenReturn(activeBinding());
        when(herbBatchImageMapper.selectDetailByBatchIdAndImageId(1L, 12L)).thenReturn(activeVO());

        herbBatchImageService.setPrimary(1L, 12L);

        verify(herbBatchImageMapper).clearPrimaryByBatchId(1L);
        verify(herbBatchImageMapper).updatePrimaryByBatchIdAndImageId(1L, 12L);
    }

    @Test
    void updateImageRoleRejectsUnknownRole() {
        HerbBatchImageUpdateRequest request = new HerbBatchImageUpdateRequest();
        request.setImageRole("unknown");
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbBatchImageMapper.selectByBatchIdAndImageId(1L, 12L)).thenReturn(activeBinding());

        assertThatThrownBy(() -> herbBatchImageService.update(1L, 12L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid image role");
    }

    @Test
    void refreshStatisticsUpdatesBatchImageCount() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.DRAFT));
        when(herbBatchImageMapper.countBoundByBatchId(1L)).thenReturn(3L);

        HerbBatchImageStatisticsVO result = herbBatchImageService.refreshStatistics(1L);

        assertThat(result.getImageCount()).isEqualTo(3);
        verify(herbBatchMapper).updateStatisticsById(any(HerbBatchEntity.class));
    }

    @Test
    void refreshStatisticsRejectsArchivedBatch() {
        when(herbBatchMapper.selectById(1L))
                .thenReturn(activeBatch(HerbBatchStatusConstants.ARCHIVED));

        assertThatThrownBy(() -> herbBatchImageService.refreshStatistics(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Archived or cancelled batch cannot maintain images");
    }

    private HerbBatchImageBindRequest bindRequest() {
        HerbBatchImageBindRequest request = new HerbBatchImageBindRequest();
        request.setImageId(12L);
        request.setImageRole("leaf");
        request.setIsPrimary(0);
        request.setSortOrder(1);
        return request;
    }

    private HerbBatchEntity activeBatch(String status) {
        HerbBatchEntity entity = new HerbBatchEntity();
        entity.setId(1L);
        entity.setBatchCode("BATCH_20260710_001");
        entity.setBatchName("Huanglian batch");
        entity.setBatchStatus(status);
        entity.setImageCount(0);
        return entity;
    }

    private HerbImageEntity activeImage() {
        return activeImage(12L);
    }

    private HerbImageEntity activeImage(Long id) {
        HerbImageEntity entity = new HerbImageEntity();
        entity.setId(id);
        entity.setImageNo("IMG_" + id);
        entity.setImageUrl("/image/" + id + ".jpg");
        entity.setOriginalFilename("image-" + id + ".jpg");
        return entity;
    }

    private HerbBatchImageEntity activeBinding() {
        HerbBatchImageEntity entity = new HerbBatchImageEntity();
        entity.setId(1L);
        entity.setBatchId(1L);
        entity.setImageId(12L);
        entity.setImageRole("leaf");
        entity.setBindStatus(HerbBatchImageStatusConstants.BOUND);
        entity.setIsPrimary(0);
        entity.setSortOrder(1);
        return entity;
    }

    private HerbBatchImageVO activeVO() {
        HerbBatchImageVO vo = new HerbBatchImageVO();
        vo.setId(1L);
        vo.setBatchId(1L);
        vo.setImageId(12L);
        vo.setImageCode("IMG_12");
        vo.setImageRole("leaf");
        vo.setBindStatus(HerbBatchImageStatusConstants.BOUND);
        return vo;
    }
}
