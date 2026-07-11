package com.bdis.modules.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchCreateRequest;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchUpdateRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.impl.HerbBatchServiceImpl;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbBatchServiceTest {

    @Mock private HerbBatchMapper herbBatchMapper;

    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private HerbBatchImageMapper herbBatchImageMapper;

    @Mock private CollectionAccessService collectionAccessService;

    private HerbBatchService herbBatchService;

    @BeforeEach
    void setUp() {
        herbBatchService =
                new HerbBatchServiceImpl(
                        herbBatchMapper,
                        herbCollectionTaskMapper,
                        herbSpeciesMapper,
                        herbBatchImageMapper,
                        collectionAccessService);
    }

    @Test
    void createBatchSetsDefaultsAndFillsSpeciesNameFromSpecies() {
        HerbBatchCreateRequest request = new HerbBatchCreateRequest();
        request.setBatchCode("BATCH_20260710_001");
        request.setBatchName("Huanglian batch 001");
        request.setSpeciesId(1L);

        HerbEntity species = new HerbEntity();
        species.setId(1L);
        species.setHerbName("Huanglian");
        when(herbBatchMapper.selectByBatchCode("BATCH_20260710_001")).thenReturn(null);
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(species);

        HerbBatchVO result = herbBatchService.create(request);

        ArgumentCaptor<HerbBatchEntity> captor = ArgumentCaptor.forClass(HerbBatchEntity.class);
        verify(herbBatchMapper).insert(captor.capture());
        HerbBatchEntity inserted = captor.getValue();
        assertThat(inserted.getBatchStatus()).isEqualTo(HerbBatchStatusConstants.DRAFT);
        assertThat(inserted.getSpeciesName()).isEqualTo("Huanglian");
        assertThat(inserted.getImageCount()).isZero();
        assertThat(inserted.getIdentifiedCount()).isZero();
        assertThat(inserted.getReviewedCount()).isZero();
        assertThat(inserted.getNeedReviewCount()).isZero();
        assertThat(inserted.getIsDeleted()).isZero();
        assertThat(inserted.getCreatedAt()).isNotNull();
        assertThat(inserted.getUpdatedAt()).isNotNull();
        assertThat(result.getBatchCode()).isEqualTo("BATCH_20260710_001");
    }

    @Test
    void createBatchRejectsDuplicateBatchCode() {
        HerbBatchCreateRequest request = new HerbBatchCreateRequest();
        request.setBatchCode("BATCH_20260710_001");
        request.setBatchName("Huanglian batch 001");
        when(herbBatchMapper.selectByBatchCode("BATCH_20260710_001"))
                .thenReturn(new HerbBatchEntity());

        assertThatThrownBy(() -> herbBatchService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Batch code already exists");
    }

    @Test
    void updateBatchRejectsInvalidStatus() {
        HerbBatchEntity existing = activeBatch();
        HerbBatchUpdateRequest request = new HerbBatchUpdateRequest();
        request.setBatchName("Updated batch");
        request.setBatchStatus("unknown");
        when(herbBatchMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> herbBatchService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid batch status");
    }

    @Test
    void updateBatchRejectsDirectStatusChange() {
        HerbBatchEntity existing = activeBatch();
        HerbBatchUpdateRequest request = new HerbBatchUpdateRequest();
        request.setBatchName("Updated batch");
        request.setBatchStatus(HerbBatchStatusConstants.COLLECTING);
        when(herbBatchMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> herbBatchService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("status APIs");
    }

    @Test
    void updateBatchRejectsArchivedBatch() {
        HerbBatchEntity existing = activeBatch();
        existing.setBatchStatus(HerbBatchStatusConstants.ARCHIVED);
        HerbBatchUpdateRequest request = new HerbBatchUpdateRequest();
        request.setBatchName("Updated batch");
        when(herbBatchMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> herbBatchService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Archived or cancelled batch cannot be updated");
    }

    @Test
    void deleteBatchRejectsBatchWithBoundImages() {
        HerbBatchEntity existing = activeBatch();
        when(herbBatchMapper.selectById(1L)).thenReturn(existing);
        when(herbBatchImageMapper.countByBatchId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> herbBatchService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bound collection images");
    }

    @Test
    void pageBatchNormalizesInvalidPageParameters() {
        HerbBatchQueryRequest request = new HerbBatchQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        CollectionAccessScope scope = new CollectionAccessScope(false, List.of(1001L));
        when(collectionAccessService.currentScope()).thenReturn(scope);
        when(herbBatchMapper.countPage(request, scope)).thenReturn(1L);
        when(herbBatchMapper.selectPage(request, scope, 0L, 10)).thenReturn(List.of(activeVO()));

        PageResult<HerbBatchVO> result = herbBatchService.page(request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void listSelectableBatchesExcludesArchivedAndCancelledByDefault() {
        HerbBatchQueryRequest request = new HerbBatchQueryRequest();
        CollectionAccessScope scope = new CollectionAccessScope(false, List.of(1001L));
        when(collectionAccessService.currentScope()).thenReturn(scope);
        when(herbBatchMapper.selectList(request, scope)).thenReturn(List.of(activeListVO()));

        List<HerbBatchListVO> result = herbBatchService.list(request);

        assertThat(request.getExcludedStatuses())
                .containsExactly(
                        HerbBatchStatusConstants.ARCHIVED, HerbBatchStatusConstants.CANCELLED);
        assertThat(result).hasSize(1);
    }

    @Test
    void createBatchValidatesCollectionTaskWhenTaskIdProvided() {
        HerbBatchCreateRequest request = new HerbBatchCreateRequest();
        request.setBatchCode("BATCH_20260710_001");
        request.setBatchName("Huanglian batch 001");
        request.setTaskId(10L);
        when(herbBatchMapper.selectByBatchCode("BATCH_20260710_001")).thenReturn(null);
        when(herbCollectionTaskMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> herbBatchService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Collection task not found");
    }

    private HerbBatchEntity activeBatch() {
        HerbBatchEntity entity = new HerbBatchEntity();
        entity.setId(1L);
        entity.setBatchCode("BATCH_20260710_001");
        entity.setBatchName("Huanglian batch 001");
        entity.setBatchStatus(HerbBatchStatusConstants.DRAFT);
        entity.setImageCount(0);
        entity.setIdentifiedCount(0);
        entity.setReviewedCount(0);
        entity.setNeedReviewCount(0);
        entity.setIsDeleted(0);
        return entity;
    }

    private HerbBatchVO activeVO() {
        HerbBatchVO vo = new HerbBatchVO();
        vo.setId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch 001");
        vo.setBatchStatus(HerbBatchStatusConstants.DRAFT);
        vo.setImageCount(0);
        vo.setIdentifiedCount(0);
        vo.setReviewedCount(0);
        vo.setNeedReviewCount(0);
        return vo;
    }

    private HerbBatchListVO activeListVO() {
        HerbBatchListVO vo = new HerbBatchListVO();
        vo.setId(1L);
        vo.setBatchCode("BATCH_20260710_001");
        vo.setBatchName("Huanglian batch 001");
        vo.setBatchStatus(HerbBatchStatusConstants.DRAFT);
        return vo;
    }
}
