package com.bdis.modules.assistant.agent.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.AgentCollectionPlanService;
import com.bdis.modules.auth.service.CurrentUserService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AgentPlanRecoveryJobTest {

  @Mock private AgentTaskMapper taskMapper;
  @Mock private CurrentUserService currentUserService;
  @Mock private AgentCollectionPlanService planService;

  @Test
  void stalledPlanIsRecoveredOnWorkerThreadWithTaskOwnerSecurityContext() throws Exception {
    AgentTaskEntity task = new AgentTaskEntity();
    task.setId(11L);
    task.setUserId(7L);
    CurrentUser owner =
        new CurrentUser(
            7L,
            "teacher",
            "教师",
            null,
            null,
            Set.of("TEACHER"),
            Set.of(2L),
            Set.of("growth:record:view"));
    when(taskMapper.selectPlanRecoveryCandidates(50)).thenReturn(List.of(task));
    when(currentUserService.load(7L)).thenReturn(owner);
    when(planService.generate(11L, false))
        .thenAnswer(
            ignored -> {
              assertThat(SecurityUtils.currentUser().getUserId()).isEqualTo(7L);
              return null;
            });
    AgentPlanRecoveryJob job =
        new AgentPlanRecoveryJob(taskMapper, currentUserService, planService);
    ReflectionTestUtils.setField(job, "batchSize", 50);

    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      Future<?> future = executor.submit(job::recoverPlans);
      future.get();
    }

    verify(planService).generate(11L, false);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
