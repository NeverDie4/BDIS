package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Built;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Finding;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Stage;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveResultVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.DigitalLifeNarrationService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeImageVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentDigitalArchiveServiceTest {

  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentStepMapper stepMapper;
  @Mock private AgentActionMapper actionMapper;
  @Mock private HerbDigitalTwinAgentTaskService agentTaskService;
  @Mock private AgentReanalysisSnapshotBuilder snapshotBuilder;
  @Mock private HerbDigitalLifeArchiveService archiveService;
  @Mock private DigitalLifeNarrationService narrationService;
  @Mock private DigitalLifeIntegrityService integrityService;

  private AgentDigitalArchiveService service;

  @BeforeEach
  void setUp() {
    service =
        new AgentDigitalArchiveService(
            taskMapper, stepMapper, actionMapper, agentTaskService, snapshotBuilder,
            archiveService, narrationService, integrityService,
            new ObjectMapper().findAndRegisterModules(), 85);
  }

  @Test
  void completeApprovedStagesSatisfyPrerequisites() {
    assertThat(service.prerequisiteViolations(archive("approved"), snapshot(List.of())))
        .isEmpty();
  }

  @Test
  void unapprovedStageAndHighFindingBlockPreparation() {
    List<String> violations =
        service.prerequisiteViolations(
            archive("submitted"),
            snapshot(List.of(new Finding(1L, "MISSING_IMAGE", "HIGH", "OPEN"))));

    assertThat(violations)
        .contains("存在未审核通过的阶段", "存在高风险未处理发现项");
  }

  @Test
  void prepareReusesNarrationAndIntegrityServicesAndCreatesConfirmationAction() {
    AgentTaskEntity task = new AgentTaskEntity();
    task.setId(1L);
    task.setCollectionTaskId(12L);
    task.setStatus("RUNNING");
    task.setVersion(2);
    when(taskMapper.selectById(1L)).thenReturn(task);
    when(agentTaskService.getDetail(1L)).thenReturn(new AgentTaskDetailVO());
    when(agentTaskService.updateProgress(anyLong(), anyInt(), any()))
        .thenReturn(new AgentTaskSummaryVO());
    when(archiveService.getByTaskId(12L)).thenReturn(archive("approved"));
    when(snapshotBuilder.build(1L, List.of(12L)))
        .thenReturn(new Built(snapshot(List.of()), "{}", "snapshot"));
    when(narrationService.generateForTask(12L))
        .thenReturn(new DigitalLifeNarrationGenerationVO(2, 0, 0));
    DigitalLifeIntegrityVO missing =
        new DigitalLifeIntegrityVO(false, 0, null, "sha256-v1", null, null, null, "missing");
    DigitalLifeIntegrityVO verified = integrity();
    when(integrityService.verify(12L)).thenReturn(missing, verified);
    when(integrityService.generate(12L)).thenReturn(verified);
    when(actionMapper.selectByTaskId(1L)).thenReturn(List.of());
    when(stepMapper.selectMaxStepNo(1L)).thenReturn(10);
    AtomicLong ids = new AtomicLong(100);
    when(stepMapper.insertIfAbsent(any(AgentStepEntity.class)))
        .thenAnswer(invocation -> {
          AgentStepEntity step = invocation.getArgument(0);
          step.setId(ids.incrementAndGet());
          return 1;
        });
    when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
    when(stepMapper.markSucceeded(anyLong(), any(), any(), any())).thenReturn(1);
    when(stepMapper.markWaiting(anyLong(), any())).thenReturn(1);
    when(actionMapper.insert(any(AgentActionEntity.class))).thenReturn(1);

    AgentDigitalArchiveResultVO result = service.prepare(1L);

    assertThat(result.integrityVerified()).isTrue();
    assertThat(result.completenessScore()).isEqualTo(100);
    verify(narrationService).generateForTask(12L);
    verify(integrityService).generate(12L);
    ArgumentCaptor<AgentActionEntity> action = ArgumentCaptor.forClass(AgentActionEntity.class);
    verify(actionMapper).insert(action.capture());
    assertThat(action.getValue().getActionType()).isEqualTo("ENABLE_PUBLIC_TRACE");
    assertThat(action.getValue().getRiskLevel()).isEqualTo("HIGH");
  }

  private HerbDigitalLifeArchiveVO archive(String auditStatus) {
    HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
    archive.setTaskId(12L);
    archive.setTaskCode("TASK-12");
    archive.setStageCount(2);
    archive.setImageCount(2);
    archive.setPublicVisible(false);
    archive.setStages(List.of(stage(1L, auditStatus), stage(2L, auditStatus)));
    return archive;
  }

  private HerbDigitalLifeStageVO stage(Long id, String auditStatus) {
    HerbDigitalLifeStageVO stage = new HerbDigitalLifeStageVO();
    stage.setBatchId(id);
    stage.setGrowthRecordId(id + 10);
    stage.setCollectedAt(LocalDateTime.of(2026, 7, id.intValue(), 10, 0));
    stage.setAuditStatus(auditStatus);
    HerbDigitalLifeMetricsVO metrics = new HerbDigitalLifeMetricsVO();
    metrics.setPlantHeight(BigDecimal.TEN);
    stage.setMetrics(metrics);
    HerbDigitalLifeImageVO image = new HerbDigitalLifeImageVO();
    image.setImageId(id + 20);
    stage.setImages(List.of(image));
    return stage;
  }

  private AgentReanalysisSnapshot snapshot(List<Finding> findings) {
    return new AgentReanalysisSnapshot(
        List.of(
            new Stage(12L, 1L, LocalDateTime.of(2026, 7, 1, 10, 0), 11L, "approved",
                Map.of("plantHeight", BigDecimal.TEN), Map.of("whole", 1), 1, 1),
            new Stage(12L, 2L, LocalDateTime.of(2026, 7, 2, 10, 0), 12L, "approved",
                Map.of("plantHeight", BigDecimal.ONE), Map.of("whole", 1), 1, 1)),
        findings, 100, "READY");
  }

  private DigitalLifeIntegrityVO integrity() {
    return new DigitalLifeIntegrityVO(
        true, 8, "0123456789abcdef", "sha256-v1:2", LocalDateTime.now(),
        null, null, "verified");
  }
}
