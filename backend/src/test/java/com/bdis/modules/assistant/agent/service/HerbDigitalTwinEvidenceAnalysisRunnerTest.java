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
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.AnalysisOutcome;
import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.EvidenceFindingDraft;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutor;
import com.bdis.modules.assistant.agent.tool.AgentToolResult;
import com.bdis.modules.assistant.agent.tool.DigitalLifeArchiveReadTool;
import com.bdis.modules.assistant.agent.tool.GrowthRecordReadTool;
import com.bdis.modules.assistant.agent.tool.RecognitionReadTool;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceExplanation;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HerbDigitalTwinEvidenceAnalysisRunnerTest {

    @Mock private AgentTaskMapper taskMapper;
    @Mock private AgentStepMapper stepMapper;
    @Mock private AgentFindingMapper findingMapper;
    @Mock private HerbDigitalTwinAgentTaskService taskService;
    @Mock private AgentTaskStateMachine stateMachine;
    @Mock private AgentToolExecutor toolExecutor;
    @Mock private GrowthRecordReadTool growthRecordTool;
    @Mock private RecognitionReadTool recognitionTool;
    @Mock private DigitalLifeArchiveReadTool archiveTool;
    @Mock private AgentEvidenceAnalysisService analysisService;
    @Mock private AgentEvidenceExplanationService explanationService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private HerbDigitalTwinEvidenceAnalysisRunner runner;
    private AgentStepEntity timelineStep;
    private AgentStepEntity evidenceStep;

    @BeforeEach
    void setUp() {
        authenticate();
        AgentTaskEntity task = new AgentTaskEntity();
        task.setId(1L);
        task.setStatus("RUNNING");
        task.setCollectionTaskId(12L);
        task.setSessionId("session-1");
        task.setVersion(2);
        timelineStep = step(5L, 5, "ANALYZE_TIMELINE");
        evidenceStep = step(6L, 6, "ANALYZE_EVIDENCE");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskService.getDetail(1L)).thenReturn(new AgentTaskDetailVO());
        when(stepMapper.selectByTaskIdAndNo(1L, 5)).thenAnswer(ignored -> timelineStep);
        when(stepMapper.selectByTaskIdAndNo(1L, 6)).thenAnswer(ignored -> evidenceStep);
        when(stepMapper.insertIfAbsent(any())).thenReturn(1);
        when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
        doAnswer(
                        invocation -> {
                            Long id = invocation.getArgument(0);
                            AgentStepEntity step = id.equals(5L) ? timelineStep : evidenceStep;
                            step.setStatus("SUCCEEDED");
                            step.setOutputJson(invocation.getArgument(2));
                            return 1;
                        })
                .when(stepMapper)
                .markSucceeded(anyLong(), anyString(), anyString(), any());
        when(taskService.updateProgress(anyLong(), anyInt(), any()))
                .thenReturn(new AgentTaskSummaryVO());
        stubTools();
        stubExecutor();
        AgentEvidenceAnalysisVO analysis = analysis();
        when(analysisService.analyze(any()))
                .thenReturn(
                        new AnalysisOutcome(
                                analysis,
                                List.of(
                                        new EvidenceFindingDraft(
                                                "MISSING_ROOT_IMAGE",
                                                "LOW",
                                                "BATCH",
                                                11L,
                                                "缺少根部图片",
                                                "缺少根部图片",
                                                Map.of("batchId", 11L),
                                                "补充根部图片",
                                                false))));
        when(explanationService.explain(analysis)).thenReturn(analysis.explanation());
        runner =
                new HerbDigitalTwinEvidenceAnalysisRunner(
                        taskMapper,
                        stepMapper,
                        findingMapper,
                        taskService,
                        stateMachine,
                        toolExecutor,
                        growthRecordTool,
                        recognitionTool,
                        archiveTool,
                        analysisService,
                        explanationService,
                        objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completesAuditedEvidenceWorkflowAtFiftyFivePercent() {
        AgentEvidenceAnalysisVO result = runner.run(1L);

        assertThat(result.stageCount()).isZero();
        verify(toolExecutor, times(4)).execute(anyString(), any(), anyString(), any());
        verify(findingMapper).upsert(any());
        verify(taskService).updateProgress(1L, 55, "EVIDENCE_ANALYSIS_COMPLETED");
    }

    @Test
    void repeatedRunReturnsStoredResultWithoutCallingToolsAgain() {
        runner.run(1L);

        AgentEvidenceAnalysisVO repeated = runner.run(1L);

        assertThat(repeated.agentTaskId()).isEqualTo(1L);
        verify(toolExecutor, times(4)).execute(anyString(), any(), anyString(), any());
        verify(findingMapper, times(1)).upsert(any());
    }

    @Test
    void userWithoutTaskAccessCannotRunOrReadAnalysis() {
        doThrow(new ForbiddenException("无权访问")).when(taskService).getDetail(1L);

        assertThatThrownBy(() -> runner.run(1L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> runner.getAnalysis(1L)).isInstanceOf(ForbiddenException.class);
        verify(toolExecutor, never()).execute(anyString(), any(), anyString(), any());
    }

    private void stubExecutor() {
        when(toolExecutor.execute(anyString(), any(), anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            AgentToolExecutor.AgentToolInvocation<?> call =
                                    invocation.getArgument(3);
                            return new AgentToolResult<>(
                                    invocation.getArgument(0),
                                    true,
                                    "读取成功",
                                    call.execute(),
                                    List.of(),
                                    LocalDateTime.now(),
                                    1L);
                        });
    }

    private void stubTools() {
        when(growthRecordTool.listTaskGrowthRecords(anyLong(), any()))
                .thenReturn(new GrowthRecordToolData.RecordList(12L, List.of()));
        when(archiveTool.getDigitalLifeStages(anyLong(), any()))
                .thenReturn(new DigitalLifeArchiveToolData.StageList(12L, List.of()));
        when(recognitionTool.listUnrecognizedImages(anyLong(), any()))
                .thenReturn(new RecognitionToolData.TaskImageList(12L, List.of()));
        when(recognitionTool.listLowConfidenceImages(anyLong(), any()))
                .thenReturn(new RecognitionToolData.TaskImageList(12L, List.of()));
    }

    private AgentEvidenceAnalysisVO analysis() {
        AgentEvidenceExplanation explanation =
                new AgentEvidenceExplanation(
                        "规则分析完成。",
                        List.of(),
                        List.of(),
                        List.of("不能形成确定因果结论。"),
                        List.of(),
                        List.of(),
                        "LOW");
        return new AgentEvidenceAnalysisVO(
                1L,
                12L,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                explanation,
                LocalDateTime.of(2026, 7, 16, 10, 0));
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

    private void authenticate() {
        CurrentUser user =
                new CurrentUser(7L, "teacher", "教师", 1L, 1L, Set.of("TEACHER"), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
