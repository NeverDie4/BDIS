package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.AgentCompletenessDiagnosisService.DiagnosisOutcome;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutor;
import com.bdis.modules.assistant.agent.tool.AgentToolResult;
import com.bdis.modules.assistant.agent.tool.CollectionTaskReadTool;
import com.bdis.modules.assistant.agent.tool.DigitalLifeArchiveReadTool;
import com.bdis.modules.assistant.agent.tool.GrowthRecordReadTool;
import com.bdis.modules.assistant.agent.tool.RecognitionReadTool;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.assistant.agent.vo.AgentWorkflowStartVO;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HerbDigitalTwinAgentWorkflowRunnerTest {

    @Mock private AgentTaskMapper taskMapper;
    @Mock private AgentStepMapper stepMapper;
    @Mock private AgentFindingMapper findingMapper;
    @Mock private HerbDigitalTwinAgentTaskService taskService;
    @Mock private AgentTaskStateMachine stateMachine;
    @Mock private AgentToolExecutor toolExecutor;
    @Mock private CollectionTaskReadTool collectionTaskTool;
    @Mock private GrowthRecordReadTool growthRecordTool;
    @Mock private RecognitionReadTool recognitionTool;
    @Mock private DigitalLifeArchiveReadTool archiveTool;
    @Mock private AgentCompletenessDiagnosisService diagnosisService;
    @Mock private HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner;
    @Mock private AgentCollectionPlanService collectionPlanService;

    private HerbDigitalTwinAgentWorkflowRunner runner;
    private AgentTaskEntity task;

    @BeforeEach
    void setUp() {
        authenticate();
        task = task("CREATED");
        when(taskMapper.selectById(1L)).thenAnswer(ignored -> task);
        when(taskService.getDetail(1L)).thenReturn(new AgentTaskDetailVO());
        when(taskService.transition(1L, AgentTaskStatus.PLANNING))
                .thenAnswer(
                        ignored -> {
                            task.setStatus("PLANNING");
                            task.setVersion(task.getVersion() + 1);
                            return new AgentTaskSummaryVO();
                        });
        doAnswer(
                        invocation -> {
                            AgentTaskStatus target = invocation.getArgument(1);
                            task.setStatus(target.getCode());
                            task.setVersion(task.getVersion() + 1);
                            return task;
                        })
                .when(stateMachine)
                .transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class));
        when(stepMapper.insertIfAbsent(any())).thenReturn(1);
        when(stepMapper.selectByTaskId(1L)).thenReturn(steps());
        when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
        when(stepMapper.markSucceeded(anyLong(), anyString(), anyString(), any())).thenReturn(1);
        when(taskService.updateProgress(anyLong(), anyInt(), any()))
                .thenReturn(new AgentTaskSummaryVO());
        when(taskService.listFindings(1L)).thenReturn(List.of());
        stubTools();
        stubExecutor();
        when(diagnosisService.diagnose(any())).thenReturn(outcome());
        when(collectionPlanService.generate(1L, false))
                .thenAnswer(
                        ignored -> {
                            task.setStatus("WAITING_CONFIRMATION");
                            task.setCurrentPhase("WAITING_COLLECTION_PLAN_CONFIRMATION");
                            return null;
                        });
        runner =
                new HerbDigitalTwinAgentWorkflowRunner(
                        taskMapper,
                        stepMapper,
                        findingMapper,
                        taskService,
                        stateMachine,
                        toolExecutor,
                        collectionTaskTool,
                        growthRecordTool,
                        recognitionTool,
                        archiveTool,
                        diagnosisService,
                        evidenceAnalysisRunner,
                        collectionPlanService,
                        new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startsFourStepWorkflowAndCallsAllAuditedToolsOnce() {
        AgentWorkflowStartVO result = runner.start(1L);

        assertThat(result.status()).isEqualTo("WAITING_CONFIRMATION");
        assertThat(task.getStatus()).isEqualTo("WAITING_CONFIRMATION");
        verify(stepMapper, times(4)).insertIfAbsent(any());
        verify(stepMapper, times(4)).markRunning(anyLong(), any());
        verify(stepMapper, times(4)).markSucceeded(anyLong(), anyString(), anyString(), any());
        verify(toolExecutor, times(8)).execute(anyString(), any(), anyString(), any());
        verify(taskService).updateProgress(1L, 35, "COMPLETENESS_DIAGNOSIS_COMPLETED");
        verify(evidenceAnalysisRunner).run(1L);
        verify(collectionPlanService).generate(1L, false);
    }

    @Test
    void repeatedStartDoesNotExecuteToolsAgain() {
        runner.start(1L);

        AgentWorkflowStartVO repeated = runner.start(1L);

        assertThat(repeated.message()).contains("未重复执行");
        verify(toolExecutor, times(8)).execute(anyString(), any(), anyString(), any());
    }

    @Test
    void optimisticConflictFromConcurrentStarterReturnsCurrentState() {
        task.setStatus("CREATED");
        doAnswer(
                        ignored -> {
                            task.setStatus("RUNNING");
                            throw new ResourceConflictException("并发推进");
                        })
                .when(taskService)
                .transition(1L, AgentTaskStatus.PLANNING);

        AgentWorkflowStartVO result = runner.start(1L);

        assertThat(result.status()).isEqualTo("RUNNING");
        verify(toolExecutor, never()).execute(anyString(), any(), anyString(), any());
    }

    @Test
    void unauthorizedUserCannotStartWorkflow() {
        doThrow(new ForbiddenException("无权访问该 Agent 任务")).when(taskService).getDetail(1L);

        assertThatThrownBy(() -> runner.start(1L)).isInstanceOf(ForbiddenException.class);
        verify(taskService, never()).transition(anyLong(), any());
        verify(toolExecutor, never()).execute(anyString(), any(), anyString(), any());
    }

    private void stubExecutor() {
        when(toolExecutor.execute(anyString(), any(), anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            AgentToolExecutor.AgentToolInvocation<?> call =
                                    invocation.getArgument(3);
                            Object data = call.execute();
                            return new AgentToolResult<>(
                                    invocation.getArgument(0),
                                    true,
                                    "读取成功",
                                    data,
                                    List.of(),
                                    LocalDateTime.now(),
                                    1L);
                        });
    }

    private void stubTools() {
        CollectionTaskToolData.Overview overview =
                new CollectionTaskToolData.Overview(
                        12L,
                        "TASK-12",
                        "黄连任务",
                        1L,
                        "黄连",
                        2L,
                        "基地",
                        "地点",
                        "采集员",
                        status("in_progress"),
                        null,
                        null,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        status("not_ready"));
        when(collectionTaskTool.getCollectionTaskOverview(anyLong(), any())).thenReturn(overview);
        when(collectionTaskTool.listCollectionTaskBatches(anyLong(), any()))
                .thenReturn(new CollectionTaskToolData.BatchList(12L, List.of()));
        when(growthRecordTool.listTaskGrowthRecords(anyLong(), any()))
                .thenReturn(new GrowthRecordToolData.RecordList(12L, List.of()));
        when(recognitionTool.listUnrecognizedImages(anyLong(), any()))
                .thenReturn(new RecognitionToolData.TaskImageList(12L, List.of()));
        when(recognitionTool.listLowConfidenceImages(anyLong(), any()))
                .thenReturn(new RecognitionToolData.TaskImageList(12L, List.of()));
        when(archiveTool.getDigitalLifeArchiveOverview(anyLong(), any()))
                .thenReturn(
                        new DigitalLifeArchiveToolData.Overview(
                                12L,
                                null,
                                "黄连任务",
                                "黄连",
                                "基地",
                                0,
                                0,
                                0,
                                0,
                                false,
                                false,
                                status("not_ready"),
                                null,
                                null));
        when(archiveTool.getDigitalLifeStages(anyLong(), any()))
                .thenReturn(new DigitalLifeArchiveToolData.StageList(12L, List.of()));
        when(archiveTool.getArchiveIntegrityStatus(anyLong(), any()))
                .thenReturn(
                        new DigitalLifeArchiveToolData.IntegrityStatus(
                                12L, false, 0, null, null, null, null, null, "尚未生成"));
    }

    private DiagnosisOutcome outcome() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("任务基础信息", 10);
        return new DiagnosisOutcome(
                1L,
                12L,
                "黄连任务",
                0,
                0,
                10,
                "NOT_READY",
                scores,
                Map.of("任务基础信息", List.of()),
                List.of(),
                List.of("补充观测阶段"),
                "当前档案未就绪",
                LocalDateTime.now());
    }

    private List<AgentStepEntity> steps() {
        return List.of(
                step(1L, 1, "LOAD_CONTEXT"),
                step(2L, 2, "INSPECT_TASK"),
                step(3L, 3, "ANALYZE_TIMELINE"),
                step(4L, 4, "ANALYZE_EVIDENCE"));
    }

    private AgentStepEntity step(Long id, int number, String type) {
        AgentStepEntity step = new AgentStepEntity();
        step.setId(id);
        step.setAgentTaskId(1L);
        step.setStepNo(number);
        step.setStepType(type);
        step.setStatus("PENDING");
        return step;
    }

    private AgentTaskEntity task(String status) {
        AgentTaskEntity entity = new AgentTaskEntity();
        entity.setId(1L);
        entity.setStatus(status);
        entity.setVersion(0);
        entity.setCollectionTaskId(12L);
        entity.setSessionId("session-1");
        return entity;
    }

    private StatusValue status(String code) {
        return new StatusValue(code, code);
    }

    private void authenticate() {
        CurrentUser user =
                new CurrentUser(7L, "teacher", "教师", 1L, 1L, Set.of("TEACHER"), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
