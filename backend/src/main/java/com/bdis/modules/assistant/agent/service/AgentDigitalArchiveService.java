package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveResultVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveStatusVO;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.DigitalLifeNarrationService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentDigitalArchiveService {

  public static final String ENABLE_PUBLIC_TRACE = "ENABLE_PUBLIC_TRACE";
  private static final String WAITING_CONFIRMATION = "WAITING_CONFIRMATION";

  private final AgentTaskMapper taskMapper;
  private final AgentStepMapper stepMapper;
  private final AgentActionMapper actionMapper;
  private final HerbDigitalTwinAgentTaskService agentTaskService;
  private final AgentReanalysisSnapshotBuilder snapshotBuilder;
  private final HerbDigitalLifeArchiveService archiveService;
  private final DigitalLifeNarrationService narrationService;
  private final DigitalLifeIntegrityService integrityService;
  private final ObjectMapper objectMapper;
  private final int minimumCompletenessScore;

  public AgentDigitalArchiveService(
      AgentTaskMapper taskMapper,
      AgentStepMapper stepMapper,
      AgentActionMapper actionMapper,
      HerbDigitalTwinAgentTaskService agentTaskService,
      AgentReanalysisSnapshotBuilder snapshotBuilder,
      HerbDigitalLifeArchiveService archiveService,
      DigitalLifeNarrationService narrationService,
      DigitalLifeIntegrityService integrityService,
      ObjectMapper objectMapper,
      @Value("${assistant.agent.archive-min-completeness-score:85}")
          int minimumCompletenessScore) {
    this.taskMapper = taskMapper;
    this.stepMapper = stepMapper;
    this.actionMapper = actionMapper;
    this.agentTaskService = agentTaskService;
    this.snapshotBuilder = snapshotBuilder;
    this.archiveService = archiveService;
    this.narrationService = narrationService;
    this.integrityService = integrityService;
    this.objectMapper = objectMapper;
    this.minimumCompletenessScore = minimumCompletenessScore;
  }

  @Transactional
  public AgentDigitalArchiveResultVO prepare(Long agentTaskId) {
    agentTaskService.getDetail(agentTaskId);
    AgentTaskEntity task = requireTask(agentTaskId);
    AgentActionEntity existingAction = publicAction(task.getId());
    if (existingAction != null
        && List.of(WAITING_CONFIRMATION, "CONFIRMED", "EXECUTING", "SUCCEEDED", "REJECTED")
            .contains(existingAction.getStatus())) {
      return result(task, integrityService.verify(task.getCollectionTaskId()));
    }
    if (AgentTaskStatus.REANALYZING.getCode().equals(task.getStatus())) {
      agentTaskService.transition(agentTaskId, AgentTaskStatus.RUNNING);
      task = requireTask(agentTaskId);
    }
    if (!AgentTaskStatus.RUNNING.getCode().equals(task.getStatus())) {
      throw new BusinessException("当前 Agent 任务状态不允许准备数字生命档案");
    }

    agentTaskService.updateProgress(agentTaskId, 86, "PREPARING_DIGITAL_ARCHIVE");
    task = requireTask(agentTaskId);
    HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(task.getCollectionTaskId());
    AgentReanalysisSnapshot snapshot =
        snapshotBuilder.build(task.getId(), List.of(task.getCollectionTaskId())).snapshot();
    List<String> violations = prerequisiteViolations(archive, snapshot);
    if (!violations.isEmpty()) {
      throw new BusinessException("数字生命档案尚未满足准备条件：" + String.join("；", violations));
    }

    succeed(step(task.getId(), "PREPARE_DIGITAL_ARCHIVE", "准备数字生命档案"),
        "档案前置条件检查通过", null);
    DigitalLifeNarrationGenerationVO narration = narrationService.generateForTask(task.getCollectionTaskId());
    if (narration.failed() > 0) {
      throw new BusinessException("部分阶段解说生成失败，已停止档案公开流程");
    }
    succeed(step(task.getId(), "GENERATE_STAGE_NARRATIONS", "生成阶段解说"),
        "阶段解说已生成或复用", toJson(narration));

    String summary = archiveSummary(archive, snapshot);
    succeed(step(task.getId(), "GENERATE_ARCHIVE_SUMMARY", "生成档案摘要"), summary, null);

    DigitalLifeIntegrityVO integrity = integrityService.verify(task.getCollectionTaskId());
    if (!integrity.verified()) {
      integrity = integrityService.generate(task.getCollectionTaskId());
    }
    succeed(step(task.getId(), "GENERATE_ARCHIVE_HASH", "生成档案哈希"),
        "已生成或复用档案哈希版本", toJson(integrity));
    integrity = integrityService.verify(task.getCollectionTaskId());
    if (!integrity.verified()) {
      throw new BusinessException("档案完整性校验失败：" + integrity.message());
    }
    succeed(step(task.getId(), "VERIFY_ARCHIVE", "验证档案完整性"),
        "SHA-256 档案完整性校验通过", toJson(integrity));

    AgentDigitalArchiveResultVO prepared = result(task, integrity);
    step(task.getId(), "GENERATE_TRACE_QR", "生成任务级公开二维码");
    AgentStepEntity waiting = step(task.getId(), "WAIT_PUBLIC_CONFIRMATION", "等待公开确认");
    waitStep(waiting);
    createPublicAction(task, waiting, prepared);
    agentTaskService.updateProgress(agentTaskId, 92, "WAITING_PUBLIC_CONFIRMATION");
    agentTaskService.transition(agentTaskId, AgentTaskStatus.WAITING_CONFIRMATION);
    return prepared;
  }

  public AgentDigitalArchiveStatusVO status(Long agentTaskId) {
    agentTaskService.getDetail(agentTaskId);
    AgentTaskEntity task = requireTask(agentTaskId);
    AgentActionEntity action = publicAction(agentTaskId);
    AgentReanalysisSnapshot snapshot =
        snapshotBuilder.build(agentTaskId, List.of(task.getCollectionTaskId())).snapshot();
    HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(task.getCollectionTaskId());
    List<String> violations = prerequisiteViolations(archive, snapshot);
    DigitalLifeIntegrityVO integrity = integrityService.verify(task.getCollectionTaskId());
    boolean waiting = action != null && WAITING_CONFIRMATION.equals(action.getStatus());
    String status = action == null ? (violations.isEmpty() ? "READY" : "NOT_READY") : action.getStatus();
    return new AgentDigitalArchiveStatusVO(
        agentTaskId,
        status,
        statusLabel(status),
        snapshot.completenessScore(),
        minimumCompletenessScore,
        violations.isEmpty(),
        integrity.verified(),
        waiting,
        waiting ? action.getId() : null,
        violations.isEmpty() ? "档案已满足准备条件" : String.join("；", violations));
  }

  public AgentDigitalArchiveResultVO result(Long agentTaskId) {
    agentTaskService.getDetail(agentTaskId);
    AgentTaskEntity task = requireTask(agentTaskId);
    AgentStepEntity prepared = stepMapper.selectByTaskIdAndType(agentTaskId, "PREPARE_DIGITAL_ARCHIVE");
    if (prepared == null || !"SUCCEEDED".equals(prepared.getStatus())) {
      throw new BusinessException("数字生命档案尚未完成准备");
    }
    return result(task, integrityService.verify(task.getCollectionTaskId()));
  }

  List<String> prerequisiteViolations(
      HerbDigitalLifeArchiveVO archive, AgentReanalysisSnapshot snapshot) {
    List<String> violations = new ArrayList<>();
    List<HerbDigitalLifeStageVO> stages = archive.getStages() == null ? List.of() : archive.getStages();
    if (stages.size() < 2) violations.add("有效阶段少于两个");
    if (stages.stream().anyMatch(stage -> stage.getGrowthRecordId() == null))
      violations.add("存在缺少生长记录的阶段");
    if (stages.stream().anyMatch(stage -> !hasMetric(stage.getMetrics())))
      violations.add("存在缺少关键指标的阶段");
    if (stages.stream().anyMatch(stage -> stage.getImages() == null || stage.getImages().isEmpty()))
      violations.add("存在缺少现场图片的阶段");
    if (snapshot.stages().stream()
        .anyMatch(stage -> stage.imageCount() == 0 || stage.recognizedImageCount() < stage.imageCount()))
      violations.add("存在尚未识别或未明确复核的图片");
    if (stages.stream().anyMatch(stage -> !"approved".equals(stage.getAuditStatus())))
      violations.add("存在未审核通过的阶段");
    if (snapshot.findings().stream().anyMatch(finding -> "OPEN".equals(finding.status())
        && List.of("HIGH", "CRITICAL").contains(finding.severity())))
      violations.add("存在高风险未处理发现项");
    if (!timelineValid(stages)) violations.add("阶段时间线顺序不合法");
    if (snapshot.completenessScore() < minimumCompletenessScore)
      violations.add("档案完整度未达到阈值 " + minimumCompletenessScore);
    return violations.stream().distinct().toList();
  }

  private boolean hasMetric(HerbDigitalLifeMetricsVO metrics) {
    return metrics != null
        && (metrics.getPlantHeight() != null
            || metrics.getStemDiameter() != null
            || metrics.getTemperature() != null
            || metrics.getHumidity() != null
            || metrics.getSoilMoisture() != null
            || metrics.getSoilPh() != null
            || metrics.getLight() != null
            || StringUtils.hasText(metrics.getLeafColor())
            || StringUtils.hasText(metrics.getFloweringStatus()));
  }

  private boolean timelineValid(List<HerbDigitalLifeStageVO> stages) {
    LocalDateTime previous = null;
    for (HerbDigitalLifeStageVO stage : stages) {
      if (stage.getCollectedAt() == null) continue;
      if (previous != null && stage.getCollectedAt().isBefore(previous)) return false;
      previous = stage.getCollectedAt();
    }
    return true;
  }

  private AgentDigitalArchiveResultVO result(AgentTaskEntity task, DigitalLifeIntegrityVO integrity) {
    HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(task.getCollectionTaskId());
    AgentReanalysisSnapshot snapshot =
        snapshotBuilder.build(task.getId(), List.of(task.getCollectionTaskId())).snapshot();
    boolean published = Boolean.TRUE.equals(archive.getPublicVisible());
    String traceCode = archive.getTraceCode();
    List<String> limitations = new ArrayList<>();
    if (!published) limitations.add("档案尚未开启公开访问");
    if (snapshot.findings().stream().anyMatch(f -> "OPEN".equals(f.status())))
      limitations.add("仍有非阻断性发现项待持续观察");
    return new AgentDigitalArchiveResultVO(
        task.getId(), archive.getTaskId(), archive.getTaskCode(), traceCode,
        published && StringUtils.hasText(traceCode) ? "/trace/digital-life/" + traceCode : null,
        published && StringUtils.hasText(traceCode)
            ? "/api/trace/digital-life/" + traceCode + "/qrcode" : null,
        value(archive.getStageCount()), value(archive.getImageCount()), snapshot.completenessScore(),
        integrity.verified(), shortHash(integrity.rootHash()), integrity.hashVersion(), published,
        integrity.generatedTime(),
        List.of("连续观测阶段聚合", "阶段科研解说", "SHA-256 完整性校验", "任务级档案摘要"),
        List.copyOf(limitations));
  }

  private void createPublicAction(
      AgentTaskEntity task, AgentStepEntity waiting, AgentDigitalArchiveResultVO prepared) {
    if (publicAction(task.getId()) != null) return;
    AgentActionEntity action = new AgentActionEntity();
    LocalDateTime now = LocalDateTime.now();
    action.setAgentTaskId(task.getId());
    action.setStepId(waiting.getId());
    action.setActionType(ENABLE_PUBLIC_TRACE);
    action.setTargetType("COLLECTION_TASK");
    action.setTargetId(task.getCollectionTaskId());
    action.setActionName("开启任务级公开数字生命档案");
    action.setActionDescription("确认后将生成公开二维码并开启任务级公开溯源访问");
    action.setPayloadJson(toJson(prepared));
    action.setRiskLevel("HIGH");
    action.setNeedConfirm(1);
    action.setStatus(WAITING_CONFIRMATION);
    action.setRequestedTime(now);
    action.setVersion(0);
    action.setCreateTime(now);
    action.setUpdateTime(now);
    actionMapper.insert(action);
  }

  private AgentActionEntity publicAction(Long agentTaskId) {
    return actionMapper.selectByTaskId(agentTaskId).stream()
        .filter(action -> ENABLE_PUBLIC_TRACE.equals(action.getActionType()))
        .findFirst().orElse(null);
  }

  private AgentStepEntity step(Long taskId, String type, String name) {
    AgentStepEntity existing = stepMapper.selectByTaskIdAndType(taskId, type);
    if (existing != null) return existing;
    LocalDateTime now = LocalDateTime.now();
    AgentStepEntity step = new AgentStepEntity();
    step.setAgentTaskId(taskId);
    step.setStepNo(value(stepMapper.selectMaxStepNo(taskId)) + 1);
    step.setStepType(type);
    step.setStepName(name);
    step.setStatus("PENDING");
    step.setRetryCount(0);
    step.setMaxRetryCount(0);
    step.setCreateTime(now);
    step.setUpdateTime(now);
    if (stepMapper.insertIfAbsent(step) == 0) {
      return stepMapper.selectByTaskIdAndType(taskId, type);
    }
    return step;
  }

  private void succeed(AgentStepEntity step, String summary, String outputJson) {
    if (step == null || "SUCCEEDED".equals(step.getStatus())) return;
    LocalDateTime now = LocalDateTime.now();
    if ("PENDING".equals(step.getStatus()) && stepMapper.markRunning(step.getId(), now) == 0)
      throw new ResourceConflictException("档案步骤已被其他请求推进");
    if (stepMapper.markSucceeded(step.getId(), summary, outputJson, LocalDateTime.now()) == 0)
      throw new ResourceConflictException("档案步骤完成状态更新冲突");
  }

  private void waitStep(AgentStepEntity step) {
    if (step != null && "PENDING".equals(step.getStatus())
        && stepMapper.markWaiting(step.getId(), LocalDateTime.now()) == 0)
      throw new ResourceConflictException("公开确认等待步骤状态更新冲突");
  }

  private AgentTaskEntity requireTask(Long id) {
    AgentTaskEntity task = taskMapper.selectById(id);
    if (task == null) throw new BusinessException("Agent 任务不存在");
    return task;
  }

  private String archiveSummary(HerbDigitalLifeArchiveVO archive, AgentReanalysisSnapshot snapshot) {
    return "连续观测周期共 " + value(archive.getStageCount()) + " 个阶段、"
        + value(archive.getImageCount()) + " 张现场图片，档案完整度 "
        + snapshot.completenessScore() + " 分；摘要仅陈述已记录证据，不推断功效、疗效或因果关系。";
  }

  private String statusLabel(String status) {
    return switch (status) {
      case "READY" -> "可准备";
      case "NOT_READY" -> "条件不足";
      case WAITING_CONFIRMATION -> "等待公开确认";
      case "SUCCEEDED" -> "已公开";
      case "REJECTED" -> "已完成内部档案";
      case "FAILED" -> "公开失败";
      default -> status;
    };
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("数字生命档案结果序列化失败");
    }
  }

  private String shortHash(String rootHash) {
    return !StringUtils.hasText(rootHash) ? null : rootHash.substring(0, Math.min(12, rootHash.length()));
  }

  private int value(Integer value) { return value == null ? 0 : value; }
}
