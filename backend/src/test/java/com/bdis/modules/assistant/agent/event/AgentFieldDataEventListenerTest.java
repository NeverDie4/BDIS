package com.bdis.modules.assistant.agent.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

import com.bdis.modules.assistant.agent.service.AgentWaitConditionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentFieldDataEventListenerTest {

  @Mock private AgentWaitConditionService waitConditionService;

  @Test
  void listenerFailureIsContainedAndCannotAffectCommittedUpload() {
    doThrow(new IllegalStateException("check failed"))
        .when(waitConditionService)
        .checkByFollowUpTaskId(200L);
    AgentFieldDataEventListener listener = new AgentFieldDataEventListener(waitConditionService);

    assertThatCode(
            () ->
                listener.onFieldDataChanged(
                    new AgentFieldDataChangedEvent(
                        AgentFieldDataChangedEvent.Type.IMAGE_UPLOADED, 200L, 20L, 30L, null)))
        .doesNotThrowAnyException();
  }
}
