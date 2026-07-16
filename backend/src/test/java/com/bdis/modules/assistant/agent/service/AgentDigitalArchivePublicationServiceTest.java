package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentActionRejectRequest;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentBusinessLinkMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFieldDataMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentActionExecutionVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveResultVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveStatusVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AgentDigitalArchivePublicationServiceTest {

  @Mock private AgentActionMapper actionMapper;
  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentStepMapper stepMapper;
  @Mock private AgentBusinessLinkMapper businessLinkMapper;
  @Mock private AgentFieldDataMapper fieldDataMapper;
  @Mock private HerbCollectionTaskMapper collectionTaskMapper;
  @Mock private HerbDigitalTwinAgentTaskService agentTaskService;
  @Mock private AgentDigitalArchiveService digitalArchiveService;
  @Mock private GrowthRecordService growthRecordService;
  @Mock private HerbDigitalLifeArchiveService archiveService;
  @Mock private DigitalLifeIntegrityService integrityService;
  @Mock private AgentTaskStateMachine stateMachine;
  @Mock private PlatformTransactionManager transactionManager;

  private AgentDigitalArchivePublicationService service;
  private AgentTaskEntity task;

  @BeforeEach
  void setUp() {
    authenticate();
    when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    task = task();
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(agentTaskService.getDetail(1L)).thenReturn(new AgentTaskDetailVO());
    when(agentTaskService.updateProgress(anyLong(), anyInt(), anyString()))
        .thenReturn(new AgentTaskSummaryVO());
    when(stateMachine.transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class)))
        .thenAnswer(invocation -> {
          AgentTaskEntity value = invocation.getArgument(0);
          value.setStatus(invocation.getArgument(1, AgentTaskStatus.class).getCode());
          value.setVersion(value.getVersion() + 1);
          return value;
        });
    when(stateMachine.transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    service =
        new AgentDigitalArchivePublicationService(
            actionMapper, taskMapper, stepMapper, businessLinkMapper, fieldDataMapper,
            collectionTaskMapper, agentTaskService, digitalArchiveService, growthRecordService,
            archiveService, integrityService, stateMachine,
            new ObjectMapper().findAndRegisterModules(), transactionManager);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void confirmationPublishesApprovedStagesAndReissuesIntegrityVersion() {
    AgentActionEntity action = action();
    when(actionMapper.selectById(50L)).thenReturn(action);
    when(actionMapper.confirm(anyLong(), anyInt(), anyLong(), any())).thenReturn(1);
    when(actionMapper.markExecuting(anyLong(), anyInt(), any())).thenReturn(1);
    when(actionMapper.markSucceeded(anyLong(), anyInt(), anyString(), any())).thenReturn(1);
    when(digitalArchiveService.status(1L))
        .thenReturn(new AgentDigitalArchiveStatusVO(
            1L, "WAITING_CONFIRMATION", "等待公开确认", 100, 85,
            true, true, true, 50L, "ready"));
    when(fieldDataMapper.selectGrowthRecords(12L)).thenReturn(List.of(record(101L), record(102L)));
    HerbCollectionTaskEntity collectionTask = collectionTask();
    when(collectionTaskMapper.selectById(12L)).thenReturn(collectionTask);
    when(archiveService.publicArchive("DL-TASK-00000012"))
        .thenReturn(new HerbDigitalLifePublicArchiveVO());
    when(integrityService.generate(12L)).thenReturn(integrity());
    when(integrityService.verifyPublic("DL-TASK-00000012")).thenReturn(integrity());
    when(stepMapper.selectByTaskIdAndType(1L, "GENERATE_TRACE_QR"))
        .thenReturn(step(21L, "PENDING"));
    when(stepMapper.selectByTaskIdAndType(1L, "WAIT_PUBLIC_CONFIRMATION"))
        .thenReturn(step(22L, "WAITING"));
    when(stepMapper.selectByTaskIdAndType(1L, "COMPLETE_REPORT"))
        .thenReturn(step(23L, "PENDING"));
    when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
    when(stepMapper.markSucceeded(anyLong(), anyString(), any(), any())).thenReturn(1);
    when(stepMapper.completeWaiting(anyLong(), anyString(), any(), any())).thenReturn(1);
    when(businessLinkMapper.insert(any())).thenReturn(1);
    when(digitalArchiveService.result(1L)).thenReturn(result(true));

    AgentActionExecutionVO result = service.confirm(50L);

    assertThat(result.actionStatus()).isEqualTo("SUCCEEDED");
    verify(growthRecordService, times(2)).generateTraceQrCode(anyLong());
    verify(growthRecordService, times(2)).enablePublicTrace(anyLong());
    verify(integrityService).generate(12L);
    verify(integrityService).verifyPublic("DL-TASK-00000012");
    verify(stateMachine)
        .transition(any(AgentTaskEntity.class), org.mockito.ArgumentMatchers.eq(AgentTaskStatus.COMPLETED), anyString());
  }

  @Test
  void rejectionCompletesInternalArchiveWithoutPublishing() {
    AgentActionEntity action = action();
    when(actionMapper.selectById(50L)).thenReturn(action);
    when(actionMapper.reject(anyLong(), anyInt(), anyString(), any())).thenReturn(1);
    when(stepMapper.selectByTaskIdAndType(1L, "GENERATE_TRACE_QR"))
        .thenReturn(step(21L, "PENDING"));
    when(stepMapper.selectByTaskIdAndType(1L, "WAIT_PUBLIC_CONFIRMATION"))
        .thenReturn(step(22L, "WAITING"));
    when(stepMapper.selectByTaskIdAndType(1L, "COMPLETE_REPORT"))
        .thenReturn(step(23L, "PENDING"));
    when(stepMapper.markSkipped(anyLong(), anyString(), any())).thenReturn(1);
    when(stepMapper.completeWaiting(anyLong(), anyString(), any(), any())).thenReturn(1);
    when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
    when(stepMapper.markSucceeded(anyLong(), anyString(), any(), any())).thenReturn(1);
    when(digitalArchiveService.result(1L)).thenReturn(result(false));

    AgentActionExecutionVO result =
        service.reject(50L, new AgentActionRejectRequest("暂不公开"));

    assertThat(result.actionStatus()).isEqualTo("REJECTED");
    verify(growthRecordService, never()).enablePublicTrace(anyLong());
    verify(agentTaskService).updateProgress(1L, 100, "INTERNAL_ARCHIVE_COMPLETED");
  }

  private AgentActionEntity action() {
    AgentActionEntity action = new AgentActionEntity();
    action.setId(50L);
    action.setAgentTaskId(1L);
    action.setActionType("ENABLE_PUBLIC_TRACE");
    action.setStatus("WAITING_CONFIRMATION");
    action.setVersion(0);
    return action;
  }

  private AgentTaskEntity task() {
    AgentTaskEntity value = new AgentTaskEntity();
    value.setId(1L);
    value.setCollectionTaskId(12L);
    value.setStatus("WAITING_CONFIRMATION");
    value.setVersion(3);
    return value;
  }

  private GrowthRecordEntity record(Long id) {
    GrowthRecordEntity record = new GrowthRecordEntity();
    record.setId(id);
    record.setReviewStatus("approved");
    return record;
  }

  private HerbCollectionTaskEntity collectionTask() {
    HerbCollectionTaskEntity value = new HerbCollectionTaskEntity();
    value.setId(12L);
    value.setTaskCode("TASK-12");
    value.setTaskStatus("completed");
    value.setTraceCode("DL-TASK-00000012");
    value.setPublicVisible(1);
    return value;
  }

  private AgentStepEntity step(Long id, String status) {
    AgentStepEntity step = new AgentStepEntity();
    step.setId(id);
    step.setStatus(status);
    return step;
  }

  private DigitalLifeIntegrityVO integrity() {
    return new DigitalLifeIntegrityVO(
        true, 12, "0123456789abcdef", "sha256-v1:3", LocalDateTime.now(),
        null, null, "verified");
  }

  private AgentDigitalArchiveResultVO result(boolean publicVisible) {
    return new AgentDigitalArchiveResultVO(
        1L, 12L, "TASK-12", publicVisible ? "DL-TASK-00000012" : null,
        null, null, 2, 2, 100, true, "0123456789ab", "sha256-v1:3",
        publicVisible, LocalDateTime.now(), List.of("哈希校验"), List.of());
  }

  private void authenticate() {
    CurrentUser user =
        new CurrentUser(7L, "teacher", "教师", 1L, 1L, Set.of("TEACHER"), Set.of(), Set.of());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
  }
}
