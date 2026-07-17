package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class AgentCollectionPlanAsyncDispatcher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AgentCollectionPlanAsyncDispatcher.class);

    private final AgentCollectionPlanService planService;
    private final AsyncTaskExecutor executor;
    private final Set<Long> runningTaskIds = ConcurrentHashMap.newKeySet();

    public AgentCollectionPlanAsyncDispatcher(
            AgentCollectionPlanService planService,
            @Qualifier("agentToolSecurityExecutor") AsyncTaskExecutor executor) {
        this.planService = planService;
        this.executor = executor;
    }

    public AgentCollectionPlanVO triggerGenerate(Long agentTaskId, boolean regenerate) {
        if (!runningTaskIds.add(agentTaskId)) {
            LOGGER.info("Agent collection plan generation already running: agentTaskId={}", agentTaskId);
            return null;
        }
        try {
            planService.validateGenerationRequest(agentTaskId, regenerate);
        } catch (RuntimeException exception) {
            runningTaskIds.remove(agentTaskId);
            throw exception;
        }
        executor.execute(
                () -> {
                    try {
                        planService.generate(agentTaskId, regenerate);
                    } catch (RuntimeException exception) {
                        LOGGER.warn(
                                "Agent collection plan async generation failed: agentTaskId={}",
                                agentTaskId,
                                exception);
                    } finally {
                        runningTaskIds.remove(agentTaskId);
                    }
                });
        return null;
    }
}
