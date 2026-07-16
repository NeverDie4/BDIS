package com.bdis.modules.assistant.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeImageVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeRecognitionVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class CollectionTaskReadToolTest {

    private HerbCollectionTaskService taskService;
    private HerbDigitalLifeArchiveService archiveService;
    private AgentReadToolSupport support;
    private CollectionTaskReadTool tool;
    private AgentToolExecutionContext context;

    @BeforeEach
    void setUp() {
        taskService = mock(HerbCollectionTaskService.class);
        archiveService = mock(HerbDigitalLifeArchiveService.class);
        support = mock(AgentReadToolSupport.class);
        when(support.status(any()))
                .thenAnswer(
                        invocation -> {
                            String code = invocation.getArgument(0);
                            return new StatusValue(code, code == null ? null : "中文-" + code);
                        });
        tool = new CollectionTaskReadTool(taskService, archiveService, support);
        context = mock(AgentToolExecutionContext.class);
    }

    @Test
    void returnsTaskAggregateFromSingleArchiveLoad() {
        when(taskService.getById(12L)).thenReturn(task());
        when(archiveService.getByTaskId(12L)).thenReturn(archive());

        CollectionTaskToolData.Overview result = tool.getCollectionTaskOverview(12L, context);

        assertThat(result.taskName()).isEqualTo("黄连连续观测");
        assertThat(result.batchCount()).isEqualTo(2);
        assertThat(result.growthRecordCount()).isEqualTo(1);
        assertThat(result.imageCount()).isEqualTo(1);
        assertThat(result.identifiedImageCount()).isEqualTo(1);
        assertThat(result.pendingReviewCount()).isEqualTo(1);
        assertThat(result.approvedCount()).isEqualTo(1);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.taskStatus().label()).isEqualTo("中文-in_progress");
        verify(taskService).getById(12L);
        verify(archiveService).getByTaskId(12L);
    }

    @Test
    void handlesMissingImagesAndRecognitionWithoutNullPointer() {
        HerbDigitalLifeArchiveVO archive = archive();
        archive.getStages().get(0).setImages(null);
        archive.getStages().get(0).setRecognition(null);
        when(archiveService.getByTaskId(12L)).thenReturn(archive);

        CollectionTaskToolData.Progress result = tool.getCollectionTaskProgress(12L, context);

        assertThat(result.imageCount()).isZero();
        assertThat(result.identifiedImageCount()).isZero();
        assertThat(result.pendingReviewCount()).isZero();
    }

    @Test
    void permissionDenialStopsBeforeBusinessServiceCalls() {
        doThrow(new ForbiddenException("采集任务超出当前数据范围"))
                .when(support)
                .requireTaskContext(context, 12L);

        assertThatThrownBy(() -> tool.getCollectionTaskOverview(12L, context))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(taskService, archiveService);
    }

    private HerbCollectionTaskVO task() {
        HerbCollectionTaskVO task = new HerbCollectionTaskVO();
        task.setId(12L);
        task.setTaskCode("TASK-12");
        task.setTaskName("黄连连续观测");
        task.setSpeciesName("黄连");
        task.setBaseName("标本园");
        task.setCollectorName("采集员甲");
        task.setTaskStatus("in_progress");
        task.setBatchCount(2L);
        return task;
    }

    private HerbDigitalLifeArchiveVO archive() {
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        archive.setTaskId(12L);
        archive.setStageCount(2);
        archive.setValidStageCount(1);
        archive.setImageCount(1);
        archive.setArchiveStatus("not_ready");

        HerbDigitalLifeStageVO first = new HerbDigitalLifeStageVO();
        first.setBatchId(101L);
        first.setGrowthRecordId(201L);
        first.setAuditStatus("approved");
        HerbDigitalLifeImageVO image = new HerbDigitalLifeImageVO();
        image.setImageId(301L);
        first.setImages(List.of(image));
        HerbDigitalLifeRecognitionVO recognition = new HerbDigitalLifeRecognitionVO();
        recognition.setSpeciesName("黄连");
        recognition.setConfidence(new BigDecimal("0.62"));
        recognition.setNeedReview(true);
        first.setRecognition(recognition);

        HerbDigitalLifeStageVO second = new HerbDigitalLifeStageVO();
        second.setBatchId(102L);
        second.setAuditStatus("rejected");
        second.setImages(List.of());
        archive.setStages(new java.util.ArrayList<>(List.of(first, second)));
        return archive;
    }
}
