package com.bdis.modules.assistant.agent.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bdis.modules.assistant.agent.service.AgentReanalysisFailureRecorder;
import com.bdis.modules.assistant.agent.service.AgentReanalysisService;
import com.bdis.modules.assistant.agent.service.AgentWaitConditionService.AgentWorkflowResumeRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentWorkflowResumeDispatcherTest {

  @Mock private AgentReanalysisService reanalysisService;
  @Mock private AgentReanalysisFailureRecorder failureRecorder;

  @Test
  void dispatcherRunsReanalysisAndContainsFailureOutsideUploadTransaction() {
    doThrow(new IllegalStateException("reanalyze failed")).when(reanalysisService).run(1L);
    AgentWorkflowResumeDispatcher dispatcher =
        new AgentWorkflowResumeDispatcher(reanalysisService, failureRecorder);

    assertThatCode(() -> dispatcher.dispatch(new AgentWorkflowResumeRequestedEvent(1L)))
        .doesNotThrowAnyException();
    verify(reanalysisService).run(1L);
    verify(failureRecorder).record(eq(1L), any(RuntimeException.class));
  }
}
