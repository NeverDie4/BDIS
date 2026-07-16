package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.entity.AgentFindingEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.AgentCompletenessDiagnosisService.DiagnosisInput;
import com.bdis.modules.assistant.agent.service.AgentCompletenessDiagnosisService.DiagnosisOutcome;
import com.bdis.modules.assistant.agent.service.AgentCompletenessDiagnosisService.FindingDraft;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutionContext;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutor;
import com.bdis.modules.assistant.agent.tool.AgentToolResult;
import com.bdis.modules.assistant.agent.tool.CollectionTaskReadTool;
import com.bdis.modules.assistant.agent.tool.DigitalLifeArchiveReadTool;
import com.bdis.modules.assistant.agent.tool.GrowthRecordReadTool;
import com.bdis.modules.assistant.agent.tool.RecognitionReadTool;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.vo.AgentCompletenessReportVO;
import com.bdis.modules.assistant.agent.vo.AgentDiagnosisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentWorkflowStartVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class HerbDigitalTwinAgentWorkflowRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HerbDigitalTwinAgentWorkflowRunner.class);
    private static final List<StepDefinition> DIAGNOSIS_STEPS =
            List.of(
                    new StepDefinition(1, "LOAD_CONTEXT", "装载任务上下文", "读取采集任务基础上下文"),
                    new StepDefinition(2, "INSPECT_TASK", "检查采集任务", "批量读取观测批次"),
                    new StepDefinition(3, "ANALYZE_TIMELINE", "检查观测时间线", "读取连续观测生长记录"),
                    new StepDefinition(4, "ANALYZE_EVIDENCE", "检查证据完整性", "检查图片、识别、审核与档案状态"));

    private final AgentTaskMapper taskMapper;
    private final AgentStepMapper stepMapper;
    private final AgentFindingMapper findingMapper;
    private final HerbDigitalTwinAgentTaskService taskService;
    private final AgentTaskStateMachine stateMachine;
    private final AgentToolExecutor toolExecutor;
    private final CollectionTaskReadTool collectionTaskTool;
    private final GrowthRecordReadTool growthRecordTool;
    private final RecognitionReadTool recognitionTool;
    private final DigitalLifeArchiveReadTool archiveTool;
    private final AgentCompletenessDiagnosisService diagnosisService;
    private final HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner;
    private final AgentCollectionPlanService collectionPlanService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, ReentrantLock> taskLocks = new ConcurrentHashMap<>();

    public HerbDigitalTwinAgentWorkflowRunner(
            AgentTaskMapper taskMapper,
            AgentStepMapper stepMapper,
            AgentFindingMapper findingMapper,
            HerbDigitalTwinAgentTaskService taskService,
            AgentTaskStateMachine stateMachine,
            AgentToolExecutor toolExecutor,
            CollectionTaskReadTool collectionTaskTool,
            GrowthRecordReadTool growthRecordTool,
            RecognitionReadTool recognitionTool,
            DigitalLifeArchiveReadTool archiveTool,
            AgentCompletenessDiagnosisService diagnosisService,
            HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner,
            AgentCollectionPlanService collectionPlanService,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.findingMapper = findingMapper;
        this.taskService = taskService;
        this.stateMachine = stateMachine;
        this.toolExecutor = toolExecutor;
        this.collectionTaskTool = collectionTaskTool;
        this.growthRecordTool = growthRecordTool;
        this.recognitionTool = recognitionTool;
        this.archiveTool = archiveTool;
        this.diagnosisService = diagnosisService;
        this.evidenceAnalysisRunner = evidenceAnalysisRunner;
        this.collectionPlanService = collectionPlanService;
        this.objectMapper = objectMapper;
    }

    public AgentWorkflowStartVO start(Long agentTaskId) {
        taskService.getDetail(agentTaskId);
        ReentrantLock lock = taskLocks.computeIfAbsent(agentTaskId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return startLocked(agentTaskId);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                taskLocks.remove(agentTaskId, lock);
            }
        }
    }

    public AgentDiagnosisResponseVO getDiagnosis(Long agentTaskId) {
        taskService.getDetail(agentTaskId);
        AgentStepEntity step = stepMapper.selectByTaskIdAndType(agentTaskId, "ANALYZE_EVIDENCE");
        if (step == null || !StringUtils.hasText(step.getOutputJson())) {
            String status = step == null ? "NOT_STARTED" : step.getStatus();
            return new AgentDiagnosisResponseVO(false, status, "完整性诊断尚未生成，请先启动 Agent 任务。", null);
        }
        try {
            AgentCompletenessReportVO report =
                    objectMapper.readValue(step.getOutputJson(), AgentCompletenessReportVO.class);
            return new AgentDiagnosisResponseVO(true, "AVAILABLE", "完整性诊断已生成。", report);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Agent 完整性诊断报告读取失败");
        }
    }

    private AgentWorkflowStartVO startLocked(Long agentTaskId) {
        AgentTaskEntity task = requireTask(agentTaskId);
        AgentTaskStatus current = AgentTaskStatus.fromCode(task.getStatus());
        if (current != AgentTaskStatus.CREATED) {
            if (current == AgentTaskStatus.RUNNING
                    && "COMPLETENESS_DIAGNOSIS_COMPLETED".equals(task.getCurrentPhase())) {
                evidenceAnalysisRunner.run(task.getId());
                collectionPlanService.generate(task.getId(), false);
            } else if (current == AgentTaskStatus.RUNNING
                    && "EVIDENCE_ANALYSIS_COMPLETED".equals(task.getCurrentPhase())) {
                collectionPlanService.generate(task.getId(), false);
            }
            return alreadyStarted(task);
        }
        try {
            taskService.transition(task.getId(), AgentTaskStatus.PLANNING);
            task = requireTask(task.getId());
        } catch (ResourceConflictException exception) {
            return alreadyStarted(requireTask(agentTaskId));
        }
        ensureDiagnosisSteps(task.getId());
        stateMachine.transition(task, AgentTaskStatus.RUNNING);
        Long collectionTaskId = task.getCollectionTaskId();

        AgentStepEntity currentStep = null;
        try {
            Map<String, AgentStepEntity> steps = loadDiagnosisSteps(task.getId());

            currentStep = steps.get("LOAD_CONTEXT");
            beginStep(task, currentStep, "LOADING_CONTEXT", 0);
            AgentToolExecutionContext loadContext = context(task, currentStep);
            CollectionTaskToolData.Overview taskOverview =
                    invoke(
                            CollectionTaskReadTool.OVERVIEW,
                            loadContext,
                            "装载采集任务 " + collectionTaskId,
                            () ->
                                    collectionTaskTool.getCollectionTaskOverview(
                                            collectionTaskId, loadContext));
            succeedStep(task, currentStep, "采集任务上下文装载完成", taskOverview, 10);

            currentStep = steps.get("INSPECT_TASK");
            beginStep(task, currentStep, "INSPECTING_TASK", 10);
            AgentToolExecutionContext inspectContext = context(task, currentStep);
            CollectionTaskToolData.BatchList batches =
                    invoke(
                            CollectionTaskReadTool.LIST_BATCHES,
                            inspectContext,
                            "检查任务下全部采集批次",
                            () ->
                                    collectionTaskTool.listCollectionTaskBatches(
                                            collectionTaskId, inspectContext));
            succeedStep(task, currentStep, "采集任务与批次检查完成", batches, 20);

            currentStep = steps.get("ANALYZE_TIMELINE");
            beginStep(task, currentStep, "CHECKING_TIMELINE", 20);
            AgentToolExecutionContext timelineContext = context(task, currentStep);
            GrowthRecordToolData.RecordList growthRecords =
                    invoke(
                            GrowthRecordReadTool.LIST_BY_TASK,
                            timelineContext,
                            "检查连续观测生长记录时间线",
                            () ->
                                    growthRecordTool.listTaskGrowthRecords(
                                            collectionTaskId, timelineContext));
            succeedStep(task, currentStep, "观测时间线检查完成", growthRecords, 28);

            currentStep = steps.get("ANALYZE_EVIDENCE");
            beginStep(task, currentStep, "CHECKING_EVIDENCE", 28);
            AgentToolExecutionContext evidenceContext = context(task, currentStep);
            RecognitionToolData.TaskImageList unrecognized =
                    invoke(
                            RecognitionReadTool.UNRECOGNIZED,
                            evidenceContext,
                            "检查未识别现场图片",
                            () ->
                                    recognitionTool.listUnrecognizedImages(
                                            collectionTaskId, evidenceContext));
            RecognitionToolData.TaskImageList lowConfidence =
                    invoke(
                            RecognitionReadTool.LOW_CONFIDENCE,
                            evidenceContext,
                            "检查低置信度现场图片",
                            () ->
                                    recognitionTool.listLowConfidenceImages(
                                            collectionTaskId, evidenceContext));
            DigitalLifeArchiveToolData.Overview archiveOverview =
                    invoke(
                            DigitalLifeArchiveReadTool.OVERVIEW,
                            evidenceContext,
                            "检查数字生命档案概览",
                            () ->
                                    archiveTool.getDigitalLifeArchiveOverview(
                                            collectionTaskId, evidenceContext));
            DigitalLifeArchiveToolData.StageList stages =
                    invoke(
                            DigitalLifeArchiveReadTool.STAGES,
                            evidenceContext,
                            "检查数字生命阶段证据",
                            () ->
                                    archiveTool.getDigitalLifeStages(
                                            collectionTaskId, evidenceContext));
            DigitalLifeArchiveToolData.IntegrityStatus integrity =
                    invoke(
                            DigitalLifeArchiveReadTool.INTEGRITY,
                            evidenceContext,
                            "检查现有档案哈希状态",
                            () ->
                                    archiveTool.getArchiveIntegrityStatus(
                                            collectionTaskId, evidenceContext));

            DiagnosisOutcome outcome =
                    diagnosisService.diagnose(
                            new DiagnosisInput(
                                    task.getId(),
                                    taskOverview,
                                    batches,
                                    growthRecords,
                                    unrecognized,
                                    lowConfidence,
                                    archiveOverview,
                                    stages,
                                    integrity));
            persistFindings(task.getId(), currentStep.getId(), outcome.findings());
            AgentCompletenessReportVO report =
                    outcome.toReport(taskService.listFindings(task.getId()));
            succeedStep(task, currentStep, outcome.summary(), report, 35);
                taskService.updateProgress(task.getId(), 35, "COMPLETENESS_DIAGNOSIS_COMPLETED");
                evidenceAnalysisRunner.run(task.getId());
                collectionPlanService.generate(task.getId(), false);
                LOGGER.info(
                    "Agent completeness diagnosis completed, taskId={}, score={}, readiness={}",
                    task.getId(),
                    report.completenessScore(),
                    report.archiveReadiness());
                return new AgentWorkflowStartVO(
                        task.getId(),
                        AgentTaskStatus.WAITING_CONFIRMATION.getCode(),
                        "证据分析与复测方案生成完成，请确认下一步业务动作。");
        } catch (RuntimeException exception) {
            failWorkflow(task.getId(), currentStep, exception);
            throw exception;
        }
    }

    private void ensureDiagnosisSteps(Long agentTaskId) {
        LocalDateTime now = LocalDateTime.now();
        for (StepDefinition definition : DIAGNOSIS_STEPS) {
            AgentStepEntity step = new AgentStepEntity();
            step.setAgentTaskId(agentTaskId);
            step.setStepNo(definition.number());
            step.setStepType(definition.type());
            step.setStepName(definition.name());
            step.setDescription(definition.description());
            step.setStatus("PENDING");
            step.setRetryCount(0);
            step.setMaxRetryCount(0);
            step.setCreateTime(now);
            step.setUpdateTime(now);
            stepMapper.insertIfAbsent(step);
        }
    }

    private Map<String, AgentStepEntity> loadDiagnosisSteps(Long agentTaskId) {
        Map<String, AgentStepEntity> steps =
                stepMapper.selectByTaskId(agentTaskId).stream()
                        .filter(
                                step ->
                                        DIAGNOSIS_STEPS.stream()
                                                .anyMatch(
                                                        def ->
                                                                def.type()
                                                                        .equals(
                                                                                step
                                                                                        .getStepType())))
                        .collect(
                                Collectors.toMap(
                                        AgentStepEntity::getStepType,
                                        step -> step,
                                        (left, right) -> left));
        if (steps.size() != DIAGNOSIS_STEPS.size()) {
            throw new BusinessException("Agent 完整性诊断步骤初始化失败");
        }
        return steps;
    }

    private void beginStep(
            AgentTaskEntity task, AgentStepEntity step, String currentPhase, int progress) {
        if (step == null || stepMapper.markRunning(step.getId(), LocalDateTime.now()) == 0) {
            throw new ResourceConflictException("Agent 步骤已被其他执行器推进");
        }
        step.setStatus("RUNNING");
        taskService.updateProgress(task.getId(), progress, currentPhase);
    }

    private void succeedStep(
            AgentTaskEntity task,
            AgentStepEntity step,
            String summary,
            Object output,
            int progress) {
        String outputJson = toJson(output);
        if (stepMapper.markSucceeded(step.getId(), summary, outputJson, LocalDateTime.now()) == 0) {
            throw new ResourceConflictException("Agent 步骤完成状态更新冲突");
        }
        step.setStatus("SUCCEEDED");
        taskService.updateProgress(task.getId(), progress, phaseAfter(step.getStepType()));
    }

    private String phaseAfter(String stepType) {
        return switch (stepType) {
            case "LOAD_CONTEXT" -> "LOADING_CONTEXT";
            case "INSPECT_TASK" -> "INSPECTING_TASK";
            case "ANALYZE_TIMELINE" -> "CHECKING_TIMELINE";
            case "ANALYZE_EVIDENCE" -> "CHECKING_EVIDENCE";
            default -> null;
        };
    }

    private <T> T invoke(
            String toolName,
            AgentToolExecutionContext context,
            String requestSummary,
            Supplier<T> invocation) {
        AgentToolResult<T> result =
                toolExecutor.execute(toolName, context, requestSummary, invocation::get);
        if (!Boolean.TRUE.equals(result.success())) {
            throw new BusinessException(result.summary());
        }
        return result.data();
    }

    private void persistFindings(Long agentTaskId, Long stepId, List<FindingDraft> findingDrafts) {
        LocalDateTime now = LocalDateTime.now();
        for (FindingDraft draft : findingDrafts) {
            AgentFindingEntity finding = new AgentFindingEntity();
            finding.setAgentTaskId(agentTaskId);
            finding.setStepId(stepId);
            finding.setFindingType(draft.findingType());
            finding.setSeverity(draft.severity());
            finding.setTargetType(draft.targetType());
            finding.setTargetId(draft.targetId());
            finding.setTitle(draft.title());
            finding.setDescription(draft.description());
            finding.setEvidenceJson(toJson(draft.evidence()));
            finding.setSuggestion(draft.suggestion());
            finding.setStatus("OPEN");
            finding.setCreateTime(now);
            finding.setUpdateTime(now);
            findingMapper.upsert(finding);
        }
    }

    private void failWorkflow(Long agentTaskId, AgentStepEntity step, RuntimeException exception) {
        if (step != null && "RUNNING".equals(step.getStatus())) {
            stepMapper.markFailed(
                    step.getId(),
                    "DIAGNOSIS_FAILED",
                    truncate(exception.getMessage(), 1000),
                    LocalDateTime.now());
        }
        AgentTaskEntity latest = requireTask(agentTaskId);
        AgentTaskStatus status = AgentTaskStatus.fromCode(latest.getStatus());
        if (status == AgentTaskStatus.PLANNING || status == AgentTaskStatus.RUNNING) {
            stateMachine.transition(
                    latest,
                    AgentTaskStatus.FAILED,
                    "完整性诊断失败：" + truncate(exception.getMessage(), 900));
        }
        LOGGER.warn(
                "Agent completeness diagnosis failed, taskId={}, error={}",
                agentTaskId,
                exception.getClass().getSimpleName());
    }

    private AgentToolExecutionContext context(AgentTaskEntity task, AgentStepEntity step) {
        return AgentToolExecutionContext.fromCurrentUser(
                new AgentToolExecutionContext.Scope(
                        task.getSessionId(),
                        task.getId(),
                        step.getId(),
                        "digital-life",
                        task.getCollectionTaskId(),
                        null,
                        null,
                        null,
                        UUID.randomUUID().toString()));
    }

    private AgentTaskEntity requireTask(Long agentTaskId) {
        AgentTaskEntity task = taskMapper.selectById(agentTaskId);
        if (task == null) {
            throw new BusinessException("Agent 任务不存在");
        }
        return task;
    }

    private AgentWorkflowStartVO alreadyStarted(AgentTaskEntity task) {
        return new AgentWorkflowStartVO(task.getId(), task.getStatus(), "Agent 任务已启动，本次请求未重复执行诊断。");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Agent 诊断结果序列化失败");
        }
    }

    private String truncate(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return "未知错误";
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private record StepDefinition(int number, String type, String name, String description) {}
}
