package com.bdis.modules.assistant.agent.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentReanalysisFailureRecorderTest {

  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentTaskStateMachine stateMachine;

  @Test
  void systemFailureTransitionsOnlyReanalyzingTaskToFailed() {
    AgentTaskEntity task = new AgentTaskEntity();
    task.setId(1L);
    task.setStatus("REANALYZING");
    task.setVersion(3);
    when(taskMapper.selectById(1L)).thenReturn(task);
    AgentReanalysisFailureRecorder recorder =
        new AgentReanalysisFailureRecorder(taskMapper, stateMachine);

    recorder.record(1L, new IllegalStateException("database unavailable"));

    verify(stateMachine)
        .transition(task, AgentTaskStatus.FAILED, "重新分析发生不可恢复的系统错误：database unavailable");
  }
}
