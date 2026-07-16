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
import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.AnalysisInput;
import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.AnalysisOutcome;
import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.EvidenceFindingDraft;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutionContext;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutor;
import com.bdis.modules.assistant.agent.tool.AgentToolResult;
import com.bdis.modules.assistant.agent.tool.DigitalLifeArchiveReadTool;
import com.bdis.modules.assistant.agent.tool.GrowthRecordReadTool;
import com.bdis.modules.assistant.agent.tool.RecognitionReadTool;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceExplanation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class HerbDigitalTwinEvidenceAnalysisRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HerbDigitalTwinEvidenceAnalysisRunner.class);
    private static final int TIMELINE_STEP_NO = 5;
    private static final int EVIDENCE_STEP_NO = 6;

    private final AgentTaskMapper taskMapper;
    private final AgentStepMapper stepMapper;
    private final AgentFindingMapper findingMapper;
    private final HerbDigitalTwinAgentTaskService taskService;
    private final AgentTaskStateMachine stateMachine;
    private final AgentToolExecutor toolExecutor;
    private final GrowthRecordReadTool growthRecordTool;
    private final RecognitionReadTool recognitionTool;
    private final DigitalLifeArchiveReadTool archiveTool;
    private final AgentEvidenceAnalysisService analysisService;
    private final AgentEvidenceExplanationService explanationService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, ReentrantLock> taskLocks = new ConcurrentHashMap<>();

    public HerbDigitalTwinEvidenceAnalysisRunner(
            AgentTaskMapper taskMapper,
            AgentStepMapper stepMapper,
            AgentFindingMapper findingMapper,
            HerbDigitalTwinAgentTaskService taskService,
            AgentTaskStateMachine stateMachine,
            AgentToolExecutor toolExecutor,
            GrowthRecordReadTool growthRecordTool,
            RecognitionReadTool recognitionTool,
            DigitalLifeArchiveReadTool archiveTool,
            AgentEvidenceAnalysisService analysisService,
            AgentEvidenceExplanationService explanationService,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.findingMapper = findingMapper;
        this.taskService = taskService;
        this.stateMachine = stateMachine;
        this.toolExecutor = toolExecutor;
        this.growthRecordTool = growthRecordTool;
        this.recognitionTool = recognitionTool;
        this.archiveTool = archiveTool;
        this.analysisService = analysisService;
        this.explanationService = explanationService;
        this.objectMapper = objectMapper;
    }

    public AgentEvidenceAnalysisVO run(Long agentTaskId) {
        taskService.getDetail(agentTaskId);
        ReentrantLock lock = taskLocks.computeIfAbsent(agentTaskId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return runLocked(agentTaskId);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                taskLocks.remove(agentTaskId, lock);
            }
        }
    }

    public AgentEvidenceAnalysisResponseVO getAnalysis(Long agentTaskId) {
        taskService.getDetail(agentTaskId);
        AgentStepEntity step = stepMapper.selectByTaskIdAndNo(agentTaskId, EVIDENCE_STEP_NO);
        if (step == null || !StringUtils.hasText(step.getOutputJson())) {
            return new AgentEvidenceAnalysisResponseVO(
                    false, step == null ? "NOT_STARTED" : step.getStatus(), "证据分析尚未生成。", null);
        }
        return new AgentEvidenceAnalysisResponseVO(
                true, "AVAILABLE", "证据分析已生成。", readAnalysis(step.getOutputJson()));
    }

    private AgentEvidenceAnalysisVO runLocked(Long agentTaskId) {
        AgentTaskEntity task = requireRunningTask(agentTaskId);
        ensureSteps(task.getId());
        AgentStepEntity evidenceStep =
                stepMapper.selectByTaskIdAndNo(task.getId(), EVIDENCE_STEP_NO);
        if (evidenceStep != null
                && "SUCCEEDED".equals(evidenceStep.getStatus())
                && StringUtils.hasText(evidenceStep.getOutputJson())) {
            return readAnalysis(evidenceStep.getOutputJson());
        }

        AgentStepEntity currentStep = null;
        try {
            AgentStepEntity timelineStep =
                    stepMapper.selectByTaskIdAndNo(task.getId(), TIMELINE_STEP_NO);
            GrowthRecordToolData.RecordList growthRecords;
            if (timelineStep != null
                    && "SUCCEEDED".equals(timelineStep.getStatus())
                    && StringUtils.hasText(timelineStep.getOutputJson())) {
                growthRecords =
                        read(timelineStep.getOutputJson(), GrowthRecordToolData.RecordList.class);
            } else {
                currentStep = timelineStep;
                beginStep(task, timelineStep, "ANALYZING_METRIC_TIMELINE", 35);
                AgentToolExecutionContext context = context(task, timelineStep);
                growthRecords =
                        invoke(
                                GrowthRecordReadTool.LIST_BY_TASK,
                                context,
                                "读取连续阶段指标用于时序分析",
                                () ->
                                        growthRecordTool.listTaskGrowthRecords(
                                                task.getCollectionTaskId(), context));
                succeedStep(task, timelineStep, "时序指标读取与差值计算输入已准备", growthRecords, 45);
            }

            currentStep = evidenceStep;
            beginStep(task, evidenceStep, "ANALYZING_MULTIMODAL_EVIDENCE", 45);
            AgentToolExecutionContext context = context(task, evidenceStep);
            DigitalLifeArchiveToolData.StageList stages =
                    invoke(
                            DigitalLifeArchiveReadTool.STAGES,
                            context,
                            "读取阶段图片、位置与审核证据",
                            () ->
                                    archiveTool.getDigitalLifeStages(
                                            task.getCollectionTaskId(), context));
            RecognitionToolData.TaskImageList unrecognized =
                    invoke(
                            RecognitionReadTool.UNRECOGNIZED,
                            context,
                            "读取未识别图片证据",
                            () ->
                                    recognitionTool.listUnrecognizedImages(
                                            task.getCollectionTaskId(), context));
            RecognitionToolData.TaskImageList lowConfidence =
                    invoke(
                            RecognitionReadTool.LOW_CONFIDENCE,
                            context,
                            "读取低置信度识别证据",
                            () ->
                                    recognitionTool.listLowConfidenceImages(
                                            task.getCollectionTaskId(), context));

            AnalysisOutcome outcome =
                    analysisService.analyze(
                            new AnalysisInput(
                                    task.getId(),
                                    task.getCollectionTaskId(),
                                    growthRecords,
                                    stages,
                                    unrecognized,
                                    lowConfidence));
            AgentEvidenceExplanation explanation = explanationService.explain(outcome.analysis());
            AgentEvidenceAnalysisVO analysis = withExplanation(outcome.analysis(), explanation);
            persistFindings(task.getId(), evidenceStep.getId(), outcome.findings());
            succeedStep(task, evidenceStep, "多模态异常与证据缺口分析完成", analysis, 55);
            taskService.updateProgress(task.getId(), 55, "EVIDENCE_ANALYSIS_COMPLETED");
            LOGGER.info(
                    "Agent evidence analysis completed, taskId={}, stageCount={}, findingCount={}",
                    task.getId(),
                    analysis.stageCount(),
                    outcome.findings().size());
            return analysis;
        } catch (RuntimeException exception) {
            failWorkflow(task.getId(), currentStep, exception);
            throw exception;
        }
    }

    private void ensureSteps(Long agentTaskId) {
        insertStep(
                agentTaskId,
                TIMELINE_STEP_NO,
                "ANALYZE_TIMELINE",
                "分析多阶段指标变化",
                "计算相邻差值、变化率、连续方向和历史偏离");
        insertStep(
                agentTaskId,
                EVIDENCE_STEP_NO,
                "ANALYZE_EVIDENCE",
                "分析多模态证据缺口",
                "联合指标、图片类型、识别、位置和审核元数据形成可复核分析");
    }

    private void insertStep(
            Long agentTaskId, int stepNo, String type, String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        AgentStepEntity step = new AgentStepEntity();
        step.setAgentTaskId(agentTaskId);
        step.setStepNo(stepNo);
        step.setStepType(type);
        step.setStepName(name);
        step.setDescription(description);
        step.setStatus("PENDING");
        step.setRetryCount(0);
        step.setMaxRetryCount(0);
        step.setCreateTime(now);
        step.setUpdateTime(now);
        stepMapper.insertIfAbsent(step);
    }

    private void beginStep(AgentTaskEntity task, AgentStepEntity step, String phase, int progress) {
        if (step == null || stepMapper.markRunning(step.getId(), LocalDateTime.now()) == 0) {
            throw new ResourceConflictException("Agent 证据分析步骤已由其他执行器推进");
        }
        step.setStatus("RUNNING");
        taskService.updateProgress(task.getId(), progress, phase);
    }

    private void succeedStep(
            AgentTaskEntity task,
            AgentStepEntity step,
            String summary,
            Object output,
            int progress) {
        LocalDateTime now = LocalDateTime.now();
        if (stepMapper.markSucceeded(step.getId(), summary, toJson(output), now) == 0) {
            throw new ResourceConflictException("Agent 证据分析步骤完成状态更新冲突");
        }
        step.setStatus("SUCCEEDED");
        taskService.updateProgress(task.getId(), progress, "ANALYZING_MULTIMODAL_EVIDENCE");
    }

    private void persistFindings(
            Long agentTaskId, Long stepId, List<EvidenceFindingDraft> findingDrafts) {
        LocalDateTime now = LocalDateTime.now();
        for (EvidenceFindingDraft draft : findingDrafts) {
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

    private <T> T invoke(
            String toolName,
            AgentToolExecutionContext context,
            String summary,
            Supplier<T> invocation) {
        AgentToolResult<T> result =
                toolExecutor.execute(toolName, context, summary, invocation::get);
        if (!Boolean.TRUE.equals(result.success())) {
            throw new BusinessException(result.summary());
        }
        return result.data();
    }

    private AgentEvidenceAnalysisVO withExplanation(
            AgentEvidenceAnalysisVO analysis, AgentEvidenceExplanation explanation) {
        return new AgentEvidenceAnalysisVO(
                analysis.agentTaskId(),
                analysis.collectionTaskId(),
                analysis.stageCount(),
                analysis.stages(),
                analysis.metricTrends(),
                analysis.findings(),
                analysis.evidenceGaps(),
                explanation,
                analysis.generatedTime());
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

    private AgentTaskEntity requireRunningTask(Long agentTaskId) {
        AgentTaskEntity task = taskMapper.selectById(agentTaskId);
        if (task == null) {
            throw new BusinessException("Agent 任务不存在");
        }
        if (!AgentTaskStatus.RUNNING.getCode().equals(task.getStatus())) {
            throw new BusinessException("Agent 任务当前状态不能执行证据分析");
        }
        return task;
    }

    private void failWorkflow(Long agentTaskId, AgentStepEntity step, RuntimeException exception) {
        if (step != null && "RUNNING".equals(step.getStatus())) {
            stepMapper.markFailed(
                    step.getId(),
                    "EVIDENCE_ANALYSIS_FAILED",
                    truncate(exception.getMessage()),
                    LocalDateTime.now());
        }
        AgentTaskEntity latest = taskMapper.selectById(agentTaskId);
        if (latest != null && AgentTaskStatus.RUNNING.getCode().equals(latest.getStatus())) {
            stateMachine.transition(
                    latest, AgentTaskStatus.FAILED, "证据分析失败：" + truncate(exception.getMessage()));
        }
        LOGGER.warn(
                "Agent evidence analysis failed, taskId={}, error={}",
                agentTaskId,
                exception.getClass().getSimpleName());
    }

    private AgentEvidenceAnalysisVO readAnalysis(String json) {
        return read(json, AgentEvidenceAnalysisVO.class);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Agent 证据分析结果读取失败");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Agent 证据分析结果序列化失败");
        }
    }

    private String truncate(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        return message.length() <= 900 ? message : message.substring(0, 900);
    }
}
