package com.bdis.modules.assistant.agent.job;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.AgentCollectionPlanService;
import com.bdis.modules.auth.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AgentPlanRecoveryJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgentPlanRecoveryJob.class);

  private final AgentTaskMapper taskMapper;
  private final CurrentUserService currentUserService;
  private final AgentCollectionPlanService planService;

  @Value("${assistant.agent.plan-recovery-batch-size:50}")
  private int batchSize;

  public AgentPlanRecoveryJob(
      AgentTaskMapper taskMapper,
      CurrentUserService currentUserService,
      AgentCollectionPlanService planService) {
    this.taskMapper = taskMapper;
    this.currentUserService = currentUserService;
    this.planService = planService;
  }

  @Scheduled(
      initialDelayString = "#{${assistant.agent.plan-recovery-initial-delay-seconds:5} * 1000}",
      fixedDelayString = "#{${assistant.agent.plan-recovery-interval-seconds:30} * 1000}")
  public void recoverPlans() {
    for (AgentTaskEntity task :
        taskMapper.selectPlanRecoveryCandidates(Math.max(1, batchSize))) {
      recoverPlan(task);
    }
  }

  private void recoverPlan(AgentTaskEntity task) {
    SecurityContext previousContext = SecurityContextHolder.getContext();
    try {
      CurrentUser owner = currentUserService.load(task.getUserId());
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              owner,
              null,
              owner.getPermissions().stream().map(SimpleGrantedAuthority::new).toList());
      SecurityContext recoveryContext = SecurityContextHolder.createEmptyContext();
      recoveryContext.setAuthentication(authentication);
      SecurityContextHolder.setContext(recoveryContext);
      planService.generate(task.getId(), false);
      LOGGER.info("Recovered Agent collection plan generation, taskId={}", task.getId());
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Agent collection plan recovery failed, taskId={}: {}",
          task.getId(),
          exception.getMessage());
    } finally {
      SecurityContextHolder.setContext(previousContext);
    }
  }
}
