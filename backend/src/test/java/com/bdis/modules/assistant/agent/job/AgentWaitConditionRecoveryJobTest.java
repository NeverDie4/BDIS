package com.bdis.modules.assistant.agent.job;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.service.AgentWaitConditionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AgentWaitConditionRecoveryJobTest {

  @Mock private AgentWaitConditionMapper waitConditionMapper;
  @Mock private AgentWaitConditionService waitConditionService;
  @InjectMocks private AgentWaitConditionRecoveryJob job;

  @Test
  void persistedCandidateIsRecheckedAfterRestartWithoutInMemoryTimer() {
    AgentWaitConditionEntity condition = new AgentWaitConditionEntity();
    condition.setId(10L);
    condition.setAgentTaskId(1L);
    ReflectionTestUtils.setField(job, "batchSize", 100);
    when(waitConditionMapper.selectRecoveryCandidates(100)).thenReturn(List.of(condition));

    job.recoverWaitingTasks();

    verify(waitConditionService).check(10L);
  }
}
