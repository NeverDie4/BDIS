package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentTaskCancelRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskCreateRequest;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.service.impl.HerbDigitalTwinAgentTaskServiceImpl;
import com.bdis.modules.assistant.agent.support.AgentTaskAccessService;
import com.bdis.modules.assistant.agent.support.AgentTaskNoGenerator;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.assistant.mapper.HerbAiChatSessionMapper;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class HerbDigitalTwinAgentTaskServiceImplTest {

  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentStepMapper stepMapper;
  @Mock private AgentFindingMapper findingMapper;
  @Mock private AgentActionMapper actionMapper;

  @Mock private AgentWaitConditionMapper waitConditionMapper;
  @Mock private HerbCollectionTaskMapper collectionTaskMapper;
  @Mock private HerbAiChatSessionMapper chatSessionMapper;
  @Mock private CollectionAccessService collectionAccessService;
  @Mock private AgentTaskAccessService accessService;
  @Mock private AgentTaskStateMachine stateMachine;

  private HerbDigitalTwinAgentTaskServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new HerbDigitalTwinAgentTaskServiceImpl(
            taskMapper,
            stepMapper,
            findingMapper,
            actionMapper,
            waitConditionMapper,
            collectionTaskMapper,
            chatSessionMapper,
            collectionAccessService,
            accessService,
            new AgentTaskNoGenerator(),
            stateMachine,
            new ObjectMapper());
    authenticate(1001L, "TEACHER");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createUsesJwtUserAndCreatesLoadContextStepInSameServiceCall() {
    HerbCollectionTaskEntity collectionTask = collectionTask();
    when(collectionTaskMapper.selectByIdForUpdate(12L)).thenReturn(collectionTask);
    doAnswer(
            invocation -> {
              invocation.<AgentTaskEntity>getArgument(0).setId(1L);
              return 1;
            })
        .when(taskMapper)
        .insert(any(AgentTaskEntity.class));
    when(taskMapper.updateTaskNo(anyLong(), anyString(), anyString(), any())).thenReturn(1);
    when(stepMapper.insert(any(AgentStepEntity.class))).thenReturn(1);

    AgentTaskSummaryVO result = service.create(validCreateRequest());

    ArgumentCaptor<AgentTaskEntity> taskCaptor = ArgumentCaptor.forClass(AgentTaskEntity.class);
    ArgumentCaptor<AgentStepEntity> stepCaptor = ArgumentCaptor.forClass(AgentStepEntity.class);
    verify(taskMapper).insert(taskCaptor.capture());
    verify(stepMapper).insert(stepCaptor.capture());
    assertThat(taskCaptor.getValue().getUserId()).isEqualTo(1001L);
    assertThat(taskCaptor.getValue().getStatus()).isEqualTo("CREATED");
    assertThat(stepCaptor.getValue().getAgentTaskId()).isEqualTo(1L);
    assertThat(stepCaptor.getValue().getStepType()).isEqualTo("LOAD_CONTEXT");
    assertThat(stepCaptor.getValue().getStatus()).isEqualTo("PENDING");
    assertThat(result.getTaskNo()).startsWith("AGENT-DT-");
    assertThat(result.getStatusLabel()).isEqualTo("已创建");
  }

  @Test
  void createFailsWhenCollectionTaskIsOutsideTeacherScope() {
    HerbCollectionTaskEntity collectionTask = collectionTask();
    when(collectionTaskMapper.selectByIdForUpdate(12L)).thenReturn(collectionTask);
    doThrow(new ForbiddenException("只能管理本人创建的采集任务"))
        .when(accessService)
        .requireCreateAccess(collectionTask);

    assertThatThrownBy(() -> service.create(validCreateRequest()))
        .isInstanceOf(ForbiddenException.class);
    verify(taskMapper, never()).insert(any());
    verify(stepMapper, never()).insert(any());
  }

  @Test
  void activeDuplicateTaskIsRejectedBeforeInsert() {
    when(collectionTaskMapper.selectByIdForUpdate(12L)).thenReturn(collectionTask());
    AgentTaskEntity duplicate = new AgentTaskEntity();
    duplicate.setId(99L);
    when(taskMapper.selectActiveDuplicate(1001L, 12L, "DIGITAL_TWIN_RESEARCH"))
        .thenReturn(duplicate);

    assertThatThrownBy(() -> service.create(validCreateRequest()))
        .isInstanceOf(ResourceConflictException.class)
        .hasMessageContaining("已有正在处理");
    verify(taskMapper, never()).insert(any());
  }

  @Test
  void concurrentUniqueConflictDoesNotCreateDuplicateStep() {
    when(collectionTaskMapper.selectByIdForUpdate(12L)).thenReturn(collectionTask());
    doThrow(new DuplicateKeyException("active key"))
        .when(taskMapper)
        .insert(any(AgentTaskEntity.class));

    assertThatThrownBy(() -> service.create(validCreateRequest()))
        .isInstanceOf(ResourceConflictException.class)
        .hasMessageContaining("已有正在处理");
    verify(stepMapper, never()).insert(any());
  }

  @Test
  void missingCollectionTargetReturnsNotFound() {
    when(collectionTaskMapper.selectByIdForUpdate(12L)).thenReturn(null);

    assertThatThrownBy(() -> service.create(validCreateRequest()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("采集任务不存在");
  }

  @Test
  void invalidGoalAndMismatchedTargetAreRejected() {
    AgentTaskCreateRequest invalidGoal = validCreateRequest();
    invalidGoal.setGoalType("UNKNOWN");
    assertThatThrownBy(() -> service.create(invalidGoal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("目标类型无效");

    AgentTaskCreateRequest mismatched = validCreateRequest();
    mismatched.setTargetId(13L);
    assertThatThrownBy(() -> service.create(mismatched))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("必须与采集任务 ID 一致");
  }

  @Test
  void cancelTransitionsTaskAndCancelsUnfinishedChildren() {
    AgentTaskEntity task = agentTask(AgentTaskStatus.WAITING_FIELD_DATA);
    HerbCollectionTaskEntity collectionTask = collectionTask();
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(collectionTaskMapper.selectById(12L)).thenReturn(collectionTask);
    when(stepMapper.selectByTaskId(1L)).thenReturn(List.of());
    when(findingMapper.selectByTaskId(1L)).thenReturn(List.of());
    when(actionMapper.selectByTaskId(1L)).thenReturn(List.of());
    doAnswer(
            invocation -> {
              AgentTaskEntity transitioning = invocation.getArgument(0);
              transitioning.setStatus("CANCELLED");
              transitioning.setCancelTime(LocalDateTime.now());
              transitioning.setFinishTime(transitioning.getCancelTime());
              transitioning.setResultSummary(invocation.getArgument(2));
              return transitioning;
            })
        .when(stateMachine)
        .transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class), anyString());
    AgentTaskCancelRequest request = new AgentTaskCancelRequest();
    request.setReason("用户取消本次分析");

    AgentTaskDetailVO result = service.cancel(1L, request);

    verify(stepMapper).cancelUnfinished(1L, task.getCancelTime());
    verify(actionMapper).cancelUnexecuted(1L, task.getCancelTime());
    verify(waitConditionMapper).cancelByAgentTaskId(1L, task.getCancelTime());
    assertThat(result.getStatus()).isEqualTo("CANCELLED");
    assertThat(result.getResultSummary()).contains("用户取消本次分析");
  }

  private AgentTaskCreateRequest validCreateRequest() {
    AgentTaskCreateRequest request = new AgentTaskCreateRequest();
    request.setGoalType("DIGITAL_TWIN_RESEARCH");
    request.setGoalText("持续观察这个黄连任务，数据完整后生成可信数字生命档案。");
    request.setTargetType("COLLECTION_TASK");
    request.setTargetId(12L);
    request.setCollectionTaskId(12L);
    request.setPageContext("digital-life");
    return request;
  }

  private HerbCollectionTaskEntity collectionTask() {
    HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
    task.setId(12L);
    task.setTaskName("黄连连续观测任务");
    task.setSpeciesId(5L);
    task.setCreatedBy(1001L);
    task.setCollectorId(2001L);
    return task;
  }

  private AgentTaskEntity agentTask(AgentTaskStatus status) {
    AgentTaskEntity task = new AgentTaskEntity();
    task.setId(1L);
    task.setTaskNo("AGENT-DT-20260716-000001");
    task.setUserId(1001L);
    task.setGoalType("DIGITAL_TWIN_RESEARCH");
    task.setGoalText("持续观察黄连任务");
    task.setTargetType("COLLECTION_TASK");
    task.setTargetId(12L);
    task.setCollectionTaskId(12L);
    task.setStatus(status.getCode());
    task.setProgressPercent(0);
    task.setVersion(0);
    task.setCreateTime(LocalDateTime.now());
    task.setUpdateTime(LocalDateTime.now());
    return task;
  }

  private void authenticate(Long userId, String role) {
    CurrentUser current =
        new CurrentUser(
            userId,
            "user-" + userId,
            "测试教师",
            1L,
            1L,
            Set.of(role),
            Set.of(1L),
            Set.of("growth:record:view", "growth:record:create"));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(current, null));
  }
}
