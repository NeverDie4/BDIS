package com.bdis.modules.assistant.agent.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentTaskStateMachineTest {

  @Mock private AgentTaskMapper taskMapper;

  private AgentTaskStateMachine stateMachine;

  @BeforeEach
  void setUp() {
    stateMachine = new AgentTaskStateMachine(taskMapper);
  }

  @Test
  void legalTransitionUpdatesStatusTimeAndVersion() {
    when(taskMapper.updateState(
            anyLong(),
            anyString(),
            anyInt(),
            anyString(),
            nullable(String.class),
            any(LocalDateTime.class),
            nullable(LocalDateTime.class),
            nullable(LocalDateTime.class),
            any(LocalDateTime.class)))
        .thenReturn(1);
    AgentTaskEntity task = task(AgentTaskStatus.CREATED, 0);

    stateMachine.transition(task, AgentTaskStatus.PLANNING);

    assertThat(task.getStatus()).isEqualTo("PLANNING");
    assertThat(task.getStartTime()).isNotNull();
    assertThat(task.getVersion()).isEqualTo(1);
  }

  @Test
  void illegalTransitionThrowsBusinessException() {
    AgentTaskEntity task = task(AgentTaskStatus.CREATED, 0);

    assertThatThrownBy(() -> stateMachine.transition(task, AgentTaskStatus.COMPLETED))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("CREATED -> COMPLETED");
    verifyNoInteractions(taskMapper);
  }

  @Test
  void completedAndCancelledTasksCannotTransitionAgain() {
    for (AgentTaskStatus terminal : Set.of(AgentTaskStatus.COMPLETED, AgentTaskStatus.CANCELLED)) {
      assertThatThrownBy(() -> stateMachine.transition(task(terminal, 2), AgentTaskStatus.RUNNING))
          .isInstanceOf(ResourceConflictException.class)
          .hasMessageContaining("终态");
    }
    verifyNoInteractions(taskMapper);
  }

  @Test
  void optimisticLockConflictDoesNotAdvanceInMemoryTask() {
    when(taskMapper.updateState(
            anyLong(),
            anyString(),
            anyInt(),
            anyString(),
            nullable(String.class),
            any(LocalDateTime.class),
            nullable(LocalDateTime.class),
            nullable(LocalDateTime.class),
            any(LocalDateTime.class)))
        .thenReturn(0);
    AgentTaskEntity task = task(AgentTaskStatus.CREATED, 0);

    assertThatThrownBy(() -> stateMachine.transition(task, AgentTaskStatus.PLANNING))
        .isInstanceOf(ResourceConflictException.class)
        .hasMessageContaining("其他请求推进");
    assertThat(task.getStatus()).isEqualTo("CREATED");
    assertThat(task.getVersion()).isZero();
  }

  @Test
  void transitionTableMatchesWorkflowContract() {
    Map<AgentTaskStatus, Set<AgentTaskStatus>> expected =
        Map.of(
            AgentTaskStatus.CREATED, Set.of(AgentTaskStatus.PLANNING, AgentTaskStatus.CANCELLED),
            AgentTaskStatus.PLANNING,
                Set.of(AgentTaskStatus.RUNNING, AgentTaskStatus.FAILED, AgentTaskStatus.CANCELLED),
            AgentTaskStatus.RUNNING,
                Set.of(
                    AgentTaskStatus.WAITING_CONFIRMATION,
                    AgentTaskStatus.WAITING_FIELD_DATA,
                    AgentTaskStatus.REANALYZING,
                    AgentTaskStatus.COMPLETED,
                    AgentTaskStatus.FAILED,
                    AgentTaskStatus.CANCELLED),
            AgentTaskStatus.WAITING_CONFIRMATION,
                Set.of(AgentTaskStatus.RUNNING, AgentTaskStatus.CANCELLED, AgentTaskStatus.FAILED),
            AgentTaskStatus.WAITING_FIELD_DATA,
                Set.of(
                    AgentTaskStatus.REANALYZING,
                    AgentTaskStatus.WAITING_CONFIRMATION,
                    AgentTaskStatus.CANCELLED,
                    AgentTaskStatus.FAILED),
            AgentTaskStatus.REANALYZING,
                Set.of(
                    AgentTaskStatus.RUNNING,
                    AgentTaskStatus.WAITING_CONFIRMATION,
                    AgentTaskStatus.WAITING_FIELD_DATA,
                    AgentTaskStatus.COMPLETED,
                    AgentTaskStatus.FAILED,
                    AgentTaskStatus.CANCELLED));

    for (AgentTaskStatus source : AgentTaskStatus.values()) {
      for (AgentTaskStatus target : AgentTaskStatus.values()) {
        assertThat(stateMachine.canTransition(source, target))
            .as("%s -> %s", source, target)
            .isEqualTo(expected.getOrDefault(source, Set.of()).contains(target));
      }
    }
  }

  private AgentTaskEntity task(AgentTaskStatus status, int version) {
    AgentTaskEntity task = new AgentTaskEntity();
    task.setId(1L);
    task.setStatus(status.getCode());
    task.setVersion(version);
    task.setCreateTime(LocalDateTime.now());
    return task;
  }
}
