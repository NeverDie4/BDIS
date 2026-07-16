package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisExplanation;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Built;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Stage;
import com.bdis.modules.assistant.agent.entity.AgentAnalysisRoundEntity;
import com.bdis.modules.assistant.agent.entity.AgentBusinessLinkEntity;
import com.bdis.modules.assistant.agent.entity.AgentFindingEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentAnalysisRoundMapper;
import com.bdis.modules.assistant.agent.mapper.AgentBusinessLinkMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentReanalysisComparisonVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentReanalysisServiceTest {

  @Mock private AgentTaskMapper taskMapper;
  @Mock private AgentStepMapper stepMapper;
  @Mock private AgentFindingMapper findingMapper;
  @Mock private AgentActionMapper actionMapper;
  @Mock private AgentAnalysisRoundMapper roundMapper;
  @Mock private AgentBusinessLinkMapper businessLinkMapper;
  @Mock private AgentReanalysisSnapshotBuilder snapshotBuilder;
  @Mock private AgentReanalysisExplanationService explanationService;
  @Mock private AgentTaskStateMachine stateMachine;

  private ObjectMapper objectMapper;
  private AgentReanalysisService service;
  private AgentTaskEntity task;
  private AgentStepEntity step;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    service =
        new AgentReanalysisService(
            taskMapper,
            stepMapper,
            findingMapper,
            actionMapper,
            roundMapper,
            businessLinkMapper,
            snapshotBuilder,
            explanationService,
            stateMachine,
            objectMapper);
    ReflectionTestUtils.setField(service, "maxResearchRounds", 3);
    task = task();
    step = step();
    when(taskMapper.selectById(1L)).thenReturn(task);
    AgentBusinessLinkEntity link = new AgentBusinessLinkEntity();
    link.setBusinessId(200L);
    when(businessLinkMapper.selectByAgentTaskId(1L, "FOLLOW_UP_COLLECTION_TASK"))
        .thenReturn(List.of(link));
    when(roundMapper.insertIfAbsent(any()))
        .thenAnswer(
            invocation -> {
              invocation.<AgentAnalysisRoundEntity>getArgument(0).setId(10L);
              return 1;
            });
    when(roundMapper.markRunning(
            anyLong(), anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(1);
    when(roundMapper.markSucceeded(
            anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
        .thenReturn(1);
    when(stepMapper.selectByTaskId(1L)).thenReturn(List.of(step));
    when(stepMapper.markRunning(anyLong(), any())).thenReturn(1);
    when(stepMapper.markSucceeded(anyLong(), anyString(), anyString(), any())).thenReturn(1);
    when(taskMapper.updateProgress(anyLong(), anyInt(), anyInt(), anyString(), any()))
        .thenReturn(1);
    when(explanationService.explain(any(), any(), anyString(), any()))
        .thenAnswer(invocation -> invocation.getArgument(3, AgentReanalysisExplanation.class));
    when(stateMachine.transition(any(AgentTaskEntity.class), any(AgentTaskStatus.class)))
        .thenAnswer(
            invocation -> {
              AgentTaskEntity value = invocation.getArgument(0);
              value.setStatus(invocation.getArgument(1, AgentTaskStatus.class).getCode());
              value.setVersion(value.getVersion() + 1);
              return value;
            });
  }

  @Test
  void resolvedImageFindingAndImprovedCompletenessLeadToArchiveReadiness() throws Exception {
    Built baseline = built(snapshot(50, "NOT_READY", false), "base");
    Built current = built(snapshot(100, "READY", true), "current");
    when(snapshotBuilder.build(1L, List.of(12L, 200L))).thenReturn(current, current);
    when(snapshotBuilder.build(1L, List.of(12L))).thenReturn(baseline);
    AgentFindingEntity before = finding("MISSING_ROOT_IMAGE", "OPEN");
    AgentFindingEntity after = finding("MISSING_ROOT_IMAGE", "RESOLVED");
    when(findingMapper.selectByTaskId(1L)).thenReturn(List.of(before), List.of(after));

    AgentReanalysisComparisonVO result = service.run(1L);

    assertThat(result.outcome()).isEqualTo("READY_FOR_ARCHIVE");
    assertThat(result.completenessScoreBefore()).isEqualTo(50);
    assertThat(result.completenessScoreAfter()).isEqualTo(100);
    assertThat(result.resolvedFindings()).hasSize(1);
    assertThat(result.conclusion()).contains("不能证明因果");
    verify(findingMapper).resolve(anyLong(), anyString(), any());
    verify(stateMachine).transition(task, AgentTaskStatus.RUNNING);
    verify(taskMapper)
        .updateProgress(eq(1L), eq(5), eq(85), eq("READY_FOR_ARCHIVE_PREPARATION"), any());
  }

  @Test
  void identicalCurrentSnapshotReturnsPersistedResultWithoutAnotherRound() throws Exception {
    AgentReanalysisComparisonVO persisted =
        new AgentReanalysisComparisonVO(
            9L,
            1,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            Map.of(),
            50,
            70,
            "NOT_READY",
            "NOT_READY",
            "已分析",
            "NEED_MORE_FIELD_DATA");
    AgentAnalysisRoundEntity latest = new AgentAnalysisRoundEntity();
    latest.setStatus("SUCCEEDED");
    latest.setCurrentHash("same");
    latest.setChangeSummary(objectMapper.writeValueAsString(persisted));
    when(roundMapper.selectLatestByTaskId(1L)).thenReturn(latest);
    when(snapshotBuilder.build(1L, List.of(12L, 200L)))
        .thenReturn(built(snapshot(70, "NOT_READY", true), "same"));

    AgentReanalysisComparisonVO result = service.run(1L);

    assertThat(result.analysisRoundId()).isEqualTo(9L);
    verify(roundMapper, never()).insertIfAbsent(any());
    verify(stepMapper, never()).markRunning(anyLong(), any());
  }

  @Test
  void remainingResolvableGapCreatesOnlyOneNextPlanStepBelowRoundLimit() throws Exception {
    Built baseline = built(snapshot(50, "NOT_READY", false), "base");
    Built current = built(snapshot(65, "NOT_READY", false), "changed");
    when(snapshotBuilder.build(1L, List.of(12L, 200L))).thenReturn(current, current);
    when(snapshotBuilder.build(1L, List.of(12L))).thenReturn(baseline);
    when(findingMapper.selectByTaskId(1L)).thenReturn(List.of(), List.of());
    when(stepMapper.selectMaxStepNo(1L)).thenReturn(10);
    when(stepMapper.insertIfAbsent(any())).thenReturn(1);

    AgentReanalysisComparisonVO result = service.run(1L);

    assertThat(result.outcome()).isEqualTo("NEED_MORE_FIELD_DATA");
    verify(taskMapper)
        .updateProgress(eq(1L), eq(5), eq(62), eq("GENERATING_NEXT_COLLECTION_PLAN"), any());
    verify(stepMapper).insertIfAbsent(any());
  }

  @Test
  void maximumRoundCompletesWithExplicitInsufficientEvidenceInsteadOfLooping() throws Exception {
    Built current = built(snapshot(65, "NOT_READY", false), "round3");
    AgentAnalysisRoundEntity latest = new AgentAnalysisRoundEntity();
    latest.setRoundNo(2);
    latest.setStatus("SUCCEEDED");
    latest.setCurrentHash("round2");
    latest.setCurrentSnapshot(objectMapper.writeValueAsString(snapshot(60, "NOT_READY", false)));
    when(roundMapper.selectLatestByTaskId(1L)).thenReturn(latest);
    when(snapshotBuilder.build(1L, List.of(12L, 200L))).thenReturn(current, current);
    when(findingMapper.selectByTaskId(1L)).thenReturn(List.of(), List.of());
    when(stateMachine.transition(
            any(AgentTaskEntity.class),
            org.mockito.ArgumentMatchers.eq(AgentTaskStatus.COMPLETED),
            anyString()))
        .thenReturn(task);

    AgentReanalysisComparisonVO result = service.run(1L);

    assertThat(result.outcome()).isEqualTo("INSUFFICIENT_EVIDENCE");
    assertThat(result.conclusion()).contains("证据仍不足");
    verify(taskMapper)
        .updateProgress(eq(1L), eq(4), eq(100), eq("RESEARCH_COMPLETED_WITH_LIMITATIONS"), any());
    verify(stepMapper, never()).insertIfAbsent(any());
  }

  @Test
  void incompleteNewFollowUpStageCreatesNewFindings() throws Exception {
    Built baseline = built(snapshot(50, "NOT_READY", false), "base");
    Built current = built(snapshotWithIncompleteFollowUp(), "incomplete-follow-up");
    when(snapshotBuilder.build(1L, List.of(12L, 200L))).thenReturn(current, current);
    when(snapshotBuilder.build(1L, List.of(12L))).thenReturn(baseline);
    when(findingMapper.selectByTaskId(1L)).thenReturn(List.of(), List.of());
    when(stepMapper.selectMaxStepNo(1L)).thenReturn(10);
    when(stepMapper.insertIfAbsent(any())).thenReturn(1);

    AgentReanalysisComparisonVO result = service.run(1L);

    assertThat(result.outcome()).isEqualTo("NEED_MORE_FIELD_DATA");
    verify(findingMapper, atLeastOnce()).upsert(any());
  }

  private AgentTaskEntity task() {
    AgentTaskEntity value = new AgentTaskEntity();
    value.setId(1L);
    value.setCollectionTaskId(12L);
    value.setStatus("REANALYZING");
    value.setVersion(4);
    return value;
  }

  private AgentStepEntity step() {
    AgentStepEntity value = new AgentStepEntity();
    value.setId(20L);
    value.setAgentTaskId(1L);
    value.setStepType("REANALYZE");
    value.setStatus("PENDING");
    return value;
  }

  private AgentFindingEntity finding(String type, String status) {
    AgentFindingEntity value = new AgentFindingEntity();
    value.setId(30L);
    value.setAgentTaskId(1L);
    value.setFindingType(type);
    value.setSeverity("HIGH");
    value.setTitle("缺少根部图片");
    value.setStatus(status);
    return value;
  }

  private Built built(AgentReanalysisSnapshot snapshot, String hash) throws Exception {
    return new Built(snapshot, objectMapper.writeValueAsString(snapshot), hash);
  }

  private AgentReanalysisSnapshot snapshot(int score, String readiness, boolean complete) {
    Stage first =
        new Stage(
            12L,
            100L,
            LocalDateTime.of(2026, 7, 1, 8, 0),
            1000L,
            "approved",
            Map.of("plantHeight", new BigDecimal("10")),
            Map.of("root", 1),
            1,
            1);
    Stage followUp =
        new Stage(
            200L,
            201L,
            LocalDateTime.of(2026, 7, 10, 8, 0),
            complete ? 2000L : null,
            complete ? "approved" : "draft",
            complete ? Map.of("plantHeight", new BigDecimal("12")) : Map.of(),
            complete ? Map.of("root", 1) : Map.of(),
            complete ? 1 : 0,
            complete ? 1 : 0);
    return new AgentReanalysisSnapshot(
        complete ? List.of(first, followUp) : List.of(first), List.of(), score, readiness);
  }

  private AgentReanalysisSnapshot snapshotWithIncompleteFollowUp() {
    AgentReanalysisSnapshot original = snapshot(50, "NOT_READY", false);
    Stage incomplete =
        new Stage(
            200L,
            201L,
            LocalDateTime.of(2026, 7, 10, 8, 0),
            2000L,
            "draft",
            Map.of(),
            Map.of(),
            0,
            0);
    return new AgentReanalysisSnapshot(
        List.of(original.stages().get(0), incomplete), List.of(), 35, "NOT_READY");
  }
}
