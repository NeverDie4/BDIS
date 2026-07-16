package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.vo.AgentCompletenessReportVO;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AgentCompletenessDiagnosisService {

    private static final Set<String> PLACEHOLDER_VALUES =
            Set.of("11", "66", "77", "111", "222", "666");

    public DiagnosisOutcome diagnose(DiagnosisInput input) {
        List<FindingDraft> findings = new ArrayList<>();
        inspectTask(input, findings);
        inspectStages(input, findings);
        inspectGrowthRecords(input, findings);
        inspectImages(input, findings);
        inspectReviews(input, findings);

        Score score = score(input);
        String readiness = readiness(input, findings);
        List<String> suggestions = suggestions(findings, readiness);
        String summary = summary(input, score.total(), findings, readiness);
        return new DiagnosisOutcome(
                input.agentTaskId(),
                input.task().taskId(),
                input.task().taskName(),
                input.archiveOverview().stageCount(),
                input.archiveOverview().validStageCount(),
                score.total(),
                readiness,
                score.breakdown(),
                score.deductions(),
                List.copyOf(findings),
                suggestions,
                summary,
                LocalDateTime.now());
    }

    private void inspectTask(DiagnosisInput input, List<FindingDraft> findings) {
        CollectionTaskToolData.Overview task = input.task();
        List<String> missing = new ArrayList<>();
        addMissing(missing, "任务名称", task.taskName());
        addMissing(missing, "药材", task.speciesId());
        addMissing(missing, "基地", task.baseId());
        if (!missing.isEmpty() || value(input.archiveOverview().stageCount()) == 0) {
            add(
                    findings,
                    "ARCHIVE_NOT_READY",
                    "HIGH",
                    "COLLECTION_TASK",
                    task.taskId(),
                    "数字生命档案尚未就绪",
                    "任务基础信息或观测阶段不完整：" + String.join("、", missing),
                    Map.of(
                            "missingFields",
                            missing,
                            "stageCount",
                            value(input.archiveOverview().stageCount())),
                    "补齐任务基础信息并至少形成一个有效观测阶段",
                    true);
        }
        if (task.baseId() == null && !StringUtils.hasText(task.collectPlace())) {
            add(
                    findings,
                    "LOCATION_MISSING",
                    "HIGH",
                    "COLLECTION_TASK",
                    task.taskId(),
                    "任务缺少基地或采集地点",
                    "任务未绑定基地且未填写采集地点",
                    Map.of("baseMissing", true, "collectPlaceMissing", true),
                    "补充真实基地或采集地点",
                    true);
        }
        if (looksLikeTest(task.taskName()) || looksLikeTest(task.collectPlace())) {
            add(
                    findings,
                    "SUSPECTED_TEST_DATA",
                    "LOW",
                    "COLLECTION_TASK",
                    task.taskId(),
                    "任务基础信息疑似测试占位值",
                    "任务名称或地点包含明显数字占位内容",
                    Map.of(
                            "taskName",
                            nullable(task.taskName()),
                            "collectPlace",
                            nullable(task.collectPlace())),
                    "请人工核对数据来源，不自动修改业务数据",
                    false);
        }
        if (task.taskStatus() != null && "cancelled".equals(task.taskStatus().code())) {
            add(
                    findings,
                    "ARCHIVE_NOT_READY",
                    "CRITICAL",
                    "COLLECTION_TASK",
                    task.taskId(),
                    "采集任务已取消",
                    "已取消任务不能继续形成可信数字生命档案",
                    Map.of("taskStatus", "cancelled"),
                    "确认是否应重新创建正式连续观测任务",
                    true);
        }
    }

    private void inspectStages(DiagnosisInput input, List<FindingDraft> findings) {
        for (DigitalLifeArchiveToolData.Stage stage : input.stages().stages()) {
            Long batchId = stage.batchId();
            if (stage.growthRecordId() == null) {
                add(
                        findings,
                        "MISSING_GROWTH_RECORD",
                        "HIGH",
                        "BATCH",
                        batchId,
                        "批次缺少生长记录",
                        "该观测批次尚未关联生长记录",
                        Map.of("batchId", batchId),
                        "按批次补充唯一生长记录",
                        true);
            }
            if (!StringUtils.hasText(stage.collectorName())) {
                add(
                        findings,
                        "ARCHIVE_NOT_READY",
                        "MEDIUM",
                        "BATCH",
                        batchId,
                        "批次缺少采集员信息",
                        "观测阶段无法确认现场采集责任人",
                        Map.of("batchId", batchId),
                        "核对任务分配和采集员快照",
                        false);
            }
            if (!StringUtils.hasText(stage.baseName())
                    && !StringUtils.hasText(stage.locationName())) {
                add(
                        findings,
                        "LOCATION_MISSING",
                        "HIGH",
                        "BATCH",
                        batchId,
                        "批次缺少采集地点",
                        "阶段未记录基地或地点",
                        Map.of("batchId", batchId),
                        "补充真实采集地点和定位证据",
                        true);
            }
            if (outsideTaskWindow(stage.collectedAt(), input.task())) {
                add(
                        findings,
                        "ABNORMAL_TIMELINE",
                        "MEDIUM",
                        "BATCH",
                        batchId,
                        "批次时间与任务计划明显冲突",
                        "采集时间明显超出任务计划时间范围",
                        Map.of(
                                "collectedAt", stage.collectedAt().toString(),
                                "plannedStartTime", nullable(input.task().plannedStartTime()),
                                "plannedEndTime", nullable(input.task().plannedEndTime())),
                        "人工核对任务计划与现场采集时间",
                        false);
            }
        }
    }

    private void inspectGrowthRecords(DiagnosisInput input, List<FindingDraft> findings) {
        for (GrowthRecordToolData.Overview record : input.growthRecords().records()) {
            List<String> required = new ArrayList<>();
            addMissing(required, "growthStage", record.growthMetrics().growthStage());
            addMissing(required, "collectedAt", record.collectedAt());
            List<String> recommended = recommendedMissing(record);
            if (!required.isEmpty() || !recommended.isEmpty()) {
                String severity = required.isEmpty() ? "MEDIUM" : "HIGH";
                add(
                        findings,
                        "MISSING_METRIC",
                        severity,
                        "GROWTH_RECORD",
                        record.recordId(),
                        "生长记录指标不完整",
                        "缺少必需字段 " + required + "；缺少推荐字段 " + recommended,
                        Map.of("requiredMissing", required, "recommendedMissing", recommended),
                        required.isEmpty() ? "建议补充观测指标以提高档案完整度" : "补齐必需字段后再提交审核",
                        !required.isEmpty());
            }
            if (looksLikeTest(record.growthMetrics().growthStage())
                    || hasPlaceholderMetric(record)) {
                add(
                        findings,
                        "SUSPECTED_TEST_DATA",
                        "LOW",
                        "GROWTH_RECORD",
                        record.recordId(),
                        "生长记录疑似测试占位值",
                        "生长阶段或指标命中明显占位值规则",
                        Map.of("growthStage", nullable(record.growthMetrics().growthStage())),
                        "请人工核对原始采集记录，不自动修改数据",
                        false);
            }
        }
    }

    private void inspectImages(DiagnosisInput input, List<FindingDraft> findings) {
        Map<Long, List<RecognitionToolData.ImageOverview>> unrecognized =
                groupByBatch(input.unrecognizedImages().images());
        Map<Long, List<RecognitionToolData.ImageOverview>> lowConfidence =
                groupByBatch(input.lowConfidenceImages().images());
        for (DigitalLifeArchiveToolData.Stage stage : input.stages().stages()) {
            List<DigitalLifeArchiveToolData.ImageEvidence> images = safe(stage.images());
            if (images.isEmpty()) {
                add(
                        findings,
                        "MISSING_IMAGE",
                        "HIGH",
                        "BATCH",
                        stage.batchId(),
                        "批次缺少现场图片",
                        "该观测阶段没有现场图片证据",
                        Map.of("batchId", stage.batchId(), "imageCount", 0),
                        "补充至少一张可访问的现场图片",
                        true);
                continue;
            }
            if (images.stream().anyMatch(image -> !StringUtils.hasText(image.imageUrl()))) {
                add(
                        findings,
                        "FILE_RECORD_MISMATCH",
                        "HIGH",
                        "BATCH",
                        stage.batchId(),
                        "图片记录与可访问文件不一致",
                        "至少一条图片数据库记录无法生成浏览器可访问 URL",
                        Map.of(
                                "imageIds",
                                images.stream()
                                        .filter(image -> !StringUtils.hasText(image.imageUrl()))
                                        .map(DigitalLifeArchiveToolData.ImageEvidence::imageId)
                                        .toList()),
                        "核对文件绑定与受控访问地址",
                        true);
            }
            if (images.stream()
                    .allMatch(
                            image ->
                                    !StringUtils.hasText(image.imageType())
                                            || "other".equalsIgnoreCase(image.imageType()))) {
                add(
                        findings,
                        "MISSING_IMAGE_TYPE",
                        "MEDIUM",
                        "BATCH",
                        stage.batchId(),
                        "现场图片缺少有效类型",
                        "批次图片类型全部为空或为 other",
                        Map.of("imageCount", images.size()),
                        "按真实内容标注图片类型",
                        false);
            }
            for (RecognitionToolData.ImageOverview image :
                    unrecognized.getOrDefault(stage.batchId(), List.of())) {
                add(
                        findings,
                        "UNRECOGNIZED_IMAGE",
                        "MEDIUM",
                        "IMAGE",
                        image.imageId(),
                        "图片尚未识别",
                        "现场图片没有已有识别结果",
                        Map.of("batchId", stage.batchId(), "imageId", image.imageId()),
                        "后续由正式识别流程处理，本次不触发重新识别",
                        false);
            }
            for (RecognitionToolData.ImageOverview image :
                    lowConfidence.getOrDefault(stage.batchId(), List.of())) {
                add(
                        findings,
                        "LOW_CONFIDENCE_RECOGNITION",
                        "MEDIUM",
                        "IMAGE",
                        image.imageId(),
                        "图片识别置信度偏低",
                        "已有识别结果低于当前只读检查阈值 0.70",
                        Map.of(
                                "batchId", stage.batchId(),
                                "imageId", image.imageId(),
                                "confidence", nullable(image.highestSimilarity())),
                        "安排人工复核识别结果",
                        false);
            }
        }
    }

    private void inspectReviews(DiagnosisInput input, List<FindingDraft> findings) {
        for (GrowthRecordToolData.Overview record : input.growthRecords().records()) {
            String status = record.reviewStatus() == null ? null : record.reviewStatus().code();
            if ("draft".equals(status) || "submitted".equals(status)) {
                add(
                        findings,
                        "REVIEW_PENDING",
                        "MEDIUM",
                        "GROWTH_RECORD",
                        record.recordId(),
                        "生长记录尚未审核通过",
                        "draft".equals(status) ? "记录尚未提交审核" : "记录正在等待审核",
                        Map.of("reviewStatus", status),
                        "按现有审核流程完成审核",
                        false);
            } else if ("rejected".equals(status)) {
                add(
                        findings,
                        "REVIEW_REJECTED",
                        "HIGH",
                        "GROWTH_RECORD",
                        record.recordId(),
                        "生长记录审核被驳回",
                        "该阶段存在已驳回记录，当前阻塞档案公开",
                        Map.of("reviewStatus", status),
                        "根据审核流程修正后重新提交",
                        true);
            }
        }
    }

    private Score score(DiagnosisInput input) {
        Map<String, Integer> values = new LinkedHashMap<>();
        Map<String, List<String>> deductions = new LinkedHashMap<>();
        CollectionTaskToolData.Overview task = input.task();
        int taskScore = 0;
        List<String> taskLoss = new ArrayList<>();
        taskScore += points(task.taskName(), 2, "缺少任务名称", taskLoss);
        taskScore += points(task.speciesId(), 2, "缺少药材", taskLoss);
        taskScore += points(task.baseId(), 2, "缺少基地", taskLoss);
        taskScore += points(task.collectPlace(), 2, "缺少采集地点", taskLoss);
        taskScore +=
                task.taskStatus() != null && !"cancelled".equals(task.taskStatus().code()) ? 2 : 0;
        if (task.taskStatus() == null || "cancelled".equals(task.taskStatus().code())) {
            taskLoss.add("任务状态不可用");
        }
        put(values, deductions, "任务基础信息", taskScore, taskLoss);

        int stageCount = value(input.archiveOverview().stageCount());
        int validStageCount = value(input.archiveOverview().validStageCount());
        int continuity = stageCount == 0 ? 0 : 5 + (10 * validStageCount / stageCount);
        put(
                values,
                deductions,
                "阶段连续性",
                continuity,
                continuity == 15 ? List.of() : List.of("存在无效或缺失观测阶段"));

        int growthPresent =
                input.growthRecords().records().stream()
                        .mapToInt(this::presentGrowthFieldCount)
                        .sum();
        int growthDenominator = Math.max(stageCount * 8, 1);
        int growthScore = stageCount == 0 ? 0 : 25 * growthPresent / growthDenominator;
        put(
                values,
                deductions,
                "生长记录完整性",
                growthScore,
                growthScore == 25 ? List.of() : List.of("部分阶段缺少必需或推荐观测字段"));

        List<DigitalLifeArchiveToolData.ImageEvidence> images = allImages(input);
        long stagesWithImages =
                input.stages().stages().stream()
                        .filter(stage -> !safe(stage.images()).isEmpty())
                        .count();
        int imageScore = stageCount == 0 ? 0 : (int) (10 * stagesWithImages / stageCount);
        if (!images.isEmpty()) {
            long usable =
                    images.stream()
                            .filter(image -> StringUtils.hasText(image.imageUrl()))
                            .filter(
                                    image ->
                                            StringUtils.hasText(image.imageType())
                                                    && !"other".equalsIgnoreCase(image.imageType()))
                            .count();
            imageScore += (int) (5 * usable / images.size());
        }
        put(
                values,
                deductions,
                "现场图片证据",
                imageScore,
                imageScore == 15 ? List.of() : List.of("部分阶段缺图、图片不可访问或类型未标注"));

        int recognitionScore =
                images.isEmpty()
                        ? 0
                        : 10
                                * Math.max(
                                        images.size() - input.unrecognizedImages().images().size(),
                                        0)
                                / images.size();
        put(
                values,
                deductions,
                "识别结果完整性",
                recognitionScore,
                recognitionScore == 10 ? List.of() : List.of("存在未识别图片"));

        long approved =
                input.growthRecords().records().stream()
                        .filter(
                                record ->
                                        record.reviewStatus() != null
                                                && "approved".equals(record.reviewStatus().code()))
                        .count();
        int auditScore = stageCount == 0 ? 0 : (int) (15 * approved / stageCount);
        put(
                values,
                deductions,
                "审核流程",
                auditScore,
                auditScore == 15 ? List.of() : List.of("部分阶段尚未审核通过"));

        int traceScore = 0;
        List<String> traceLoss = new ArrayList<>();
        if (Boolean.TRUE.equals(input.archiveOverview().qrCodeGenerated())) {
            traceScore += 3;
        } else {
            traceLoss.add("尚无档案编号或二维码");
        }
        if (input.archiveOverview().archiveStatus() != null
                && !"not_ready".equals(input.archiveOverview().archiveStatus().code())) {
            traceScore += 3;
        } else {
            traceLoss.add("档案状态未就绪");
        }
        if (Boolean.TRUE.equals(input.integrity().verified())) {
            traceScore += 4;
        } else {
            traceLoss.add("哈希证据链尚未通过校验");
        }
        put(values, deductions, "溯源与档案状态", traceScore, traceLoss);
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        return new Score(total, Map.copyOf(values), copyDeductions(deductions));
    }

    private String readiness(DiagnosisInput input, List<FindingDraft> findings) {
        if (Boolean.TRUE.equals(input.archiveOverview().publicVisible())) {
            return "ALREADY_PUBLISHED";
        }
        if (value(input.archiveOverview().stageCount()) == 0
                || findings.stream()
                        .anyMatch(
                                finding ->
                                        finding.blocking()
                                                && Set.of("HIGH", "CRITICAL")
                                                        .contains(finding.severity()))) {
            return "NOT_READY";
        }
        boolean allApproved =
                !input.growthRecords().records().isEmpty()
                        && input.growthRecords().records().stream()
                                .allMatch(
                                        record ->
                                                record.reviewStatus() != null
                                                        && "approved"
                                                                .equals(
                                                                        record.reviewStatus()
                                                                                .code()));
        if (!allApproved) {
            return "PARTIALLY_READY";
        }
        boolean publicEvidenceGap =
                findings.stream()
                        .anyMatch(
                                finding ->
                                        Set.of(
                                                        "UNRECOGNIZED_IMAGE",
                                                        "LOW_CONFIDENCE_RECOGNITION",
                                                        "MISSING_IMAGE_TYPE")
                                                .contains(finding.findingType()));
        if (!publicEvidenceGap && findings.stream().noneMatch(FindingDraft::blocking)) {
            return "READY_FOR_PUBLIC_ARCHIVE";
        }
        if (value(input.archiveOverview().validStageCount()) > 0
                && findings.stream().noneMatch(FindingDraft::blocking)) {
            return "READY_FOR_INTERNAL_ARCHIVE";
        }
        return "PARTIALLY_READY";
    }

    private List<String> suggestions(List<FindingDraft> findings, String readiness) {
        LinkedHashSet<String> suggestions =
                findings.stream()
                        .map(FindingDraft::suggestion)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (suggestions.isEmpty()) {
            suggestions.add(
                    "READY_FOR_PUBLIC_ARCHIVE".equals(readiness)
                            ? "数据已满足当前公开档案规则，后续仍需用户确认公开动作"
                            : "继续维持连续观测并按现有审核流程归档");
        }
        return suggestions.stream().limit(8).toList();
    }

    private String summary(
            DiagnosisInput input, int score, List<FindingDraft> findings, String readiness) {
        long blocking = findings.stream().filter(FindingDraft::blocking).count();
        return "当前任务数字档案完整度为 "
                + score
                + "%（仅表示档案完整度，不代表医学可信度）。共有 "
                + value(input.archiveOverview().stageCount())
                + " 个观测阶段，其中 "
                + value(input.archiveOverview().validStageCount())
                + " 个有效阶段；发现 "
                + blocking
                + " 个阻塞项，档案准备状态为 "
                + readiness
                + "。";
    }

    private List<String> recommendedMissing(GrowthRecordToolData.Overview record) {
        List<String> missing = new ArrayList<>();
        GrowthRecordToolData.EnvironmentMetrics env = record.environmentMetrics();
        GrowthRecordToolData.GrowthMetrics growth = record.growthMetrics();
        addMissing(missing, "plantHeight", growth.plantHeight());
        addMissing(missing, "temperature", env.temperature());
        addMissing(missing, "humidity", env.humidity());
        addMissing(missing, "soilMoisture", env.soilMoisture());
        addMissing(missing, "soilPh", env.soilPh());
        addMissing(missing, "light", env.light());
        return missing;
    }

    private int presentGrowthFieldCount(GrowthRecordToolData.Overview record) {
        int missing = recommendedMissing(record).size();
        if (!StringUtils.hasText(record.growthMetrics().growthStage())) {
            missing++;
        }
        if (record.collectedAt() == null) {
            missing++;
        }
        return 8 - missing;
    }

    private boolean hasPlaceholderMetric(GrowthRecordToolData.Overview record) {
        return java.util.stream.Stream.of(
                        record.growthMetrics().plantHeight(),
                        record.environmentMetrics().temperature(),
                        record.environmentMetrics().humidity(),
                        record.environmentMetrics().soilMoisture(),
                        record.environmentMetrics().soilPh(),
                        record.environmentMetrics().light())
                .filter(java.util.Objects::nonNull)
                .map(BigDecimal::stripTrailingZeros)
                .map(BigDecimal::toPlainString)
                .anyMatch(PLACEHOLDER_VALUES::contains);
    }

    private boolean outsideTaskWindow(
            LocalDateTime collectedAt, CollectionTaskToolData.Overview task) {
        if (collectedAt == null) {
            return false;
        }
        return task.plannedStartTime() != null
                        && collectedAt.isBefore(task.plannedStartTime().minusDays(7))
                || task.plannedEndTime() != null
                        && collectedAt.isAfter(task.plannedEndTime().plusDays(7));
    }

    private Map<Long, List<RecognitionToolData.ImageOverview>> groupByBatch(
            List<RecognitionToolData.ImageOverview> images) {
        return images.stream()
                .filter(image -> image.batchId() != null)
                .collect(Collectors.groupingBy(RecognitionToolData.ImageOverview::batchId));
    }

    private List<DigitalLifeArchiveToolData.ImageEvidence> allImages(DiagnosisInput input) {
        return input.stages().stages().stream()
                .flatMap(stage -> safe(stage.images()).stream())
                .toList();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int points(Object value, int points, String reason, List<String> deductions) {
        boolean present =
                value != null && (!(value instanceof String text) || StringUtils.hasText(text));
        if (!present) {
            deductions.add(reason);
        }
        return present ? points : 0;
    }

    private void put(
            Map<String, Integer> values,
            Map<String, List<String>> deductions,
            String category,
            int score,
            List<String> reasons) {
        values.put(category, score);
        deductions.put(category, List.copyOf(reasons));
    }

    private Map<String, List<String>> copyDeductions(Map<String, List<String>> values) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> copied.put(key, List.copyOf(value)));
        return Map.copyOf(copied);
    }

    private void addMissing(List<String> missing, String name, Object value) {
        if (value == null || value instanceof String text && !StringUtils.hasText(text)) {
            missing.add(name);
        }
    }

    private boolean looksLikeTest(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim();
        return PLACEHOLDER_VALUES.contains(normalized)
                || normalized.matches("\\d+") && normalized.length() <= 4;
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private void add(
            List<FindingDraft> findings,
            String type,
            String severity,
            String targetType,
            Long targetId,
            String title,
            String description,
            Map<String, Object> evidence,
            String suggestion,
            boolean blocking) {
        findings.add(
                new FindingDraft(
                        type,
                        severity,
                        targetType,
                        targetId,
                        title,
                        description,
                        evidence,
                        suggestion,
                        blocking));
    }

    public record DiagnosisInput(
            Long agentTaskId,
            CollectionTaskToolData.Overview task,
            CollectionTaskToolData.BatchList batches,
            GrowthRecordToolData.RecordList growthRecords,
            RecognitionToolData.TaskImageList unrecognizedImages,
            RecognitionToolData.TaskImageList lowConfidenceImages,
            DigitalLifeArchiveToolData.Overview archiveOverview,
            DigitalLifeArchiveToolData.StageList stages,
            DigitalLifeArchiveToolData.IntegrityStatus integrity) {}

    public record FindingDraft(
            String findingType,
            String severity,
            String targetType,
            Long targetId,
            String title,
            String description,
            Map<String, Object> evidence,
            String suggestion,
            boolean blocking) {}

    public record DiagnosisOutcome(
            Long agentTaskId,
            Long collectionTaskId,
            String collectionTaskName,
            Integer stageCount,
            Integer validStageCount,
            Integer completenessScore,
            String archiveReadiness,
            Map<String, Integer> scoreBreakdown,
            Map<String, List<String>> scoreDeductions,
            List<FindingDraft> findings,
            List<String> nextSuggestions,
            String summary,
            LocalDateTime generatedTime) {

        public AgentCompletenessReportVO toReport(List<AgentFindingVO> persistedFindings) {
            Predicate<AgentFindingVO> blocking =
                    finding -> Set.of("HIGH", "CRITICAL").contains(finding.getSeverity());
            return new AgentCompletenessReportVO(
                    agentTaskId,
                    collectionTaskId,
                    collectionTaskName,
                    stageCount,
                    validStageCount,
                    completenessScore,
                    archiveReadiness,
                    scoreBreakdown,
                    scoreDeductions,
                    persistedFindings.stream().filter(blocking).toList(),
                    persistedFindings.stream().filter(blocking.negate()).toList(),
                    nextSuggestions,
                    summary,
                    generatedTime);
        }
    }

    private record Score(
            int total, Map<String, Integer> breakdown, Map<String, List<String>> deductions) {}
}
