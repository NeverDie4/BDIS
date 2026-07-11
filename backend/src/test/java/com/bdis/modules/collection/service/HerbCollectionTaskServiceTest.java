package com.bdis.modules.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbCollectionTaskStatusConstants;
import com.bdis.modules.collection.dto.HerbCollectionTaskCreateRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskUpdateRequest;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.impl.HerbCollectionTaskServiceImpl;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbCollectionTaskListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbCollectionTaskServiceTest {

    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private CollectionAccessService collectionAccessService;

    private HerbCollectionTaskService herbCollectionTaskService;

    @BeforeEach
    void setUp() {
        herbCollectionTaskService =
                new HerbCollectionTaskServiceImpl(
                        herbCollectionTaskMapper, herbSpeciesMapper, collectionAccessService);
    }

    @Test
    void createTaskSetsDefaultsAndFillsSpeciesNameFromSpecies() {
        HerbCollectionTaskCreateRequest request = createRequest();
        request.setSpeciesId(1L);
        request.setSpeciesName(null);

        HerbEntity species = new HerbEntity();
        species.setId(1L);
        species.setHerbName("Huanglian");
        when(herbCollectionTaskMapper.selectByTaskCode("TASK_20260710_001")).thenReturn(null);
        when(herbSpeciesMapper.selectActiveById(1L)).thenReturn(species);

        HerbCollectionTaskVO result = herbCollectionTaskService.create(request);

        ArgumentCaptor<HerbCollectionTaskEntity> captor =
                ArgumentCaptor.forClass(HerbCollectionTaskEntity.class);
        verify(herbCollectionTaskMapper).insert(captor.capture());
        HerbCollectionTaskEntity inserted = captor.getValue();
        assertThat(inserted.getTaskStatus()).isEqualTo(HerbCollectionTaskStatusConstants.DRAFT);
        assertThat(inserted.getSpeciesName()).isEqualTo("Huanglian");
        assertThat(inserted.getIsDeleted()).isZero();
        assertThat(inserted.getCreatedAt()).isNotNull();
        assertThat(inserted.getUpdatedAt()).isNotNull();
        assertThat(result.getTaskCode()).isEqualTo("TASK_20260710_001");
    }

    @Test
    void createTaskRejectsDuplicateTaskCode() {
        HerbCollectionTaskCreateRequest request = createRequest();
        when(herbCollectionTaskMapper.selectByTaskCode("TASK_20260710_001"))
                .thenReturn(activeTask(HerbCollectionTaskStatusConstants.DRAFT));

        assertThatThrownBy(() -> herbCollectionTaskService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Task code already exists");
    }

    @Test
    void createTaskRejectsEndTimeBeforeStartTime() {
        HerbCollectionTaskCreateRequest request = createRequest();
        request.setPlannedStartTime(LocalDateTime.of(2026, 7, 10, 18, 0));
        request.setPlannedEndTime(LocalDateTime.of(2026, 7, 10, 9, 0));

        assertThatThrownBy(() -> herbCollectionTaskService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("end time cannot be earlier than start time");
    }

    @Test
    void updateTaskRejectsInvalidStatus() {
        HerbCollectionTaskUpdateRequest request = updateRequest();
        request.setTaskStatus("unknown");
        when(herbCollectionTaskMapper.selectById(1L))
                .thenReturn(activeTask(HerbCollectionTaskStatusConstants.DRAFT));

        assertThatThrownBy(() -> herbCollectionTaskService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid collection task status");
    }

    @Test
    void deleteTaskRejectsTaskWithAssociatedBatches() {
        when(herbCollectionTaskMapper.selectById(1L))
                .thenReturn(activeTask(HerbCollectionTaskStatusConstants.DRAFT));
        when(herbCollectionTaskMapper.countBatchByTaskId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> herbCollectionTaskService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("associated batches");
    }

    @Test
    void pageTaskNormalizesInvalidPageParameters() {
        HerbCollectionTaskQueryRequest request = new HerbCollectionTaskQueryRequest();
        request.setPageNum(0);
        request.setPageSize(0);
        CollectionAccessScope scope = new CollectionAccessScope(false, List.of(1001L));
        when(collectionAccessService.currentScope()).thenReturn(scope);
        when(herbCollectionTaskMapper.countPage(request, scope)).thenReturn(1L);
        when(herbCollectionTaskMapper.selectPage(request, scope, 0L, 10))
                .thenReturn(List.of(activeVO()));

        PageResult<HerbCollectionTaskVO> result = herbCollectionTaskService.page(request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void myTasksUsePublishedAndInProgressByDefault() {
        HerbCollectionTaskMyQueryRequest request = new HerbCollectionTaskMyQueryRequest();
        request.setCollectorId(9999L);
        when(collectionAccessService.currentUserId()).thenReturn(1001L);
        when(herbCollectionTaskMapper.countMyTasks(request)).thenReturn(1L);
        when(herbCollectionTaskMapper.selectMyTasks(request, 0L, 10))
                .thenReturn(List.of(activeVO()));

        PageResult<HerbCollectionTaskVO> result = herbCollectionTaskService.myTasks(request);

        assertThat(request.getIncludedStatuses())
                .containsExactly(
                        HerbCollectionTaskStatusConstants.PUBLISHED,
                        HerbCollectionTaskStatusConstants.IN_PROGRESS);
        assertThat(request.getCollectorId()).isEqualTo(1001L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void publishTaskOnlyAllowsDraftStatus() {
        when(herbCollectionTaskMapper.selectById(1L))
                .thenReturn(activeTask(HerbCollectionTaskStatusConstants.IN_PROGRESS));

        assertThatThrownBy(() -> herbCollectionTaskService.publish(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only draft tasks can be published");
    }

    @Test
    void startTaskChangesPublishedToInProgress() {
        when(herbCollectionTaskMapper.selectById(1L))
                .thenReturn(activeTask(HerbCollectionTaskStatusConstants.PUBLISHED));
        when(herbCollectionTaskMapper.updateStatusById(
                        1L, HerbCollectionTaskStatusConstants.IN_PROGRESS))
                .thenReturn(1);
        when(herbCollectionTaskMapper.selectDetailById(1L)).thenReturn(activeVO());

        herbCollectionTaskService.start(1L);

        verify(herbCollectionTaskMapper)
                .updateStatusById(1L, HerbCollectionTaskStatusConstants.IN_PROGRESS);
    }

    @Test
    void listSelectableTasksExcludesCompletedAndCancelledByDefault() {
        HerbCollectionTaskQueryRequest request = new HerbCollectionTaskQueryRequest();
        CollectionAccessScope scope = new CollectionAccessScope(false, List.of(1001L));
        when(collectionAccessService.currentScope()).thenReturn(scope);
        when(herbCollectionTaskMapper.selectList(request, scope))
                .thenReturn(List.of(activeListVO()));

        List<HerbCollectionTaskListVO> result = herbCollectionTaskService.list(request);

        assertThat(request.getExcludedStatuses())
                .containsExactly(
                        HerbCollectionTaskStatusConstants.COMPLETED,
                        HerbCollectionTaskStatusConstants.CANCELLED);
        assertThat(result).hasSize(1);
    }

    private HerbCollectionTaskCreateRequest createRequest() {
        HerbCollectionTaskCreateRequest request = new HerbCollectionTaskCreateRequest();
        request.setTaskCode("TASK_20260710_001");
        request.setTaskName("Huanglian collection task");
        request.setPlannedStartTime(LocalDateTime.of(2026, 7, 10, 9, 0));
        request.setPlannedEndTime(LocalDateTime.of(2026, 7, 10, 18, 0));
        return request;
    }

    private HerbCollectionTaskUpdateRequest updateRequest() {
        HerbCollectionTaskUpdateRequest request = new HerbCollectionTaskUpdateRequest();
        request.setTaskName("Updated Huanglian collection task");
        request.setTaskStatus(HerbCollectionTaskStatusConstants.PUBLISHED);
        return request;
    }

    private HerbCollectionTaskEntity activeTask(String status) {
        HerbCollectionTaskEntity entity = new HerbCollectionTaskEntity();
        entity.setId(1L);
        entity.setTaskCode("TASK_20260710_001");
        entity.setTaskName("Huanglian collection task");
        entity.setTaskStatus(status);
        entity.setIsDeleted(0);
        return entity;
    }

    private HerbCollectionTaskVO activeVO() {
        HerbCollectionTaskVO vo = new HerbCollectionTaskVO();
        vo.setId(1L);
        vo.setTaskCode("TASK_20260710_001");
        vo.setTaskName("Huanglian collection task");
        vo.setTaskStatus(HerbCollectionTaskStatusConstants.PUBLISHED);
        return vo;
    }

    private HerbCollectionTaskListVO activeListVO() {
        HerbCollectionTaskListVO vo = new HerbCollectionTaskListVO();
        vo.setId(1L);
        vo.setTaskCode("TASK_20260710_001");
        vo.setTaskName("Huanglian collection task");
        vo.setTaskStatus(HerbCollectionTaskStatusConstants.PUBLISHED);
        return vo;
    }
}
