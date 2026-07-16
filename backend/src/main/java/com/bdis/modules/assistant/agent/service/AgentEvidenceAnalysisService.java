package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceExplanation;
import com.bdis.modules.assistant.agent.vo.CrossModalFindingVO;
import com.bdis.modules.assistant.agent.vo.EvidenceGapVO;
import com.bdis.modules.assistant.agent.vo.MetricTrendVO;
import com.bdis.modules.assistant.agent.vo.StageEvidenceSummaryVO;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgentEvidenceAnalysisService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Set<String> REQUIRED_IMAGE_TYPES =
            Set.of("root", "leaf", "whole_plant", "environment");
    private static final Map<String, MetricDefinition> METRICS = metrics();

    public AnalysisOutcome analyze(AnalysisInput input) {
        List<StageData> stages = stages(input);
        List<MetricTrendVO> trends = analyzeMetrics(stages);
        List<EvidenceFindingDraft> findingDrafts = new ArrayList<>();
        List<EvidenceGapVO> gaps = new ArrayList<>();
        List<CrossModalFindingVO> crossModal = new ArrayList<>();

        inspectStageCount(stages, findingDrafts, gaps);
        inspectTimeline(stages, findingDrafts, gaps);
        inspectImages(stages, input, findingDrafts, gaps);
        inspectMetricTrends(trends, findingDrafts, crossModal);
        inspectCrossModal(stages, trends, input, findingDrafts, crossModal);

        List<StageEvidenceSummaryVO> summaries = stages.stream().map(this::summary).toList();
        AgentEvidenceExplanation fallback = fallbackExplanation(stages, trends, gaps, crossModal);
        return new AnalysisOutcome(
                new AgentEvidenceAnalysisVO(
                        input.agentTaskId(),
                        input.collectionTaskId(),
                        stages.size(),
                        summaries,
                        trends,
                        List.copyOf(crossModal),
                        List.copyOf(gaps),
                        fallback,
                        LocalDateTime.now()),
                List.copyOf(findingDrafts));
    }

    private List<StageData> stages(AnalysisInput input) {
        Map<Long, GrowthRecordToolData.Overview> recordsByBatch =
                safe(input.growthRecords().records()).stream()
                        .filter(record -> record.batchId() != null)
                        .collect(
                                Collectors.toMap(
                                        GrowthRecordToolData.Overview::batchId,
                                        Function.identity(),
                                        (left, right) -> earlier(left, right)));
        List<StageData> result = new ArrayList<>();
        Set<Long> includedBatches = new LinkedHashSet<>();
        int fallbackSequence = 1;
        for (DigitalLifeArchiveToolData.Stage stage : safe(input.stages().stages())) {
            GrowthRecordToolData.Overview record = recordsByBatch.get(stage.batchId());
            result.add(
                    new StageData(
                            stage.stageId(),
                            stage.batchId(),
                            stage.sequence() == null ? fallbackSequence : stage.sequence(),
                            stage.collectedAt() != null
                                    ? stage.collectedAt()
                                    : record == null ? null : record.collectedAt(),
                            stage.locationName(),
                            stage.baseName(),
                            status(stage.reviewStatus()),
                            record,
                            safe(stage.images())));
            includedBatches.add(stage.batchId());
            fallbackSequence++;
        }
        for (GrowthRecordToolData.Overview record : safe(input.growthRecords().records())) {
            if (includedBatches.contains(record.batchId())) {
                continue;
            }
            result.add(
                    new StageData(
                            record.recordId(),
                            record.batchId(),
                            fallbackSequence++,
                            record.collectedAt(),
                            null,
                            record.baseName(),
                            status(record.reviewStatus()),
                            record,
                            List.of()));
        }
        result.sort(
                Comparator.comparing(
                                StageData::collectedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(
                                StageData::sequence, Comparator.nullsLast(Integer::compareTo)));
        return List.copyOf(result);
    }

    private GrowthRecordToolData.Overview earlier(
            GrowthRecordToolData.Overview left, GrowthRecordToolData.Overview right) {
        if (left.collectedAt() == null) {
            return right;
        }
        return right.collectedAt() != null && right.collectedAt().isBefore(left.collectedAt())
                ? right
                : left;
    }

    private List<MetricTrendVO> analyzeMetrics(List<StageData> stages) {
        List<MetricTrendVO> trends = new ArrayList<>();
        METRICS.forEach(
                (code, definition) -> {
                    List<MetricTrendVO.MetricPointVO> points =
                            stages.stream()
                                    .map(stage -> point(stage, definition))
                                    .filter(Objects::nonNull)
                                    .toList();
                    List<MetricTrendVO.MetricChangeVO> changes = changes(points);
                    BigDecimal mean = mean(points);
                    BigDecimal deviation = deviation(points, mean);
                    List<String> observations = observations(code, points, changes);
                    trends.add(
                            new MetricTrendVO(
                                    code,
                                    definition.label(),
                                    definition.unit(),
                                    points.size(),
                                    points.size() < 3 ? "ADJACENT_ONLY" : "IQR_ZSCORE_REFERENCE",
                                    points,
                                    missingStageIds(stages, definition),
                                    changes,
                                    longestStreak(changes, true),
                                    longestStreak(changes, false),
                                    mean,
                                    deviation,
                                    observations));
                });
        return List.copyOf(trends);
    }

    private List<Long> missingStageIds(List<StageData> stages, MetricDefinition definition) {
        return stages.stream()
                .filter(
                        stage ->
                                stage.record() == null
                                        || definition.value().apply(stage.record()) == null)
                .map(StageData::stageId)
                .filter(Objects::nonNull)
                .toList();
    }

    private MetricTrendVO.MetricPointVO point(StageData stage, MetricDefinition definition) {
        BigDecimal value = stage.record() == null ? null : definition.value().apply(stage.record());
        if (value == null) {
            return null;
        }
        return new MetricTrendVO.MetricPointVO(
                stage.stageId(), stage.batchId(), stage.sequence(), stage.collectedAt(), value);
    }

    private List<MetricTrendVO.MetricChangeVO> changes(List<MetricTrendVO.MetricPointVO> points) {
        List<MetricTrendVO.MetricChangeVO> changes = new ArrayList<>();
        for (int index = 1; index < points.size(); index++) {
            MetricTrendVO.MetricPointVO previous = points.get(index - 1);
            MetricTrendVO.MetricPointVO current = points.get(index);
            BigDecimal difference = current.value().subtract(previous.value());
            BigDecimal rate =
                    previous.value().compareTo(BigDecimal.ZERO) == 0
                            ? null
                            : difference
                                    .multiply(HUNDRED)
                                    .divide(previous.value().abs(), 2, RoundingMode.HALF_UP);
            changes.add(
                    new MetricTrendVO.MetricChangeVO(
                            previous.stageId(), current.stageId(), difference, rate));
        }
        return List.copyOf(changes);
    }

    private BigDecimal mean(List<MetricTrendVO.MetricPointVO> points) {
        if (points.isEmpty()) {
            return null;
        }
        BigDecimal sum =
                points.stream()
                        .map(MetricTrendVO.MetricPointVO::value)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(points.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal deviation(
            List<MetricTrendVO.MetricPointVO> points, BigDecimal historicalMean) {
        if (points.isEmpty()
                || historicalMean == null
                || historicalMean.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return points.get(points.size() - 1)
                .value()
                .subtract(historicalMean)
                .multiply(HUNDRED)
                .divide(historicalMean.abs(), 2, RoundingMode.HALF_UP);
    }

    private List<String> observations(
            String metricCode,
            List<MetricTrendVO.MetricPointVO> points,
            List<MetricTrendVO.MetricChangeVO> changes) {
        List<String> observations = new ArrayList<>();
        if (points.size() < 3) {
            observations.add("样本阶段少于 3，仅计算相邻差值，不使用复杂异常算法。");
        }
        int decline = longestStreak(changes, false);
        int rise = longestStreak(changes, true);
        if (decline >= 2) {
            observations.add(metricCode + " 连续下降 " + decline + " 次。");
        }
        if (rise >= 2) {
            observations.add(metricCode + " 连续上升 " + rise + " 次。");
        }
        if (points.size() >= 4 && isLastPointOutlier(points)) {
            observations.add("当前值同时满足 IQR 或 Z-score 参考异常条件，需结合更多证据复核。");
        }
        return List.copyOf(observations);
    }

    private int longestStreak(List<MetricTrendVO.MetricChangeVO> changes, boolean rise) {
        int longest = 0;
        int current = 0;
        for (MetricTrendVO.MetricChangeVO change : changes) {
            int sign = change.difference().compareTo(BigDecimal.ZERO);
            if (rise ? sign > 0 : sign < 0) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private boolean isLastPointOutlier(List<MetricTrendVO.MetricPointVO> points) {
        List<BigDecimal> sorted =
                points.stream().map(MetricTrendVO.MetricPointVO::value).sorted().toList();
        BigDecimal q1 = percentile(sorted, 0.25);
        BigDecimal q3 = percentile(sorted, 0.75);
        BigDecimal iqr = q3.subtract(q1);
        BigDecimal last = points.get(points.size() - 1).value();
        boolean outsideIqr =
                last.compareTo(q1.subtract(iqr.multiply(new BigDecimal("1.5")))) < 0
                        || last.compareTo(q3.add(iqr.multiply(new BigDecimal("1.5")))) > 0;
        double mean = sorted.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance =
                sorted.stream()
                        .mapToDouble(value -> Math.pow(value.doubleValue() - mean, 2))
                        .average()
                        .orElse(0);
        double standardDeviation = Math.sqrt(variance);
        boolean outsideZ =
                standardDeviation > 0
                        && Math.abs((last.doubleValue() - mean) / standardDeviation) >= 2;
        return outsideIqr || outsideZ;
    }

    private BigDecimal percentile(List<BigDecimal> sorted, double percentile) {
        int index = (int) Math.floor((sorted.size() - 1) * percentile);
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private void inspectStageCount(
            List<StageData> stages, List<EvidenceFindingDraft> findings, List<EvidenceGapVO> gaps) {
        if (stages.size() >= 3) {
            return;
        }
        addGap(
                findings,
                gaps,
                "INSUFFICIENT_STAGE_COUNT",
                "MEDIUM",
                "COLLECTION_TASK",
                null,
                "当前只有 " + stages.size() + " 个有效阶段，暂不足以使用稳定的历史异常算法。",
                "继续补充后续观测阶段；当前结论仅基于相邻差值和缺失检查。",
                Map.of("stageCount", stages.size()));
    }

    private void inspectTimeline(
            List<StageData> stages, List<EvidenceFindingDraft> findings, List<EvidenceGapVO> gaps) {
        List<StageData> bySequence =
                stages.stream()
                        .sorted(
                                Comparator.comparing(
                                        StageData::sequence,
                                        Comparator.nullsLast(Integer::compareTo)))
                        .toList();
        for (int index = 1; index < bySequence.size(); index++) {
            StageData previous = bySequence.get(index - 1);
            StageData current = bySequence.get(index);
            if (previous.collectedAt() != null
                    && current.collectedAt() != null
                    && current.collectedAt().isBefore(previous.collectedAt())) {
                addGap(
                        findings,
                        gaps,
                        "STAGE_SEQUENCE_SUSPECTED",
                        "MEDIUM",
                        "BATCH",
                        current.batchId(),
                        "按阶段序号排列时，当前批次时间早于上一阶段。",
                        "核对批次采集时间和阶段序号。",
                        evidence(
                                "previousStageId", previous.stageId(),
                                "currentStageId", current.stageId()));
            }
        }
        List<Long> positiveIntervals = new ArrayList<>();
        for (int index = 1; index < stages.size(); index++) {
            StageData previous = stages.get(index - 1);
            StageData current = stages.get(index);
            if (previous.collectedAt() == null || current.collectedAt() == null) {
                continue;
            }
            long hours = Duration.between(previous.collectedAt(), current.collectedAt()).toHours();
            if (hours == 0) {
                addGap(
                        findings,
                        gaps,
                        "STAGE_SEQUENCE_SUSPECTED",
                        "MEDIUM",
                        "BATCH",
                        current.batchId(),
                        "相邻观测阶段采集时间完全相同，需要核对是否为重复阶段。",
                        "核对批次采集时间和阶段归属。",
                        evidence(
                                "previousStageId", previous.stageId(),
                                "currentStageId", current.stageId()));
            } else if (hours > 0) {
                positiveIntervals.add(hours);
            }
        }
        if (positiveIntervals.size() >= 2) {
            List<Long> sorted = positiveIntervals.stream().sorted().toList();
            long median = sorted.get(sorted.size() / 2);
            long latest = positiveIntervals.get(positiveIntervals.size() - 1);
            if (median > 0 && latest > median * 2 && latest >= 24 * 7) {
                StageData current = stages.get(stages.size() - 1);
                addGap(
                        findings,
                        gaps,
                        "MISSING_FOLLOW_UP_STAGE",
                        "MEDIUM",
                        "BATCH",
                        current.batchId(),
                        "最近阶段间隔 " + latest + " 小时，明显长于历史中位间隔 " + median + " 小时。",
                        "补充后续观测，或说明观测周期调整原因。",
                        Map.of("latestIntervalHours", latest, "medianIntervalHours", median));
            }
        }
        Set<String> locations =
                stages.stream()
                        .map(stage -> firstText(stage.locationName(), stage.baseName()))
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (locations.size() > 1) {
            StageData latest = stages.get(stages.size() - 1);
            addGap(
                    findings,
                    gaps,
                    "LOCATION_CHANGE_SUSPECTED",
                    "MEDIUM",
                    "BATCH",
                    latest.batchId(),
                    "连续观测阶段出现多个地点或基地名称，需确认是否仍为同一观测对象。",
                    "核对定位、基地和任务连续性；名称差异本身不代表实际迁移。",
                    Map.of("locationCount", locations.size(), "locations", locations));
        }
    }

    private void inspectImages(
            List<StageData> stages,
            AnalysisInput input,
            List<EvidenceFindingDraft> findings,
            List<EvidenceGapVO> gaps) {
        Map<Long, List<RecognitionToolData.ImageOverview>> unrecognized =
                byBatch(input.unrecognizedImages());
        Map<Long, List<RecognitionToolData.ImageOverview>> lowConfidence =
                byBatch(input.lowConfidenceImages());
        for (StageData stage : stages) {
            Set<String> imageTypes =
                    stage.images().stream()
                            .map(DigitalLifeArchiveToolData.ImageEvidence::imageType)
                            .filter(StringUtils::hasText)
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet());
            for (String required : REQUIRED_IMAGE_TYPES) {
                if (!imageTypes.contains(required)) {
                    String type = missingImageFindingType(required);
                    addGap(
                            findings,
                            gaps,
                            type,
                            "LOW",
                            "BATCH",
                            stage.batchId(),
                    "该阶段缺少" + imageTypeLabel(required) + "类型图片证据。",
                            "后续采集时补充对应类型图片。",
                            evidence("batchId", stage.batchId(), "missingImageType", required));
                }
            }
            for (RecognitionToolData.ImageOverview image :
                    lowConfidence.getOrDefault(stage.batchId(), List.of())) {
                addGap(
                        findings,
                        gaps,
                        "RECOGNITION_AMBIGUOUS",
                        "MEDIUM",
                        "IMAGE",
                        image.imageId(),
                        "图片识别置信度偏低或需要人工复核。",
                        "补充清晰且类型明确的图片，并按现有流程人工复核。",
                        evidenceForRecognition(image));
            }
            for (RecognitionToolData.ImageOverview image :
                    unrecognized.getOrDefault(stage.batchId(), List.of())) {
                addGap(
                        findings,
                        gaps,
                        "INSUFFICIENT_EVIDENCE",
                        "MEDIUM",
                        "IMAGE",
                        image.imageId(),
                        "图片尚无可用识别结果，不能作为已确认的识别证据。",
                        "通过现有识别流程处理或安排人工复核。",
                        evidence("imageId", image.imageId(), "batchId", stage.batchId()));
            }
        }
    }

    private void inspectMetricTrends(
            List<MetricTrendVO> trends,
            List<EvidenceFindingDraft> findings,
            List<CrossModalFindingVO> crossModal) {
        for (MetricTrendVO trend : trends) {
            if (trend.consecutiveDeclineCount() >= 2 || trend.consecutiveRiseCount() >= 2) {
                List<Long> stageIds =
                        trend.points().stream().map(MetricTrendVO.MetricPointVO::stageId).toList();
                String direction = trend.consecutiveDeclineCount() >= 2 ? "连续下降" : "连续上升";
                addFinding(
                        findings,
                        "METRIC_TREND_CHANGE",
                        "MEDIUM",
                        "STAGE",
                        stageIds.get(stageIds.size() - 1),
                        trend.metricLabel() + "出现" + direction,
                        Map.of(
                                "metric", trend.metricCode(),
                                "stageIds", stageIds,
                                "values",
                                        trend.points().stream()
                                                .map(MetricTrendVO.MetricPointVO::value)
                                                .toList()),
                        "继续观测并结合环境、图片和人工复核信息判断。",
                        false);
                crossModal.add(
                        new CrossModalFindingVO(
                                "METRIC_TREND_CHANGE",
                                "MEDIUM",
                                stageIds,
                                List.of(trend.metricLabel() + direction),
                                List.of("该判断来自连续阶段实测值"),
                                "当前仅确认时间序列变化，不表示农业异常或因果关系。",
                                "阶段数量、设备误差和采样条件可能影响结果。"));
            }
            if (trend.points().size() >= 4 && isLastPointOutlier(trend.points())) {
                MetricTrendVO.MetricPointVO latest = trend.points().get(trend.points().size() - 1);
                addFinding(
                        findings,
                        "METRIC_OUTLIER",
                        "MEDIUM",
                        "STAGE",
                        latest.stageId(),
                        trend.metricLabel() + "当前值偏离历史分布",
                        Map.of(
                                "metric", trend.metricCode(),
                                "currentValue", latest.value(),
                                "historicalMean", nullable(trend.historicalMean()),
                                "method", trend.analysisMethod()),
                        "复核测量设备与采样条件，并增加后续阶段数据。",
                        false);
            }
        }
    }

    private void inspectCrossModal(
            List<StageData> stages,
            List<MetricTrendVO> trends,
            AnalysisInput input,
            List<EvidenceFindingDraft> findings,
            List<CrossModalFindingVO> crossModal) {
        MetricTrendVO moisture = trend(trends, "soilMoisture");
        MetricTrendVO height = trend(trends, "plantHeight");
        if (moisture != null
                && height != null
                && moisture.consecutiveDeclineCount() >= 2
                && height.changes().size() >= 2) {
            List<MetricTrendVO.MetricChangeVO> heightChanges = height.changes();
            BigDecimal previous = heightChanges.get(heightChanges.size() - 2).difference();
            BigDecimal latest = heightChanges.get(heightChanges.size() - 1).difference();
            if (latest.compareTo(previous) < 0) {
                List<Long> stageIds =
                        stages.stream().map(StageData::stageId).filter(Objects::nonNull).toList();
                crossModal.add(
                        new CrossModalFindingVO(
                                "METRIC_TREND_CHANGE",
                                "MEDIUM",
                                stageIds,
                                List.of("土壤湿度连续下降", "株高最近阶段增量低于上一阶段增量"),
                                List.of("相邻阶段指标数值"),
                                "两项变化在时间上同时出现。",
                                "现有证据不足以证明两项变化之间存在因果关系。"));
            }
        }
        Set<Long> missingRootBatches =
                stages.stream()
                        .filter(
                                stage ->
                                        stage.images().stream()
                                                .noneMatch(
                                                        image ->
                                                                "root"
                                                                        .equalsIgnoreCase(
                                                                                image.imageType())))
                        .map(StageData::batchId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        for (RecognitionToolData.ImageOverview image : safe(input.lowConfidenceImages().images())) {
            if (missingRootBatches.contains(image.batchId())) {
                crossModal.add(
                        new CrossModalFindingVO(
                                "RECOGNITION_AMBIGUOUS",
                                "MEDIUM",
                                stageIdForBatch(stages, image.batchId()),
                                List.of("当前阶段存在低置信度识别结果", "当前阶段缺少根部图片"),
                                List.of("识别置信度与图片类型覆盖"),
                                "识别不确定性与根部证据缺失同时存在。",
                                "不能据此推断药材品种或健康状态异常。"));
            }
        }
    }

    private StageEvidenceSummaryVO summary(StageData stage) {
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        if (stage.record() != null) {
            METRICS.forEach(
                    (code, definition) -> {
                        BigDecimal value = definition.value().apply(stage.record());
                        if (value != null) {
                            metrics.put(code, value);
                        }
                    });
        }
        Map<String, Integer> types = new LinkedHashMap<>();
        stage.images().stream()
                .map(DigitalLifeArchiveToolData.ImageEvidence::imageType)
                .filter(StringUtils::hasText)
                .forEach(type -> types.merge(type, 1, Integer::sum));
        List<String> facts = new ArrayList<>();
        facts.add("阶段包含 " + stage.images().size() + " 张图片记录。");
        facts.add("阶段包含 " + metrics.size() + " 项可用数值指标。");
        return new StageEvidenceSummaryVO(
                stage.stageId(),
                stage.batchId(),
                stage.sequence(),
                stage.collectedAt(),
                firstText(stage.locationName(), stage.baseName()),
                stage.reviewStatus(),
                Map.copyOf(metrics),
                stage.images().size(),
                Map.copyOf(types),
                List.copyOf(facts));
    }

    private AgentEvidenceExplanation fallbackExplanation(
            List<StageData> stages,
            List<MetricTrendVO> trends,
            List<EvidenceGapVO> gaps,
            List<CrossModalFindingVO> findings) {
        List<String> facts = new ArrayList<>();
        facts.add("本次分析覆盖 " + stages.size() + " 个连续观测阶段。");
        trends.stream()
                .filter(trend -> trend.observedCount() > 0)
                .limit(4)
                .forEach(
                        trend ->
                                facts.add(
                                        trend.metricLabel()
                                                + "包含 "
                                                + trend.observedCount()
                                                + " 个有效值。"));
        List<String> associations =
                findings.stream().map(CrossModalFindingVO::association).distinct().toList();
        List<String> uncertainties =
                findings.isEmpty()
                        ? List.of("当前仅完成规则与统计检查，尚不能形成因果或疾病诊断结论。")
                        : findings.stream()
                                .map(CrossModalFindingVO::uncertainty)
                                .distinct()
                                .toList();
        List<String> gapTexts = gaps.stream().map(EvidenceGapVO::description).distinct().toList();
        List<String> suggestions = gaps.stream().map(EvidenceGapVO::suggestion).distinct().toList();
        String confidence = stages.size() < 3 || !gaps.isEmpty() ? "LOW" : "MEDIUM";
        return new AgentEvidenceExplanation(
                "已完成基于真实指标、图片元数据、识别与审核状态的证据分析；结论不包含确定因果或疾病诊断。",
                List.copyOf(facts),
                associations,
                uncertainties,
                gapTexts,
                suggestions,
                confidence);
    }

    private void addGap(
            List<EvidenceFindingDraft> findings,
            List<EvidenceGapVO> gaps,
            String findingType,
            String severity,
            String targetType,
            Long targetId,
            String description,
            String suggestion,
            Map<String, Object> evidence) {
        gaps.add(
                new EvidenceGapVO(
                        findingType, severity, targetId, targetType, description, suggestion));
        addFinding(
                findings,
                findingType,
                severity,
                targetType,
                targetId,
                description,
                evidence,
                suggestion,
                false);
    }

    private void addFinding(
            List<EvidenceFindingDraft> findings,
            String findingType,
            String severity,
            String targetType,
            Long targetId,
            String description,
            Map<String, Object> evidence,
            String suggestion,
            boolean blocking) {
        findings.add(
                new EvidenceFindingDraft(
                        findingType,
                        severity,
                        targetType,
                        targetId,
                        description,
                        description,
                        evidence,
                        suggestion,
                        blocking));
    }

    private Map<Long, List<RecognitionToolData.ImageOverview>> byBatch(
            RecognitionToolData.TaskImageList images) {
        return safe(images.images()).stream()
                .filter(image -> image.batchId() != null)
                .collect(Collectors.groupingBy(RecognitionToolData.ImageOverview::batchId));
    }

    private Map<String, Object> evidenceForRecognition(RecognitionToolData.ImageOverview image) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("imageId", image.imageId());
        evidence.put("batchId", image.batchId());
        evidence.put("confidence", nullable(image.highestSimilarity()));
        evidence.put("needReview", Boolean.TRUE.equals(image.needReview()));
        if (image.localAtlasTopK() != null && image.localAtlasTopK().size() >= 2) {
            BigDecimal first = image.localAtlasTopK().get(0).similarity();
            BigDecimal second = image.localAtlasTopK().get(1).similarity();
            if (first != null && second != null) {
                evidence.put("topCandidateGap", first.subtract(second).abs());
            }
        }
        return Map.copyOf(evidence);
    }

    private String missingImageFindingType(String imageType) {
        return switch (imageType) {
            case "root" -> "MISSING_ROOT_IMAGE";
            case "leaf" -> "MISSING_LEAF_IMAGE";
            case "whole_plant" -> "MISSING_WHOLE_PLANT_IMAGE";
            case "environment" -> "MISSING_ENVIRONMENT_IMAGE";
            default -> "INSUFFICIENT_EVIDENCE";
        };
    }

    private String imageTypeLabel(String imageType) {
        return switch (imageType) {
            case "root" -> "根部";
            case "leaf" -> "叶片";
            case "whole_plant" -> "全株";
            case "environment" -> "现场环境";
            default -> imageType;
        };
    }

    private MetricTrendVO trend(List<MetricTrendVO> trends, String code) {
        return trends.stream()
                .filter(value -> code.equals(value.metricCode()))
                .findFirst()
                .orElse(null);
    }

    private List<Long> stageIdForBatch(List<StageData> stages, Long batchId) {
        return stages.stream()
                .filter(stage -> Objects.equals(batchId, stage.batchId()))
                .map(StageData::stageId)
                .filter(Objects::nonNull)
                .toList();
    }

    private String status(com.bdis.modules.assistant.agent.tool.dto.StatusValue status) {
        return status == null ? null : status.code();
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }

    private Map<String, Object> evidence(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), nullable(values[index + 1]));
        }
        return Map.copyOf(result);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static Map<String, MetricDefinition> metrics() {
        Map<String, MetricDefinition> metrics = new LinkedHashMap<>();
        metrics.put(
                "plantHeight",
                new MetricDefinition("株高", "cm", record -> record.growthMetrics().plantHeight()));
        metrics.put(
                "stemDiameter",
                new MetricDefinition("茎粗", "mm", record -> record.growthMetrics().stemDiameter()));
        metrics.put(
                "temperature",
                new MetricDefinition(
                        "温度", "°C", record -> record.environmentMetrics().temperature()));
        metrics.put(
                "humidity",
                new MetricDefinition("湿度", "%", record -> record.environmentMetrics().humidity()));
        metrics.put(
                "soilMoisture",
                new MetricDefinition(
                        "土壤湿度", "%", record -> record.environmentMetrics().soilMoisture()));
        metrics.put(
                "soilPh",
                new MetricDefinition(
                        "土壤 pH", null, record -> record.environmentMetrics().soilPh()));
        metrics.put(
                "light",
                new MetricDefinition("光照", "lx", record -> record.environmentMetrics().light()));
        return Map.copyOf(metrics);
    }

    public record AnalysisInput(
            Long agentTaskId,
            Long collectionTaskId,
            GrowthRecordToolData.RecordList growthRecords,
            DigitalLifeArchiveToolData.StageList stages,
            RecognitionToolData.TaskImageList unrecognizedImages,
            RecognitionToolData.TaskImageList lowConfidenceImages) {}

    public record EvidenceFindingDraft(
            String findingType,
            String severity,
            String targetType,
            Long targetId,
            String title,
            String description,
            Map<String, Object> evidence,
            String suggestion,
            boolean blocking) {}

    public record AnalysisOutcome(
            AgentEvidenceAnalysisVO analysis, List<EvidenceFindingDraft> findings) {}

    private record MetricDefinition(
            String label, String unit, Function<GrowthRecordToolData.Overview, BigDecimal> value) {}

    private record StageData(
            Long stageId,
            Long batchId,
            Integer sequence,
            LocalDateTime collectedAt,
            String locationName,
            String baseName,
            String reviewStatus,
            GrowthRecordToolData.Overview record,
            List<DigitalLifeArchiveToolData.ImageEvidence> images) {}
}
