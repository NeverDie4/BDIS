package com.bdis.modules.assistant.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.mapper.HerbAssistantImageContextMapper;
import com.bdis.modules.assistant.tool.dto.HerbAssistantBatchToolResult;
import com.bdis.modules.assistant.tool.dto.HerbAssistantImageToolResult;
import com.bdis.modules.assistant.tool.dto.HerbAssistantTaskToolResult;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainContextVO;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
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
class HerbAssistantToolsTest {

    @Mock private HerbBatchMapper batchMapper;
    @Mock private HerbAssistantImageContextMapper imageContextMapper;
    @Mock private HerbCollectionTaskMapper taskMapper;

    private HerbAssistantTools tools;

    @BeforeEach
    void setUp() {
        tools = new HerbAssistantTools(batchMapper, imageContextMapper, taskMapper);
        CurrentUser currentUser =
                new CurrentUser(1L, "user-1", "User One", null, null, Set.of(), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getBatchSummaryByIdReturnsBusinessSummary() {
        HerbBatchEntity batch = batchEntity();
        when(batchMapper.selectById(1L)).thenReturn(batch);

        HerbAssistantBatchToolResult result = tools.getBatchSummaryById(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBatchCode()).isEqualTo("BATCH_001");
        assertThat(result.getFinalSpeciesName()).isEqualTo("黄连");
        assertThat(result.getNeedReviewCount()).isEqualTo(2);
    }

    @Test
    void getBatchSummaryByCodeReturnsFriendlyErrorWhenMissing() {
        when(batchMapper.selectByBatchCode("UNKNOWN")).thenReturn(null);

        HerbAssistantBatchToolResult result = tools.getBatchSummaryByCode("UNKNOWN");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("未找到批次");
    }

    @Test
    void getImageIdentificationByIdReturnsFinalResultOnly() {
        HerbAssistantImageExplainContextVO context = new HerbAssistantImageExplainContextVO();
        context.setImageId(12L);
        context.setImageCode("IMG-012");
        context.setFinalSpeciesName("黄连");
        context.setFinalConfidence(new BigDecimal("0.9123"));
        context.setNeedReview(1);
        when(imageContextMapper.selectImageContextById(12L)).thenReturn(context);

        HerbAssistantImageToolResult result = tools.getImageIdentificationById(12L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalSpeciesName()).isEqualTo("黄连");
        assertThat(result.getFinalConfidence()).isEqualByComparingTo("0.9123");
    }

    @Test
    void listReviewingBatchesUsesReviewingStatus() {
        HerbBatchListVO batch = new HerbBatchListVO();
        batch.setId(1L);
        batch.setBatchCode("BATCH_001");
        batch.setBatchStatus("reviewing");
        when(batchMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(batch));

        List<HerbAssistantBatchToolResult> results = tools.listReviewingBatches();

        ArgumentCaptor<HerbBatchQueryRequest> queryCaptor =
                ArgumentCaptor.forClass(HerbBatchQueryRequest.class);
        org.mockito.Mockito.verify(batchMapper).selectList(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getBatchStatus()).isEqualTo("reviewing");
        assertThat(results).hasSize(1);
    }

    @Test
    void getTaskSummaryByIdReturnsTaskSummary() {
        HerbCollectionTaskVO task = taskVO();
        when(taskMapper.selectDetailById(1L)).thenReturn(task);

        HerbAssistantTaskToolResult result = tools.getTaskSummaryById(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTaskStatus()).isEqualTo("collecting");
        assertThat(result.getBatchCount()).isEqualTo(3L);
    }

    @Test
    void listMyTasksUsesAuthenticatedUser() {
        when(taskMapper.selectMyTasks(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(0L),
                        org.mockito.ArgumentMatchers.eq(20)))
                .thenReturn(List.of(taskVO()));

        List<HerbAssistantTaskToolResult> results = tools.listMyTasks();

        ArgumentCaptor<HerbCollectionTaskMyQueryRequest> queryCaptor =
                ArgumentCaptor.forClass(HerbCollectionTaskMyQueryRequest.class);
        org.mockito.Mockito.verify(taskMapper)
                .selectMyTasks(queryCaptor.capture(), org.mockito.ArgumentMatchers.eq(0L),
                        org.mockito.ArgumentMatchers.eq(20));
        assertThat(queryCaptor.getValue().getCollectorId()).isEqualTo(1L);
        assertThat(results).hasSize(1);
    }

    private HerbBatchEntity batchEntity() {
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(1L);
        batch.setBatchCode("BATCH_001");
        batch.setBatchName("黄连采集批次");
        batch.setBatchStatus("reviewing");
        batch.setSpeciesName("黄连");
        batch.setImageCount(4);
        batch.setIdentifiedCount(4);
        batch.setReviewedCount(2);
        batch.setNeedReviewCount(2);
        batch.setFinalSpeciesName("黄连");
        batch.setAvgSimilarity(new BigDecimal("0.9123"));
        return batch;
    }

    private HerbCollectionTaskVO taskVO() {
        HerbCollectionTaskVO task = new HerbCollectionTaskVO();
        task.setId(1L);
        task.setTaskCode("TASK_001");
        task.setTaskName("黄连采集任务");
        task.setTaskStatus("collecting");
        task.setSpeciesName("黄连");
        task.setCollectorName("User One");
        task.setBatchCount(3L);
        return task;
    }
}
