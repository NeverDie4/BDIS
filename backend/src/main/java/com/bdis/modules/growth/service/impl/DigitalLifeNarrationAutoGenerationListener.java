package com.bdis.modules.growth.service.impl;

import com.bdis.modules.growth.service.DigitalLifeNarrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DigitalLifeNarrationAutoGenerationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DigitalLifeNarrationAutoGenerationListener.class);

    private final DigitalLifeNarrationService narrationService;

    public DigitalLifeNarrationAutoGenerationListener(DigitalLifeNarrationService narrationService) {
        this.narrationService = narrationService;
    }

    @Async("agentToolSecurityExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void generate(Requested event) {
        try {
            narrationService.generateForTask(event.taskId());
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Automatic digital life narration generation failed, taskId={}",
                    event.taskId(),
                    exception);
        }
    }

    public record Requested(Long taskId) {}
}
