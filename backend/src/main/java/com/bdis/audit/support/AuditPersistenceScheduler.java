package com.bdis.audit.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AuditPersistenceScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPersistenceScheduler.class);

    public void afterCommit(String eventType, Runnable writer) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runSafely(eventType, writer);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        runSafely(eventType, writer);
                    }
                });
    }

    public void afterCompletion(String eventType, Runnable writer) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runSafely(eventType, writer);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        runSafely(eventType, writer);
                    }
                });
    }

    private void runSafely(String eventType, Runnable writer) {
        try {
            writer.run();
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to persist {} audit event", eventType, exception);
        }
    }
}
