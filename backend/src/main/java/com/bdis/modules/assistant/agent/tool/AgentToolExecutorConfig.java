package com.bdis.modules.assistant.agent.tool;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class AgentToolExecutorConfig {

    @Bean(name = "agentToolWorkerExecutor")
    public ThreadPoolTaskExecutor agentToolWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("agent-read-tool-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "agentToolSecurityExecutor")
    public AsyncTaskExecutor agentToolSecurityExecutor(
            @Qualifier("agentToolWorkerExecutor") ThreadPoolTaskExecutor delegate) {
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }
}
