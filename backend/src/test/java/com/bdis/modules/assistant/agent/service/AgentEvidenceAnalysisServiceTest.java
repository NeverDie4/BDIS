package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.AnalysisInput;
import com.bdis.modules.assistant.agent.service.AgentEvidenceAnalysisService.AnalysisOutcome;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.assistant.agent.vo.MetricTrendVO;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class AgentEvidenceAnalysisServiceTest {

    private final AgentEvidenceAnalysisService service = new AgentEvidenceAnalysisService();

    @Test
    void twoStagesUseAdjacentDifferenceOnlyAndMissingMetricIsNotZero() {
        AnalysisOutcome outcome =
                analyze(
                        List.of(record(1L, 11L, 10, null), record(2L, 12L, 12, null)),
                        List.of(
                                stage(1L, 11L, 1, allImages(11L)),
                                stage(2L, 12L, 2, allImages(12L))),
                        List.of(),
                        List.of());

        MetricTrendVO height = trend(outcome, "plantHeight");
        MetricTrendVO temperature = trend(outcome, "temperature");
        assertThat(outcome.analysis().metricTrends())
                .extracting(MetricTrendVO::metricCode)
                .doesNotContain("sampleWeight");
        assertThat(height.analysisMethod()).isEqualTo("ADJACENT_ONLY");
        assertThat(height.changes())
                .singleElement()
                .extracting("difference")
                .isEqualTo(new BigDecimal("2"));
        assertThat(temperature.points()).isEmpty();
        assertThat(temperature.missingStageIds()).containsExactly(1L, 2L);
        assertThat(outcome.findings())
                .anyMatch(finding -> "INSUFFICIENT_STAGE_COUNT".equals(finding.findingType()));
    }

    @Test
    void threeConsecutiveDeclinesCreateTrendFinding() {
        AnalysisOutcome outcome =
                analyze(
                        List.of(
                                record(1L, 11L, 10, 60),
                                record(2L, 12L, 11, 50),
                                record(3L, 13L, 11, 40)),
                        List.of(
                                stage(1L, 11L, 1, allImages(11L)),
                                stage(2L, 12L, 2, allImages(12L)),
                                stage(3L, 13L, 3, allImages(13L))),
                        List.of(),
                        List.of());

        MetricTrendVO moisture = trend(outcome, "soilMoisture");
        assertThat(moisture.consecutiveDeclineCount()).isEqualTo(2);
        assertThat(outcome.findings())
                .anyMatch(finding -> "METRIC_TREND_CHANGE".equals(finding.findingType()));
    }

    @Test
    void missingImageTypesAndLowConfidenceBecomeEvidenceGaps() {
        RecognitionToolData.ImageOverview ambiguous =
                new RecognitionToolData.ImageOverview(
                        99L,
                        11L,
                        "IMG-99",
                        "/api/public-files/99/content",
                        "leaf",
                        new StatusValue("success", "成功"),
                        List.of(),
                        new BigDecimal("0.45"),
                        null,
                        true,
                        null,
                        "local",
                        null,
                        LocalDateTime.now());

        AnalysisOutcome outcome =
                analyze(
                        List.of(record(1L, 11L, 10, 60)),
                        List.of(stage(1L, 11L, 1, List.of(image(99L, "leaf")))),
                        List.of(),
                        List.of(ambiguous));

        assertThat(outcome.analysis().evidenceGaps())
                .extracting("findingType")
                .contains(
                        "MISSING_ROOT_IMAGE",
                        "MISSING_WHOLE_PLANT_IMAGE",
                        "MISSING_ENVIRONMENT_IMAGE",
                        "RECOGNITION_AMBIGUOUS");
        assertThat(
                        outcome.analysis().evidenceGaps().stream()
                                .map(gap -> gap.description())
                                .toList())
                .contains(
                        "该阶段缺少根部类型图片证据。",
                        "该阶段缺少全株类型图片证据。",
                        "该阶段缺少现场环境类型图片证据。")
                .allMatch(
                        description ->
                                !description.contains("root")
                                        && !description.contains("whole_plant")
                                        && !description.contains("environment"));
    }

    @Test
    void analysisNeverReturnsImagePathOrCertainCausalConclusion() {
        AnalysisOutcome outcome =
                analyze(
                        List.of(record(1L, 11L, 10, 60)),
                        List.of(stage(1L, 11L, 1, allImages(11L))),
                        List.of(),
                        List.of());

        String text = outcome.analysis().toString();
        assertThat(text).doesNotContain("C:\\", "/uploads/", "已确认由", "已确诊", "一定会减产");
    }

    private AnalysisOutcome analyze(
            List<GrowthRecordToolData.Overview> records,
            List<DigitalLifeArchiveToolData.Stage> stages,
            List<RecognitionToolData.ImageOverview> unrecognized,
            List<RecognitionToolData.ImageOverview> lowConfidence) {
        return service.analyze(
                new AnalysisInput(
                        1L,
                        12L,
                        new GrowthRecordToolData.RecordList(12L, records),
                        new DigitalLifeArchiveToolData.StageList(12L, stages),
                        new RecognitionToolData.TaskImageList(12L, unrecognized),
                        new RecognitionToolData.TaskImageList(12L, lowConfidence)));
    }

    private MetricTrendVO trend(AnalysisOutcome outcome, String code) {
        return outcome.analysis().metricTrends().stream()
                .filter(value -> code.equals(value.metricCode()))
                .findFirst()
                .orElseThrow();
    }

    private GrowthRecordToolData.Overview record(
            Long recordId, Long batchId, int plantHeight, Integer soilMoisture) {
        return new GrowthRecordToolData.Overview(
                recordId,
                12L,
                "黄连任务",
                batchId,
                "阶段" + batchId,
                "黄连",
                "基地",
                "采集员",
                LocalDateTime.of(2026, 7, 1, 8, 0).plusDays(batchId - 11),
                new StatusValue("approved", "已通过"),
                new GrowthRecordToolData.EnvironmentMetrics(
                        null,
                        null,
                        soilMoisture == null ? null : BigDecimal.valueOf(soilMoisture),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                new GrowthRecordToolData.GrowthMetrics(
                        "生长期", BigDecimal.valueOf(plantHeight), null, null, null, null, null),
                4,
                List.of());
    }

    private DigitalLifeArchiveToolData.Stage stage(
            Long stageId,
            Long batchId,
            int sequence,
            List<DigitalLifeArchiveToolData.ImageEvidence> images) {
        return new DigitalLifeArchiveToolData.Stage(
                stageId,
                sequence,
                batchId,
                "阶段" + sequence,
                stageId,
                "生长期",
                LocalDateTime.of(2026, 7, 1, 8, 0).plusDays(sequence - 1),
                "采集员",
                "基地",
                "地点",
                new StatusValue("approved", "已通过"),
                new StatusValue("valid", "有效"),
                images.size(),
                images,
                false,
                null,
                null);
    }

    private List<DigitalLifeArchiveToolData.ImageEvidence> allImages(Long batchId) {
        return List.of(
                image(batchId * 10 + 1, "root"),
                image(batchId * 10 + 2, "leaf"),
                image(batchId * 10 + 3, "whole_plant"),
                image(batchId * 10 + 4, "environment"));
    }

    private DigitalLifeArchiveToolData.ImageEvidence image(Long imageId, String type) {
        return new DigitalLifeArchiveToolData.ImageEvidence(
                imageId, "/api/public-files/" + imageId + "/content", type, false);
    }
}
