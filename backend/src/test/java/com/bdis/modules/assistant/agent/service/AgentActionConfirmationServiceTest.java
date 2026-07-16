package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentActionConfirmRequest;
import com.bdis.modules.assistant.agent.dto.AgentActionRejectRequest;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredImageItem;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredMetricItem;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentBusinessLinkEntity;
import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentBusinessLinkMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionPlanMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionRequirementMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentActionExecutionVO;
import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.collection.dto.HerbCollectionTaskCreateRequest;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.ArgumentCaptor;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentActionConfirmationServiceTest {

  @Mock private AgentActionMapper actionMapper;
  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentCollectionPlanMapper planMapper;
  @Mock private AgentBusinessLinkMapper businessLinkMapper;
  @Mock private AgentCollectionRequirementMapper requirementMapper;
  @Mock private AgentStepMapper stepMapper;

  @Mock private AgentWaitConditionMapper waitConditionMapper;
  @Mock private AgentFieldDataConditionEvaluator fieldDataConditionEvaluator;
  @Mock private HerbDigitalTwinAgentTaskService agentTaskService;
  @Mock private AgentCollectionPlanService collectionPlanService;
  @Mock private HerbCollectionTaskService collectionTaskService;
  @Mock private AgentTaskStateMachine stateMachine;
  @Mock private PlatformTransactionManager transactionManager;

  private AgentActionConfirmationService service;
  private AgentActionEntity action;
  private AgentCollectionPlanEntity planEntity;
  private HerbCollectionTaskVO createdTask;

  @BeforeEach
  void setUp() {
    authenticate("TEACHER");
    action = action();
    planEntity = planEntity();
    AgentTaskEntity task = agentTask();
    createdTask = collectionTask(200L, "AGENT-FU-50", "draft");
    when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    when(actionMapper.selectById(50L)).thenAnswer(ignored -> action);
    when(actionMapper.confirm(anyLong(), anyInt(), anyLong(), any())).thenReturn(1);
    when(actionMapper.markExecuting(anyLong(), anyInt(), any())).thenReturn(1);
    when(actionMapper.markSucceeded(anyLong(), anyInt(), anyString(), any())).thenReturn(1);
    when(actionMapper.reject(anyLong(), anyInt(), anyString(), any())).thenReturn(1);
    when(planMapper.selectById(100L)).thenReturn(planEntity);
    when(planMapper.updateStatus(anyLong(), anyString(), anyString(), anyInt(), any()))
        .thenReturn(1);
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(agentTaskService.getDetail(1L)).thenReturn(new AgentTaskDetailVO());
    when(agentTaskService.updateProgress(anyLong(), anyInt(), any()))
        .thenReturn(new AgentTaskSummaryVO());
    when(collectionPlanService.get(1L)).thenReturn(planView());
    when(collectionTaskService.getById(12L)).thenReturn(sourceTask());
    when(collectionTaskService.create(any(HerbCollectionTaskCreateRequest.class)))
        .thenReturn(createdTask);
    when(stepMapper.selectMaxStepNo(1L)).thenReturn(8);
    AgentStepEntity confirmationStep = new AgentStepEntity();
    confirmationStep.setId(300L);
    confirmationStep.setStepNo(8);
    confirmationStep.setStepType("WAIT_FOR_CONFIRMATION");
    confirmationStep.setStatus("WAITING");
    when(stepMapper.selectByTaskId(1L)).thenReturn(List.of(confirmationStep));
    AtomicLong stepId = new AtomicLong(300);
    when(stepMapper.insertIfAbsent(any()))
        .thenAnswer(
            invocation -> {
              AgentStepEntity step = invocation.getArgument(0);
              step.setId(stepId.incrementAndGet());
              return 1;
            });
    when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
    when(stepMapper.markSucceeded(anyLong(), anyString(), anyString(), any())).thenReturn(1);
    when(stepMapper.markWaiting(anyLong(), any())).thenReturn(1);
    when(stepMapper.completeWaiting(anyLong(), anyString(), any(), any())).thenReturn(1);
    when(requirementMapper.insert(any())).thenReturn(1);
    when(businessLinkMapper.insert(any())).thenReturn(1);
    when(fieldDataConditionEvaluator.evaluate(anyLong(), any()))
        .thenReturn(
            new Snapshot(
                false,
                0,
                null,
                null,
                List.of(),
                List.of("尚未创建有效复测批次", "缺少根部图片"),
                0));
    when(stateMachine.transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class)))
        .thenAnswer(
            invocation -> {
              AgentTaskEntity value = invocation.getArgument(0);
              value.setStatus(invocation.getArgument(1, AgentTaskStatus.class).getCode());
              value.setVersion(value.getVersion() + 1);
              return value;
            });
    service =
        new AgentActionConfirmationService(
            actionMapper,
            taskMapper,
            planMapper,
            businessLinkMapper,
            requirementMapper,
            stepMapper,
            waitConditionMapper,
            fieldDataConditionEvaluator,
            agentTaskService,
            collectionPlanService,
            collectionTaskService,
            stateMachine,
            new ObjectMapper().findAndRegisterModules(),
            transactionManager);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void confirmationCreatesOneDraftTaskAndMovesAgentToWaitingFieldData() {
    AgentActionExecutionVO result =
        service.confirm(50L, new AgentActionConfirmRequest("确认复测", false));

    assertThat(result.actionStatus()).isEqualTo("SUCCEEDED");
    assertThat(result.collectionTaskId()).isEqualTo(200L);
    verify(collectionTaskService).create(any(HerbCollectionTaskCreateRequest.class));
    verify(collectionTaskService, never()).publish(anyLong());
    verify(requirementMapper, times(3)).insert(any());
    verify(businessLinkMapper).insert(any());
    ArgumentCaptor<AgentWaitConditionEntity> conditionCaptor =
        ArgumentCaptor.forClass(AgentWaitConditionEntity.class);
    verify(waitConditionMapper).insert(conditionCaptor.capture());
    assertThat(conditionCaptor.getValue().getCurrentSnapshotJson()).isNotBlank();
    assertThat(conditionCaptor.getValue().getLastCheckTime()).isNotNull();
    assertThat(conditionCaptor.getValue().getCheckCount()).isEqualTo(1);
    verify(stepMapper)
        .completeWaiting(
            org.mockito.ArgumentMatchers.eq(300L),
            org.mockito.ArgumentMatchers.eq("用户已确认复测采集方案"),
            org.mockito.ArgumentMatchers.isNull(),
            any());
    verify(stateMachine)
        .transition(any(), org.mockito.ArgumentMatchers.eq(AgentTaskStatus.WAITING_FIELD_DATA));
    verify(agentTaskService).updateProgress(1L, 72, "WAITING_FOR_FOLLOW_UP_DATA");
  }

  @Test
  void repeatedConfirmationReturnsExistingBusinessLinkWithoutCreatingAgain() {
    AgentBusinessLinkEntity link = new AgentBusinessLinkEntity();
    link.setAgentActionId(50L);
    link.setCollectionPlanId(100L);
    link.setBusinessId(200L);
    when(businessLinkMapper.selectByAction(50L, "FOLLOW_UP_COLLECTION_TASK")).thenReturn(link);
    when(collectionTaskService.getById(200L)).thenReturn(createdTask);

    AgentActionExecutionVO result =
        service.confirm(50L, new AgentActionConfirmRequest(null, false));

    assertThat(result.collectionTaskId()).isEqualTo(200L);
    verify(collectionTaskService, never()).create(any());
  }

  @Test
  void rejectionDoesNotCreateCollectionTask() {
    AgentActionExecutionVO result = service.reject(50L, new AgentActionRejectRequest("本周不安排"));

    assertThat(result.actionStatus()).isEqualTo("REJECTED");
    verify(actionMapper).reject(anyLong(), anyInt(), anyString(), any());
    verify(collectionTaskService, never()).create(any());
  }

  @Test
  void collectorCannotConfirmAndActionIsNotConsumedAsFailure() {
    authenticate("COLLECTOR");

    assertThatThrownBy(() -> service.confirm(50L, new AgentActionConfirmRequest("越权确认", false)))
        .isInstanceOf(ForbiddenException.class);
    verify(actionMapper, never()).confirm(anyLong(), anyInt(), anyLong(), any());
    verify(actionMapper, never()).markFailed(anyLong(), anyInt(), anyString(), any());
  }

  @Test
  void collectionTaskCreationFailureIsRecordedAfterTransactionRollback() {
    doThrow(new IllegalStateException("create failed"))
        .when(collectionTaskService)
        .create(any(HerbCollectionTaskCreateRequest.class));
    when(actionMapper.markFailed(anyLong(), anyInt(), anyString(), any())).thenReturn(1);

    assertThatThrownBy(() -> service.confirm(50L, new AgentActionConfirmRequest("确认复测", false)))
        .isInstanceOf(IllegalStateException.class);
    verify(actionMapper).markFailed(anyLong(), anyInt(), anyString(), any());
    verify(businessLinkMapper, never()).insert(any());
  }

  private AgentActionEntity action() {
    AgentActionEntity value = new AgentActionEntity();
    value.setId(50L);
    value.setAgentTaskId(1L);
    value.setActionType("CREATE_FOLLOW_UP_COLLECTION_TASK");
    value.setTargetId(100L);
    value.setStatus("WAITING_CONFIRMATION");
    value.setVersion(0);
    return value;
  }

  private AgentTaskEntity agentTask() {
    AgentTaskEntity value = new AgentTaskEntity();
    value.setId(1L);
    value.setStatus("WAITING_CONFIRMATION");
    value.setVersion(4);
    return value;
  }

  private AgentCollectionPlanEntity planEntity() {
    AgentCollectionPlanEntity value = new AgentCollectionPlanEntity();
    value.setId(100L);
    value.setAgentTaskId(1L);
    value.setCollectionTaskId(12L);
    value.setStatus("PROPOSED");
    value.setVersion(0);
    return value;
  }

  private AgentCollectionPlanVO planView() {
    return new AgentCollectionPlanVO(
        100L,
        "PLAN-100",
        1L,
        1,
        12L,
        "CONFIRMED",
        "已确认",
        "RULE_TEMPLATE",
        plan(),
        LocalDateTime.now(),
        LocalDateTime.now());
  }

  private FollowUpCollectionPlan plan() {
    return new FollowUpCollectionPlan(
        "复测目标",
        "DAYS_AFTER",
        3,
        LocalDateTime.now().plusDays(3),
        LocalDateTime.now().plusDays(7),
        List.of(new RequiredMetricItem("soilPh", "土壤 pH", true, "缺失", "测量", null)),
        List.of(new RequiredImageItem("root", "根部", 1, "拍摄根部", "缺图")),
        List.of(),
        List.of("完成全部必填项"),
        List.of(1L),
        "规则依据",
        "不能证明因果",
        "MEDIUM");
  }

  private HerbCollectionTaskVO sourceTask() {
    HerbCollectionTaskVO task = collectionTask(12L, "SOURCE-12", "in_progress");
    task.setTaskName("黄连连续观测");
    task.setSpeciesId(2L);
    task.setSpeciesName("黄连");
    task.setBaseId(3L);
    task.setBaseName("基地");
    task.setCollectPlace("地点");
    task.setCollectorId(7L);
    task.setCollectorName("采集员");
    return task;
  }

  private HerbCollectionTaskVO collectionTask(Long id, String code, String status) {
    HerbCollectionTaskVO task = new HerbCollectionTaskVO();
    task.setId(id);
    task.setTaskCode(code);
    task.setTaskStatus(status);
    return task;
  }

  private void authenticate(String role) {
    CurrentUser user = new CurrentUser(7L, "user", "用户", 1L, 1L, Set.of(role), Set.of(), Set.of());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
  }
}
