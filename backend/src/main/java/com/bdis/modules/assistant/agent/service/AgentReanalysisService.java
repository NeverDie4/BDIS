package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisExplanation;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Built;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.MetricChange;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Stage;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
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
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.bdis.modules.assistant.agent.vo.AgentReanalysisComparisonVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentReanalysisService {

  private static final String FOLLOW_UP_RELATION = "FOLLOW_UP_COLLECTION_TASK";

  private final AgentTaskMapper taskMapper;
  private final AgentStepMapper stepMapper;
  private final AgentFindingMapper findingMapper;
  private final AgentActionMapper actionMapper;
  private final AgentAnalysisRoundMapper roundMapper;
  private final AgentBusinessLinkMapper businessLinkMapper;
  private final AgentReanalysisSnapshotBuilder snapshotBuilder;
  private final AgentReanalysisExplanationService explanationService;
  private final AgentTaskStateMachine stateMachine;
  private final ObjectMapper objectMapper;

  @Value("${assistant.agent.max-research-rounds:3}")
  private int maxResearchRounds;

  public AgentReanalysisService(
      AgentTaskMapper taskMapper,
      AgentStepMapper stepMapper,
      AgentFindingMapper findingMapper,
      AgentActionMapper actionMapper,
      AgentAnalysisRoundMapper roundMapper,
      AgentBusinessLinkMapper businessLinkMapper,
      AgentReanalysisSnapshotBuilder snapshotBuilder,
      AgentReanalysisExplanationService explanationService,
      AgentTaskStateMachine stateMachine,
      ObjectMapper objectMapper) {
    this.taskMapper = taskMapper;
    this.stepMapper = stepMapper;
    this.findingMapper = findingMapper;
    this.actionMapper = actionMapper;
    this.roundMapper = roundMapper;
    this.businessLinkMapper = businessLinkMapper;
    this.snapshotBuilder = snapshotBuilder;
    this.explanationService = explanationService;
    this.stateMachine = stateMachine;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public AgentReanalysisComparisonVO run(Long agentTaskId) {
    AgentTaskEntity task = taskMapper.selectById(agentTaskId);
    if (task == null || !AgentTaskStatus.REANALYZING.getCode().equals(task.getStatus())) {
      throw new BusinessException("Agent 任务当前不在重新分析状态");
    }
    List<Long> taskIds = linkedTaskIds(task);
    AgentAnalysisRoundEntity latest = roundMapper.selectLatestByTaskId(agentTaskId);
    Built factualCurrent = snapshotBuilder.build(agentTaskId, taskIds);
    if (latest != null
        && "SUCCEEDED".equals(latest.getStatus())
        && Objects.equals(latest.getCurrentHash(), factualCurrent.sha256())) {
      return read(latest.getChangeSummary(), AgentReanalysisComparisonVO.class);
    }

    int roundNo = latest == null ? 1 : latest.getRoundNo() + 1;
    Built baseline =
        latest == null
            ? snapshotBuilder.build(agentTaskId, List.of(task.getCollectionTaskId()))
            : builtFromPersisted(latest.getCurrentSnapshot(), latest.getCurrentHash());
    AgentAnalysisRoundEntity round = createRound(task, taskIds, roundNo);
    LocalDateTime now = LocalDateTime.now();
    if (roundMapper.markRunning(
            round.getId(),
            baseline.canonicalJson(),
            baseline.sha256(),
            factualCurrent.canonicalJson(),
            factualCurrent.sha256(),
            now)
        == 0) {
      AgentAnalysisRoundEntity existing = roundMapper.selectByTaskAndRound(agentTaskId, roundNo);
      if (existing != null && "SUCCEEDED".equals(existing.getStatus())) {
        return read(existing.getChangeSummary(), AgentReanalysisComparisonVO.class);
      }
      throw new ResourceConflictException("该分析轮次已由其他线程执行");
    }

    AgentStepEntity step = beginReanalyzeStep(agentTaskId, baseline.canonicalJson());
    List<AgentFindingEntity> beforeFindings = findingMapper.selectByTaskId(agentTaskId);
    updateFindings(
        agentTaskId,
        beforeFindings,
        factualCurrent.snapshot(),
        step.getId(),
        factualCurrent.canonicalJson());
    Built current = snapshotBuilder.build(agentTaskId, taskIds);
    List<AgentFindingEntity> afterFindings = findingMapper.selectByTaskId(agentTaskId);
    String outcome = decide(current.snapshot(), roundNo);
    String ruleConclusion = conclusion(baseline.snapshot(), current.snapshot(), outcome);
    AgentReanalysisExplanation explanation =
        explanationService.explain(
            baseline.snapshot(),
            current.snapshot(),
            outcome,
            new AgentReanalysisExplanation(
                ruleConclusion,
                List.of(
                    "档案完整度由 "
                        + baseline.snapshot().completenessScore()
                        + "% 变化为 "
                        + current.snapshot().completenessScore()
                        + "%"),
                List.of("同期指标与图片变化不能证明因果关系"),
                ruleNextSuggestion(outcome)));
    String conclusion =
        explanation.changeSummary()
            + " 尚不能确认："
            + String.join("；", explanation.uncertainties())
            + " 下一步："
            + explanation.nextSuggestion();
    AgentReanalysisComparisonVO comparison =
        compare(
            round.getId(),
            roundNo,
            baseline.snapshot(),
            current.snapshot(),
            beforeFindings,
            afterFindings,
            conclusion,
            outcome);
    String comparisonJson = write(comparison);
    if (roundMapper.markSucceeded(
            round.getId(),
            current.canonicalJson(),
            current.sha256(),
            comparisonJson,
            conclusion,
            outcome,
            LocalDateTime.now())
        == 0) {
      throw new ResourceConflictException("分析轮次完成状态更新冲突");
    }
    if (stepMapper.markSucceeded(step.getId(), "补采前后证据比较完成", comparisonJson, LocalDateTime.now())
        == 0) {
      throw new ResourceConflictException("REANALYZE 步骤完成状态更新冲突");
    }
    advanceWorkflow(task, outcome, roundNo);
    return comparison;
  }

  private AgentAnalysisRoundEntity createRound(
      AgentTaskEntity task, List<Long> taskIds, int roundNo) {
    LocalDateTime now = LocalDateTime.now();
    AgentAnalysisRoundEntity round = new AgentAnalysisRoundEntity();
    round.setAgentTaskId(task.getId());
    round.setRoundNo(roundNo);
    round.setRoundType("FOLLOW_UP");
    round.setSourceCollectionTaskId(task.getCollectionTaskId());
    round.setFollowUpCollectionTaskId(taskIds.get(taskIds.size() - 1));
    round.setStatus("CREATED");
    round.setCreateTime(now);
    round.setUpdateTime(now);
    if (roundMapper.insertIfAbsent(round) == 0) {
      round = roundMapper.selectByTaskAndRound(task.getId(), roundNo);
    }
    if (round == null || round.getId() == null) {
      throw new ResourceConflictException("分析轮次创建冲突");
    }
    return round;
  }

  private List<Long> linkedTaskIds(AgentTaskEntity task) {
    List<Long> ids = new ArrayList<>();
    ids.add(task.getCollectionTaskId());
    businessLinkMapper.selectByAgentTaskId(task.getId(), FOLLOW_UP_RELATION).stream()
        .map(AgentBusinessLinkEntity::getBusinessId)
        .filter(Objects::nonNull)
        .forEach(ids::add);
    return ids.stream().distinct().toList();
  }

  private AgentStepEntity beginReanalyzeStep(Long taskId, String baselineJson) {
    AgentStepEntity step =
        stepMapper.selectByTaskId(taskId).stream()
            .filter(value -> "REANALYZE".equals(value.getStepType()))
            .filter(value -> "PENDING".equals(value.getStatus()))
            .reduce((left, right) -> right)
            .orElseThrow(() -> new BusinessException("缺少待执行的 REANALYZE 步骤"));
    if (stepMapper.markRunning(step.getId(), LocalDateTime.now()) == 0) {
      throw new ResourceConflictException("REANALYZE 步骤已被其他线程执行");
    }
    step.setStatus("RUNNING");
    step.setInputSnapshot(baselineJson);
    return step;
  }

  private void updateFindings(
      Long agentTaskId,
      List<AgentFindingEntity> findings,
      AgentReanalysisSnapshot current,
      Long stepId,
      String evidenceJson) {
    LocalDateTime now = LocalDateTime.now();
    for (AgentFindingEntity finding : findings) {
      if ("IGNORED".equals(finding.getStatus())) {
        continue;
      }
      if (resolved(finding.getFindingType(), current)) {
        findingMapper.resolve(finding.getId(), evidenceJson, now);
      } else {
        findingMapper.updateOpenEvidence(finding.getId(), evidenceJson, now);
      }
    }
    Stage latest = latestStage(current);
    if (latest == null) {
      upsertFinding(
          agentTaskId, stepId, "MISSING_GROWTH_RECORD", "HIGH", "补采后仍缺少有效阶段", evidenceJson);
      return;
    }
    if (latest.metrics().isEmpty()) {
      upsertFinding(agentTaskId, stepId, "MISSING_METRIC", "HIGH", "补采阶段缺少指标", evidenceJson);
    }
    if (latest.imageCount() == 0) {
      upsertFinding(agentTaskId, stepId, "MISSING_IMAGE", "HIGH", "补采阶段缺少现场图片", evidenceJson);
    } else if (latest.recognizedImageCount() < latest.imageCount()) {
      upsertFinding(
          agentTaskId, stepId, "UNRECOGNIZED_IMAGE", "MEDIUM", "补采图片尚未全部识别", evidenceJson);
    }
    if (!"approved".equals(latest.reviewStatus())) {
      upsertFinding(agentTaskId, stepId, "REVIEW_PENDING", "MEDIUM", "补采阶段尚待审核", evidenceJson);
    }
  }

  private boolean resolved(String type, AgentReanalysisSnapshot current) {
    Stage latest = latestStage(current);
    if (latest == null) {
      return false;
    }
    return switch (type) {
      case "MISSING_ROOT_IMAGE" -> latest.imageTypeCounts().getOrDefault("root", 0) > 0;
      case "MISSING_LEAF_IMAGE" -> latest.imageTypeCounts().getOrDefault("leaf", 0) > 0;
      case "MISSING_WHOLE_PLANT_IMAGE" ->
          latest.imageTypeCounts().getOrDefault("whole_plant", 0) > 0;
      case "MISSING_ENVIRONMENT_IMAGE" ->
          latest.imageTypeCounts().getOrDefault("environment", 0) > 0;
      case "MISSING_IMAGE", "MISSING_IMAGE_TYPE" -> latest.imageCount() > 0;
      case "UNRECOGNIZED_IMAGE", "LOW_CONFIDENCE_RECOGNITION" ->
          latest.imageCount() > 0 && latest.recognizedImageCount() == latest.imageCount();
      case "MISSING_METRIC" -> !latest.metrics().isEmpty();
      case "REVIEW_PENDING", "REVIEW_REJECTED" -> "approved".equals(latest.reviewStatus());
      default -> false;
    };
  }

  private void upsertFinding(
      Long agentTaskId,
      Long stepId,
      String type,
      String severity,
      String title,
      String evidenceJson) {
    LocalDateTime now = LocalDateTime.now();
    AgentFindingEntity finding = new AgentFindingEntity();
    finding.setAgentTaskId(agentTaskId);
    finding.setStepId(stepId);
    finding.setFindingType(type);
    finding.setSeverity(severity);
    finding.setTargetType("REANALYSIS");
    finding.setTargetId(stepId);
    finding.setTitle(title);
    finding.setDescription(title);
    finding.setEvidenceJson(evidenceJson);
    finding.setSuggestion("根据真实业务流程补充或审核该证据，不自动修改原始数据。");
    finding.setStatus("OPEN");
    finding.setCreateTime(now);
    finding.setUpdateTime(now);
    findingMapper.upsert(finding);
  }

  private AgentReanalysisComparisonVO compare(
      Long roundId,
      int roundNo,
      AgentReanalysisSnapshot baseline,
      AgentReanalysisSnapshot current,
      List<AgentFindingEntity> beforeFindings,
      List<AgentFindingEntity> afterFindings,
      String conclusion,
      String outcome) {
    Set<Long> beforeIds =
        beforeFindings.stream()
            .map(AgentFindingEntity::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, AgentFindingEntity> beforeById =
        beforeFindings.stream()
            .filter(value -> value.getId() != null)
            .collect(Collectors.toMap(AgentFindingEntity::getId, Function.identity()));
    List<AgentFindingVO> resolved =
        afterFindings.stream()
            .filter(value -> "RESOLVED".equals(value.getStatus()))
            .filter(
                value -> {
                  AgentFindingEntity before = beforeById.get(value.getId());
                  return before != null && !"RESOLVED".equals(before.getStatus());
                })
            .map(this::toFinding)
            .toList();
    List<AgentFindingVO> remaining =
        afterFindings.stream()
            .filter(value -> List.of("OPEN", "ACKNOWLEDGED").contains(value.getStatus()))
            .map(this::toFinding)
            .toList();
    List<AgentFindingVO> added =
        afterFindings.stream()
            .filter(value -> value.getId() == null || !beforeIds.contains(value.getId()))
            .map(this::toFinding)
            .toList();
    return new AgentReanalysisComparisonVO(
        roundId,
        roundNo,
        resolved,
        remaining,
        added,
        metricChanges(baseline, current),
        countChanges(baseline, current, false),
        countChanges(baseline, current, true),
        baseline.completenessScore(),
        current.completenessScore(),
        baseline.archiveReadiness(),
        current.archiveReadiness(),
        conclusion,
        outcome);
  }

  private List<MetricChange> metricChanges(
      AgentReanalysisSnapshot baseline, AgentReanalysisSnapshot current) {
    Stage before = latestStage(baseline);
    Stage after = latestStage(current);
    if (before == null || after == null) {
      return List.of();
    }
    Set<String> codes = new java.util.TreeSet<>();
    codes.addAll(before.metrics().keySet());
    codes.addAll(after.metrics().keySet());
    return codes.stream()
        .map(
            code -> {
              BigDecimal left = decimal(before.metrics().get(code));
              BigDecimal right = decimal(after.metrics().get(code));
              BigDecimal difference = left == null || right == null ? null : right.subtract(left);
              return new MetricChange(code, left, right, difference);
            })
        .toList();
  }

  private Map<String, Integer> countChanges(
      AgentReanalysisSnapshot baseline, AgentReanalysisSnapshot current, boolean recognition) {
    Map<String, Integer> before = aggregateCounts(baseline, recognition);
    Map<String, Integer> after = aggregateCounts(current, recognition);
    Set<String> keys = new java.util.TreeSet<>();
    keys.addAll(before.keySet());
    keys.addAll(after.keySet());
    Map<String, Integer> changes = new LinkedHashMap<>();
    keys.forEach(key -> changes.put(key, after.getOrDefault(key, 0) - before.getOrDefault(key, 0)));
    return Map.copyOf(changes);
  }

  private Map<String, Integer> aggregateCounts(
      AgentReanalysisSnapshot snapshot, boolean recognition) {
    Map<String, Integer> values = new LinkedHashMap<>();
    if (recognition) {
      values.put(
          "recognized", snapshot.stages().stream().mapToInt(Stage::recognizedImageCount).sum());
      values.put("total", snapshot.stages().stream().mapToInt(Stage::imageCount).sum());
      return values;
    }
    snapshot
        .stages()
        .forEach(
            stage ->
                stage
                    .imageTypeCounts()
                    .forEach((type, count) -> values.merge(type, count, Integer::sum)));
    return values;
  }

  private String decide(AgentReanalysisSnapshot current, int roundNo) {
    if ("READY".equals(current.archiveReadiness())) {
      return "READY_FOR_ARCHIVE";
    }
    if (current.stages().stream()
        .anyMatch(stage -> Set.of("submitted", "reviewing").contains(stage.reviewStatus()))) {
      return "NEED_MANUAL_REVIEW";
    }
    return roundNo >= Math.max(1, maxResearchRounds)
        ? "INSUFFICIENT_EVIDENCE"
        : "NEED_MORE_FIELD_DATA";
  }

  private String conclusion(
      AgentReanalysisSnapshot baseline, AgentReanalysisSnapshot current, String outcome) {
    String next =
        switch (outcome) {
          case "READY_FOR_ARCHIVE" -> "现有已审核证据达到档案准备条件。";
          case "NEED_MANUAL_REVIEW" -> "仍需审核员或专家确认补采阶段。";
          case "NEED_MORE_FIELD_DATA" -> "仍有可通过有限补采完善的证据缺口。";
          default -> "已达到最大研究轮次，现有证据仍不足。";
        };
    return "补采后档案完整度由 "
        + baseline.completenessScore()
        + "% 变化为 "
        + current.completenessScore()
        + "%。"
        + next
        + " 指标与图片的同步变化仅构成观察事实，不能据此确认因果关系、病害或产量影响。";
  }

  private String ruleNextSuggestion(String outcome) {
    return switch (outcome) {
      case "READY_FOR_ARCHIVE" -> "进入可信数字生命档案准备阶段";
      case "NEED_MANUAL_REVIEW" -> "等待有权限的审核员或专家确认";
      case "NEED_MORE_FIELD_DATA" -> "在有限轮次内生成下一轮复测方案";
      default -> "以证据不足结论结束本次研究，不继续自动补采";
    };
  }

  private void advanceWorkflow(AgentTaskEntity task, String outcome, int roundNo) {
    switch (outcome) {
      case "READY_FOR_ARCHIVE" -> {
        AgentTaskEntity running = stateMachine.transition(task, AgentTaskStatus.RUNNING);
        updateProgress(running, 85, "READY_FOR_ARCHIVE_PREPARATION");
      }
      case "NEED_MORE_FIELD_DATA" -> {
        AgentTaskEntity running = stateMachine.transition(task, AgentTaskStatus.RUNNING);
        updateProgress(running, 62, "GENERATING_NEXT_COLLECTION_PLAN");
        createPendingStep(task.getId(), "GENERATE_COLLECTION_PLAN", "生成下一轮有限复测方案");
      }
      case "NEED_MANUAL_REVIEW" -> {
        AgentTaskEntity waiting =
            stateMachine.transition(task, AgentTaskStatus.WAITING_CONFIRMATION);
        updateProgress(waiting, 80, "WAITING_MANUAL_REVIEW");
        createManualReviewAction(task.getId(), roundNo);
      }
      case "INSUFFICIENT_EVIDENCE" -> {
        updateProgress(task, 100, "RESEARCH_COMPLETED_WITH_LIMITATIONS");
        stateMachine.transition(
            task, AgentTaskStatus.COMPLETED, "已达到最大研究轮次；任务正常结束，但现有证据不足以形成可信归档结论。");
      }
      default -> throw new BusinessException("不支持的重新分析结果：" + outcome);
    }
  }

  private void updateProgress(AgentTaskEntity task, int progress, String phase) {
    LocalDateTime now = LocalDateTime.now();
    if (taskMapper.updateProgress(task.getId(), task.getVersion(), progress, phase, now) == 0) {
      throw new ResourceConflictException("重新分析进度更新冲突");
    }
    task.setVersion(task.getVersion() + 1);
    task.setProgressPercent(progress);
    task.setCurrentPhase(phase);
  }

  private void createPendingStep(Long taskId, String type, String name) {
    LocalDateTime now = LocalDateTime.now();
    AgentStepEntity step = new AgentStepEntity();
    step.setAgentTaskId(taskId);
    step.setStepNo(value(stepMapper.selectMaxStepNo(taskId)) + 1);
    step.setStepType(type);
    step.setStepName(name);
    step.setDescription(name);
    step.setStatus("PENDING");
    step.setRetryCount(0);
    step.setMaxRetryCount(0);
    step.setCreateTime(now);
    step.setUpdateTime(now);
    stepMapper.insertIfAbsent(step);
  }

  private void createManualReviewAction(Long taskId, int roundNo) {
    LocalDateTime now = LocalDateTime.now();
    AgentActionEntity action = new AgentActionEntity();
    action.setAgentTaskId(taskId);
    action.setActionType("CONFIRM_REANALYSIS_MANUAL_REVIEW");
    action.setTargetType("ANALYSIS_ROUND");
    action.setTargetId((long) roundNo);
    action.setActionName("确认补采重新分析的人工审核结论");
    action.setActionDescription("模型不决定审核结果，请由有权限的审核员或专家确认。 ");
    action.setRiskLevel("HIGH");
    action.setNeedConfirm(1);
    action.setStatus("WAITING_CONFIRMATION");
    action.setRequestedTime(now);
    action.setVersion(0);
    action.setCreateTime(now);
    action.setUpdateTime(now);
    actionMapper.insert(action);
  }

  private Stage latestStage(AgentReanalysisSnapshot snapshot) {
    return snapshot.stages().isEmpty() ? null : snapshot.stages().get(snapshot.stages().size() - 1);
  }

  private BigDecimal decimal(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return new BigDecimal(number.toString());
    }
    return null;
  }

  private AgentFindingVO toFinding(AgentFindingEntity entity) {
    AgentFindingVO value = new AgentFindingVO();
    value.setId(entity.getId());
    value.setStepId(entity.getStepId());
    value.setFindingType(entity.getFindingType());
    value.setSeverity(entity.getSeverity());
    value.setTargetType(entity.getTargetType());
    value.setTargetId(entity.getTargetId());
    value.setTitle(entity.getTitle());
    value.setDescription(entity.getDescription());
    value.setSuggestion(entity.getSuggestion());
    value.setStatus(entity.getStatus());
    value.setResolvedTime(entity.getResolvedTime());
    value.setCreateTime(entity.getCreateTime());
    return value;
  }

  private Built builtFromPersisted(String json, String hash) {
    return new Built(read(json, AgentReanalysisSnapshot.class), json, hash);
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("重新分析结果序列化失败");
    }
  }

  private <T> T read(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("重新分析持久化结果格式无效");
    }
  }

  private int value(Integer value) {
    return value == null ? 0 : value;
  }
}
