package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentCollectionPlanUpdateRequest;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredImageItem;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredMetricItem;
import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionPlanMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.FollowUpCollectionPlanGenerator.GeneratedPlan;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutor;
import com.bdis.modules.assistant.agent.tool.AgentToolResult;
import com.bdis.modules.assistant.agent.tool.CollectionTaskReadTool;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisResponseVO;
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
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentCollectionPlanServiceTest {

    @Mock private AgentCollectionPlanMapper planMapper;
    @Mock private AgentTaskMapper taskMapper;
    @Mock private AgentStepMapper stepMapper;
    @Mock private AgentActionMapper actionMapper;
    @Mock private HerbDigitalTwinAgentTaskService taskService;
    @Mock private HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner;
    @Mock private AgentTaskStateMachine stateMachine;
    @Mock private AgentToolExecutor toolExecutor;
    @Mock private CollectionTaskReadTool collectionTaskTool;
    @Mock private FollowUpCollectionPlanGenerator generator;

    private AgentCollectionPlanService service;
    private AgentTaskEntity task;
    private AgentCollectionPlanEntity storedPlan;

    @BeforeEach
    void setUp() {
        authenticate();
        task = new AgentTaskEntity();
        task.setId(1L);
        task.setCollectionTaskId(12L);
        task.setSessionId("session-1");
        task.setStatus("RUNNING");
        task.setCurrentPhase("EVIDENCE_ANALYSIS_COMPLETED");
        task.setVersion(3);
        when(taskService.getDetail(1L)).thenReturn(new AgentTaskDetailVO());
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(planMapper.selectMaxRound(1L)).thenReturn(0);
        when(planMapper.selectLatestActive(1L)).thenAnswer(ignored -> storedPlan);
        when(planMapper.selectById(anyLong())).thenAnswer(ignored -> storedPlan);
        doAnswer(
                        invocation -> {
                            storedPlan = invocation.getArgument(0);
                            storedPlan.setId(100L);
                            return 1;
                        })
                .when(planMapper)
                .insert(any());
        AgentStepEntity generateStep = step(7L, 7, "GENERATE_COLLECTION_PLAN");
        AgentStepEntity waitStep = step(8L, 8, "WAIT_FOR_CONFIRMATION");
        when(stepMapper.selectByTaskIdAndNo(1L, 7)).thenReturn(generateStep);
        when(stepMapper.selectByTaskIdAndNo(1L, 8)).thenReturn(waitStep);
        when(stepMapper.insertIfAbsent(any())).thenReturn(1);
        when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
        when(stepMapper.markSucceeded(anyLong(), anyString(), anyString(), any())).thenReturn(1);
        when(stepMapper.markWaiting(anyLong(), any())).thenReturn(1);
        when(actionMapper.insert(any())).thenReturn(1);
        when(taskService.updateProgress(anyLong(), anyInt(), any()))
                .thenReturn(new AgentTaskSummaryVO());
        when(taskService.listFindings(1L)).thenReturn(List.of());
        when(evidenceAnalysisRunner.getAnalysis(1L))
                .thenReturn(
                        new AgentEvidenceAnalysisResponseVO(true, "AVAILABLE", "ok", analysis()));
        stubTool();
        when(generator.generate(any()))
                .thenReturn(new GeneratedPlan(plan(), "RULE_TEMPLATE", "v1", null));
        doAnswer(
                        invocation -> {
                            task.setStatus(
                                    invocation.getArgument(1, AgentTaskStatus.class).getCode());
                            return task;
                        })
                .when(stateMachine)
                .transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class));
        service =
                new AgentCollectionPlanService(
                        planMapper,
                        taskMapper,
                        stepMapper,
                        actionMapper,
                        taskService,
                        evidenceAnalysisRunner,
                        stateMachine,
                        toolExecutor,
                        collectionTaskTool,
                        generator,
                        new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generationPersistsPlanAndOnePendingActionThenWaitsForConfirmation() {
        AgentCollectionPlanVO result = service.generate(1L, false);

        assertThat(result.status()).isEqualTo("PROPOSED");
        assertThat(result.plan().requiredMetrics())
                .extracting(RequiredMetricItem::metricCode)
                .containsExactly("soilPh");
        verify(planMapper).insert(any());
        verify(actionMapper).insert(any());
        verify(stateMachine).transition(task, AgentTaskStatus.WAITING_CONFIRMATION);
        verify(taskService).updateProgress(1L, 65, "WAITING_COLLECTION_PLAN_CONFIRMATION");
    }

    @Test
    void duplicateGenerationReturnsActivePlanWithoutCreatingAnotherAction() {
        service.generate(1L, false);

        AgentCollectionPlanVO repeated = service.generate(1L, false);

        assertThat(repeated.id()).isEqualTo(100L);
        verify(planMapper, times(1)).insert(any());
        verify(actionMapper, times(1)).insert(any());
    }

    @Test
    void updateRejectsPlanBelongingToAnotherAgentTask() {
        storedPlan = persistedPlan();
        storedPlan.setAgentTaskId(2L);

        assertThatThrownBy(() -> service.update(1L, 100L, updateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于");
        verify(planMapper, never()).updateEditable(any(), anyInt());
    }

    @Test
    void userCanAdjustEditablePlanWithoutChangingBusinessOwnership() {
        storedPlan = persistedPlan();
        when(planMapper.updateEditable(any(), anyInt()))
                .thenAnswer(
                        invocation -> {
                            AgentCollectionPlanEntity update = invocation.getArgument(0);
                            storedPlan.setObjective(update.getObjective());
                            storedPlan.setRequiredMetricsJson(update.getRequiredMetricsJson());
                            storedPlan.setRequiredImagesJson(update.getRequiredImagesJson());
                            storedPlan.setCompletionCriteriaJson(
                                    update.getCompletionCriteriaJson());
                            storedPlan.setRationale(update.getRationale());
                            storedPlan.setPlanSource("MANUAL_ADJUSTED");
                            storedPlan.setUpdateTime(update.getUpdateTime());
                            storedPlan.setVersion(1);
                            return 1;
                        });

        AgentCollectionPlanVO result = service.update(1L, 100L, updateRequest());

        assertThat(result.planSource()).isEqualTo("MANUAL_ADJUSTED");
        assertThat(result.collectionTaskId()).isEqualTo(12L);
        assertThat(result.plan().sourceFindingIds()).containsExactly(1L);
    }

    private void stubTool() {
        CollectionTaskToolData.Overview overview =
                new CollectionTaskToolData.Overview(
                        12L,
                        "TASK-12",
                        "黄连任务",
                        2L,
                        "黄连",
                        3L,
                        "基地",
                        "地点",
                        null,
                        new StatusValue("in_progress", "进行中"),
                        null,
                        LocalDateTime.now().plusDays(30),
                        3,
                        3,
                        4,
                        3,
                        0,
                        3,
                        0,
                        new StatusValue("not_ready", "未就绪"));
        when(collectionTaskTool.getCollectionTaskOverview(anyLong(), any())).thenReturn(overview);
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

    private FollowUpCollectionPlan plan() {
        return new FollowUpCollectionPlan(
                "复测土壤 pH",
                "DAYS_AFTER",
                3,
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(7),
                List.of(new RequiredMetricItem("soilPh", "土壤 pH", true, "缺失", "测量", null)),
                List.of(new RequiredImageItem("root", "根部", 1, "拍摄根部", "缺图")),
                List.of("记录天气"),
                List.of("完成必填项"),
                List.of(1L),
                "规则依据",
                "不能证明因果",
                "MEDIUM");
    }

    private AgentEvidenceAnalysisVO analysis() {
        return new AgentEvidenceAnalysisVO(
                1L,
                12L,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new AgentEvidenceExplanation(
                        "完成", List.of(), List.of(), List.of(), List.of(), List.of(), "LOW"),
                LocalDateTime.now());
    }

    private AgentCollectionPlanEntity persistedPlan() {
        AgentCollectionPlanEntity entity = new AgentCollectionPlanEntity();
        entity.setId(100L);
        entity.setPlanNo("PLAN-DT-20260716-1-01");
        entity.setAgentTaskId(1L);
        entity.setResearchRound(1);
        entity.setCollectionTaskId(12L);
        entity.setObjective(plan().objective());
        entity.setRecommendedTimeType("DAYS_AFTER");
        entity.setRecommendedAfterDays(3);
        entity.setRecommendedStartTime(plan().recommendedStartTime());
        entity.setRecommendedEndTime(plan().recommendedEndTime());
        try {
            ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
            entity.setRequiredMetricsJson(mapper.writeValueAsString(plan().requiredMetrics()));
            entity.setRequiredImagesJson(mapper.writeValueAsString(plan().requiredImages()));
            entity.setOptionalItemsJson(mapper.writeValueAsString(plan().optionalItems()));
            entity.setCompletionCriteriaJson(
                    mapper.writeValueAsString(plan().completionCriteria()));
            entity.setSourceFindingsJson(mapper.writeValueAsString(plan().sourceFindingIds()));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        entity.setRationale(plan().rationale());
        entity.setUncertainty(plan().uncertainty());
        entity.setPriority("MEDIUM");
        entity.setPlanSource("RULE_TEMPLATE");
        entity.setStatus("PROPOSED");
        entity.setVersion(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    private AgentCollectionPlanUpdateRequest updateRequest() {
        return new AgentCollectionPlanUpdateRequest(
                "人工调整后的复测方案",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(6),
                plan().requiredMetrics(),
                plan().requiredImages(),
                plan().completionCriteria(),
                "用户根据现场安排调整");
    }

    private AgentStepEntity step(Long id, int stepNo, String type) {
        AgentStepEntity step = new AgentStepEntity();
        step.setId(id);
        step.setAgentTaskId(1L);
        step.setStepNo(stepNo);
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
