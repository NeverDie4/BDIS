package com.bdis.modules.assistant.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.assistant.agent.dto.AgentTaskCreateRequest;
import com.bdis.modules.assistant.agent.service.AgentActionExecutionDispatcher;
import com.bdis.modules.assistant.agent.service.AgentCollectionPlanService;
import com.bdis.modules.assistant.agent.service.AgentDigitalArchiveService;
import com.bdis.modules.assistant.agent.service.AgentReanalysisQueryService;
import com.bdis.modules.assistant.agent.service.AgentWaitStatusQueryService;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentTaskService;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentWorkflowRunner;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinEvidenceAnalysisRunner;
import com.bdis.modules.assistant.agent.vo.AgentAnalysisRoundVO;
import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import com.bdis.modules.assistant.agent.vo.AgentDiagnosisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveStatusVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentReanalysisComparisonVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.assistant.agent.vo.AgentWaitStatusVO;
import com.bdis.modules.assistant.agent.vo.AgentWorkflowStartVO;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbDigitalTwinAgentControllerTest {

  @Mock private HerbDigitalTwinAgentTaskService agentTaskService;

  @Mock private HerbDigitalTwinAgentWorkflowRunner workflowRunner;

  @Mock private HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner;

  @Mock private AgentCollectionPlanService collectionPlanService;

  @Mock private AgentActionExecutionDispatcher actionExecutionDispatcher;

  @Mock private AgentWaitStatusQueryService waitStatusQueryService;

  @Mock private AgentReanalysisQueryService reanalysisQueryService;

  @Mock private AgentDigitalArchiveService digitalArchiveService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new HerbDigitalTwinAgentController(
                    agentTaskService,
                    workflowRunner,
                    evidenceAnalysisRunner,
                    collectionPlanService,
                    actionExecutionDispatcher,
                    waitStatusQueryService,
                    reanalysisQueryService,
                    digitalArchiveService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void archiveStatusReturnsPreparationState() throws Exception {
    when(digitalArchiveService.status(1L))
        .thenReturn(
            new AgentDigitalArchiveStatusVO(
                1L, "READY", "可准备", 90, 85, true, true, false, null, "档案已满足准备条件"));

    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/archive/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("READY"))
        .andExpect(jsonPath("$.data.completenessScore").value(90));
  }

  @Test
  void createReturnsUnifiedAgentTaskSummary() throws Exception {
    AgentTaskSummaryVO summary = new AgentTaskSummaryVO();
    summary.setId(1L);
    summary.setTaskNo("AGENT-DT-20260716-000001");
    summary.setStatus("CREATED");
    summary.setStatusLabel("已创建");
    when(agentTaskService.create(any())).thenReturn(summary);

    mockMvc
        .perform(
            post("/herb/assistant/agent/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "goalType":"DIGITAL_TWIN_RESEARCH",
                      "goalText":"持续观察黄连任务并生成可信数字生命档案。",
                      "targetType":"COLLECTION_TASK",
                      "targetId":12,
                      "collectionTaskId":12,
                      "pageContext":"digital-life"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.taskNo").value("AGENT-DT-20260716-000001"))
        .andExpect(jsonPath("$.data.status").value("CREATED"))
        .andExpect(jsonPath("$.data.statusLabel").value("已创建"));
  }

  @Test
  void createRequestHasNoBindableUserIdField() {
    assertThat(Arrays.stream(AgentTaskCreateRequest.class.getDeclaredFields()).map(Field::getName))
        .doesNotContain("userId");
  }

  @Test
  void createRejectsBlankGoalTextAndMissingCollectionTask() throws Exception {
    mockMvc
        .perform(
            post("/herb/assistant/agent/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "goalType":"DIGITAL_TWIN_RESEARCH",
                      "goalText":" ",
                      "targetType":"COLLECTION_TASK"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.data.goalText").exists())
        .andExpect(jsonPath("$.data.collectionTaskId").exists());
  }

  @Test
  void listAndChildResourceRoutesUseUnifiedResponses() throws Exception {
    when(agentTaskService.page(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0));
    when(agentTaskService.listSteps(1L)).thenReturn(List.of());
    when(agentTaskService.listFindings(1L)).thenReturn(List.of());
    when(agentTaskService.listPendingActions(1L)).thenReturn(List.of());

    mockMvc
        .perform(get("/herb/assistant/agent/tasks").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/steps"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/findings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/actions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void detailAndCancelRoutesReturnTaskDetail() throws Exception {
    AgentTaskDetailVO detail = new AgentTaskDetailVO();
    detail.setId(1L);
    detail.setStatus("CANCELLED");
    detail.setStatusLabel("已取消");
    when(agentTaskService.getDetail(1L)).thenReturn(detail);
    when(agentTaskService.cancel(eq(1L), any())).thenReturn(detail);

    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1));
    mockMvc
        .perform(
            post("/herb/assistant/agent/tasks/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"用户取消本次分析\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  @Test
  void startAndDiagnosisRoutesReturnExplicitWorkflowState() throws Exception {
    when(workflowRunner.start(1L)).thenReturn(new AgentWorkflowStartVO(1L, "RUNNING", "started"));
    when(workflowRunner.getDiagnosis(1L))
        .thenReturn(
            new AgentDiagnosisResponseVO(false, "NOT_STARTED", "diagnosis is not available", null));

    mockMvc
        .perform(post("/herb/assistant/agent/tasks/1/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.agentTaskId").value(1))
        .andExpect(jsonPath("$.data.status").value("RUNNING"));
    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/diagnosis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.available").value(false))
        .andExpect(jsonPath("$.data.status").value("NOT_STARTED"))
        .andExpect(jsonPath("$.data.report").doesNotExist());
  }

  @Test
  void evidenceAnalysisRouteDoesNotFabricateUnavailableReport() throws Exception {
    when(evidenceAnalysisRunner.getAnalysis(1L))
        .thenReturn(
            new AgentEvidenceAnalysisResponseVO(false, "NOT_STARTED", "not available", null));

    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/evidence-analysis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.available").value(false))
        .andExpect(jsonPath("$.data.status").value("NOT_STARTED"))
        .andExpect(jsonPath("$.data.analysis").doesNotExist());
  }

  @Test
  void collectionPlanGenerateAndQueryRoutesReturnPersistedPlan() throws Exception {
    AgentCollectionPlanVO plan =
        new AgentCollectionPlanVO(
            10L,
            "PLAN-DT-20260716-1-01",
            1L,
            1,
            12L,
            "PROPOSED",
            "待确认",
            "RULE_TEMPLATE",
            null,
            null,
            null);
    when(collectionPlanService.generate(1L, false)).thenReturn(plan);
    when(collectionPlanService.get(1L)).thenReturn(plan);

    mockMvc
        .perform(post("/herb/assistant/agent/tasks/1/collection-plan/generate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(10))
        .andExpect(jsonPath("$.data.status").value("PROPOSED"));
    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/collection-plan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.planNo").value("PLAN-DT-20260716-1-01"));
  }

  @Test
  void waitStatusRouteReturnsOnlyDisplaySummary() throws Exception {
    when(waitStatusQueryService.get(1L))
        .thenReturn(
            new AgentWaitStatusVO(
                200L,
                "黄连重点复测",
                "PARTIALLY_SATISFIED",
                null,
                List.of("已创建复测批次"),
                List.of("缺少土壤 pH"),
                50,
                null,
                "继续补充现场数据"));

    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/wait-status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.followUpTaskId").value(200))
        .andExpect(jsonPath("$.data.completedRequirements[0]").value("已创建复测批次"))
        .andExpect(jsonPath("$.data.missingRequirements[0]").value("缺少土壤 pH"))
        .andExpect(jsonPath("$.data.conditionJson").doesNotExist());
  }

  @Test
  void reanalysisAndRoundHistoryRoutesReturnPersistedSummaries() throws Exception {
    AgentReanalysisComparisonVO comparison =
        new AgentReanalysisComparisonVO(
            10L,
            1,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            java.util.Map.of(),
            java.util.Map.of(),
            72,
            94,
            "NOT_READY",
            "READY",
            "补采证据已完善",
            "READY_FOR_ARCHIVE");
    when(reanalysisQueryService.latest(1L)).thenReturn(comparison);
    when(reanalysisQueryService.rounds(1L))
        .thenReturn(
            List.of(
                new AgentAnalysisRoundVO(
                    10L,
                    1,
                    "FOLLOW_UP",
                    12L,
                    200L,
                    "a",
                    "b",
                    "补采证据已完善",
                    "READY_FOR_ARCHIVE",
                    "SUCCEEDED",
                    null,
                    null)));

    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/reanalysis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.outcome").value("READY_FOR_ARCHIVE"));
    mockMvc
        .perform(get("/herb/assistant/agent/tasks/1/analysis-rounds"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].roundNo").value(1));
  }
}
