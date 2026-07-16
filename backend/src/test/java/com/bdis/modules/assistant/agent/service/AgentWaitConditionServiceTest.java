package com.bdis.modules.assistant.agent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AgentWaitConditionServiceTest {

  @Mock private AgentWaitConditionMapper waitConditionMapper;
  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentStepMapper stepMapper;
  @Mock private AgentActionMapper actionMapper;
  @Mock private AgentFieldDataConditionEvaluator evaluator;
  @Mock private AgentTaskStateMachine stateMachine;
  @Mock private ApplicationEventPublisher eventPublisher;

  private AgentWaitConditionService service;
  private AgentWaitConditionEntity condition;

  @BeforeEach
  void setUp() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    condition = condition(objectMapper);
    when(waitConditionMapper.selectById(10L)).thenReturn(condition);
    service =
        new AgentWaitConditionService(
            waitConditionMapper,
            taskMapper,
            stepMapper,
            actionMapper,
            evaluator,
            stateMachine,
            objectMapper,
            eventPublisher);
  }

  @Test
  void satisfiedConditionMovesAgentToReanalyzingOnceAndCreatesPendingStep() {
    Snapshot snapshot = snapshot(true);
    AgentTaskEntity waiting = task("WAITING_FIELD_DATA", 4);
    AgentTaskEntity reanalyzing = task("REANALYZING", 5);
    when(evaluator.evaluate(anyLong(), any())).thenReturn(snapshot);
    when(waitConditionMapper.updateCheck(
            anyLong(), anyInt(), anyString(), anyString(), any(), any()))
        .thenReturn(1);
    when(taskMapper.selectById(1L)).thenReturn(waiting);
    when(stateMachine.transition(waiting, AgentTaskStatus.REANALYZING, "复测现场数据已满足等待条件"))
        .thenReturn(reanalyzing);
    when(stepMapper.completeWaiting(anyLong(), anyString(), anyString(), any())).thenReturn(1);
    when(stepMapper.selectMaxStepNo(1L)).thenReturn(9);
    when(stepMapper.insertIfAbsent(any())).thenReturn(1);
    when(taskMapper.updateProgress(anyLong(), anyInt(), anyInt(), anyString(), any()))
        .thenReturn(1);

    service.check(10L);

    verify(stateMachine).transition(waiting, AgentTaskStatus.REANALYZING, "复测现场数据已满足等待条件");
    verify(stepMapper).completeWaiting(anyLong(), anyString(), anyString(), any());
    verify(taskMapper).updateProgress(anyLong(), anyInt(), anyInt(), anyString(), any());
    verify(eventPublisher).publishEvent(any(Object.class));
  }

  @Test
  void optimisticConditionConflictDoesNotCreateDuplicateReanalysisStep() {
    when(evaluator.evaluate(anyLong(), any())).thenReturn(snapshot(true));
    when(waitConditionMapper.updateCheck(
            anyLong(), anyInt(), anyString(), anyString(), any(), any()))
        .thenReturn(0);

    service.check(10L);

    verify(stateMachine, never()).transition(any(), any(), any());
    verify(stepMapper, never()).insertIfAbsent(any());
  }

  @Test
  void cancelledConditionNeverRestoresAgent() {
    condition.setStatus("CANCELLED");

    service.check(10L);

    verify(waitConditionMapper).incrementTerminalCheckCount(anyLong(), any());
    verify(stateMachine, never()).transition(any(), any(), any());
  }

  @Test
  void expiredConditionCreatesConfirmationActionWithoutAnotherCollectionTask() {
    condition.setDeadline(LocalDateTime.now().minusMinutes(1));
    AgentTaskEntity waiting = task("WAITING_FIELD_DATA", 4);
    when(evaluator.evaluate(anyLong(), any())).thenReturn(snapshot(false));
    when(waitConditionMapper.updateCheck(
            anyLong(), anyInt(), anyString(), anyString(), any(), any()))
        .thenReturn(1);
    when(taskMapper.selectById(1L)).thenReturn(waiting);
    when(stateMachine.transition(waiting, AgentTaskStatus.WAITING_CONFIRMATION, "现场数据等待已过期"))
        .thenReturn(task("WAITING_CONFIRMATION", 5));

    service.check(10L);

    verify(actionMapper).insert(any());
    verify(stepMapper, never()).insertIfAbsent(any());
  }

  private AgentWaitConditionEntity condition(ObjectMapper objectMapper) throws Exception {
    AgentWaitConditionEntity value = new AgentWaitConditionEntity();
    value.setId(10L);
    value.setAgentTaskId(1L);
    value.setAgentStepId(2L);
    value.setFollowUpTaskId(200L);
    value.setConditionType("COMPOSITE_FIELD_DATA_READY");
    value.setConditionJson(
        objectMapper.writeValueAsString(
            new com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ConditionDefinition(
                List.of(), List.of(), true, LocalDateTime.now().minusDays(1))));
    value.setStatus("WAITING");
    value.setDeadline(LocalDateTime.now().plusDays(1));
    value.setVersion(0);
    return value;
  }

  private Snapshot snapshot(boolean satisfied) {
    return new Snapshot(
        satisfied,
        1,
        30L,
        LocalDateTime.now(),
        List.of("已创建复测批次"),
        satisfied ? List.of() : List.of("缺少土壤 pH"),
        satisfied ? 100 : 50);
  }

  private AgentTaskEntity task(String status, int version) {
    AgentTaskEntity value = new AgentTaskEntity();
    value.setId(1L);
    value.setStatus(status);
    value.setVersion(version);
    return value;
  }
}
