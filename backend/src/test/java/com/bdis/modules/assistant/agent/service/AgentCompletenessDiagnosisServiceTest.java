package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.assistant.agent.service.AgentCompletenessDiagnosisService.DiagnosisInput;
import com.bdis.modules.assistant.agent.service.AgentCompletenessDiagnosisService.DiagnosisOutcome;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.tool.dto.DigitalLifeArchiveToolData;
import com.bdis.modules.assistant.agent.tool.dto.GrowthRecordToolData;
import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class AgentCompletenessDiagnosisServiceTest {

    private final AgentCompletenessDiagnosisService service =
            new AgentCompletenessDiagnosisService();

    @Test
    void threeCompleteApprovedStagesAreReadyForPublicArchive() {
        DiagnosisOutcome result = service.diagnose(completeInput());

        assertThat(result.archiveReadiness()).isEqualTo("READY_FOR_PUBLIC_ARCHIVE");
        assertThat(result.completenessScore()).isEqualTo(100);
        assertThat(result.stageCount()).isEqualTo(3);
        assertThat(result.validStageCount()).isEqualTo(3);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void missingGrowthRecordCreatesBlockingFindingButStillReturnsReport() {
        DiagnosisInput input = completeInput();
        List<DigitalLifeArchiveToolData.Stage> stages = new ArrayList<>(input.stages().stages());
        DigitalLifeArchiveToolData.Stage old = stages.get(1);
        stages.set(1, stage(old.sequence(), old.batchId(), null, old.images(), "approved"));
        input =
                withStages(
                        input,
                        stages,
                        List.of(
                                input.growthRecords().records().get(0),
                                input.growthRecords().records().get(2)));

        DiagnosisOutcome result = service.diagnose(input);

        assertThat(result.findings())
                .anyMatch(
                        finding ->
                                "MISSING_GROWTH_RECORD".equals(finding.findingType())
                                        && finding.targetId().equals(old.batchId()));
        assertThat(result.archiveReadiness()).isEqualTo("NOT_READY");
        assertThat(result.summary()).contains("数字档案完整度");
    }

    @Test
    void missingImageAndUnrecognizedImageCreateExpectedFindings() {
        DiagnosisInput input = completeInput();
        List<DigitalLifeArchiveToolData.Stage> stages = new ArrayList<>(input.stages().stages());
        DigitalLifeArchiveToolData.Stage old = stages.get(0);
        stages.set(
                0,
                stage(old.sequence(), old.batchId(), old.growthRecordId(), List.of(), "approved"));
        RecognitionToolData.ImageOverview unrecognized =
                new RecognitionToolData.ImageOverview(
                        999L,
                        102L,
                        "IMG-999",
                        "/api/files/999",
                        "whole_plant",
                        new StatusValue(null, null),
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        input =
                new DiagnosisInput(
                        input.agentTaskId(),
                        input.task(),
                        input.batches(),
                        input.growthRecords(),
                        new RecognitionToolData.TaskImageList(12L, List.of(unrecognized)),
                        input.lowConfidenceImages(),
                        input.archiveOverview(),
                        new DigitalLifeArchiveToolData.StageList(12L, stages),
                        input.integrity());

        DiagnosisOutcome result = service.diagnose(input);

        assertThat(result.findings())
                .extracting(AgentCompletenessDiagnosisService.FindingDraft::findingType)
                .contains("MISSING_IMAGE", "UNRECOGNIZED_IMAGE");
        assertThat(result.archiveReadiness()).isEqualTo("NOT_READY");
    }

    @Test
    void pendingAndRejectedReviewStatusesAreSeparated() {
        DiagnosisInput input = completeInput();
        List<GrowthRecordToolData.Overview> records =
                new ArrayList<>(input.growthRecords().records());
        records.set(0, growth(201L, 101L, "submitted", "苗期"));
        records.set(1, growth(202L, 102L, "rejected", "生长期"));
        input = withStages(input, input.stages().stages(), records);

        DiagnosisOutcome result = service.diagnose(input);

        assertThat(result.findings())
                .extracting(AgentCompletenessDiagnosisService.FindingDraft::findingType)
                .contains("REVIEW_PENDING", "REVIEW_REJECTED");
        assertThat(result.archiveReadiness()).isEqualTo("NOT_READY");
    }

    @Test
    void numericGrowthStageCreatesSuspectedTestDataFinding() {
        DiagnosisInput input = completeInput();
        List<GrowthRecordToolData.Overview> records =
                new ArrayList<>(input.growthRecords().records());
        records.set(0, growth(201L, 101L, "approved", "666"));
        input = withStages(input, input.stages().stages(), records);

        DiagnosisOutcome result = service.diagnose(input);

        assertThat(result.findings())
                .anyMatch(finding -> "SUSPECTED_TEST_DATA".equals(finding.findingType()));
    }

    @Test
    void completenessScoreIsDeterministic() {
        DiagnosisInput input = completeInput();

        DiagnosisOutcome first = service.diagnose(input);
        DiagnosisOutcome second = service.diagnose(input);

        assertThat(second.completenessScore()).isEqualTo(first.completenessScore());
        assertThat(second.scoreBreakdown()).isEqualTo(first.scoreBreakdown());
        assertThat(first.scoreBreakdown().values().stream().mapToInt(Integer::intValue).sum())
                .isEqualTo(100);
    }

    private DiagnosisInput completeInput() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 8, 0);
        CollectionTaskToolData.Overview task =
                new CollectionTaskToolData.Overview(
                        12L,
                        "TASK-12",
                        "黄连连续观测",
                        1L,
                        "黄连",
                        2L,
                        "标本园",
                        "重庆市南川区",
                        "采集员甲",
                        status("in_progress"),
                        start.minusDays(1),
                        start.plusDays(40),
                        3,
                        3,
                        3,
                        3,
                        0,
                        3,
                        0,
                        status("ready"));
        List<DigitalLifeArchiveToolData.Stage> stages =
                List.of(
                        stage(1, 101L, 201L, List.of(image(301L)), "approved"),
                        stage(2, 102L, 202L, List.of(image(302L)), "approved"),
                        stage(3, 103L, 203L, List.of(image(303L)), "approved"));
        List<GrowthRecordToolData.Overview> records =
                List.of(
                        growth(201L, 101L, "approved", "苗期"),
                        growth(202L, 102L, "approved", "生长期"),
                        growth(203L, 103L, "approved", "成熟期"));
        DigitalLifeArchiveToolData.Overview archive =
                new DigitalLifeArchiveToolData.Overview(
                        12L,
                        "DL-12",
                        "黄连连续观测",
                        "黄连",
                        "标本园",
                        3,
                        3,
                        3,
                        3,
                        false,
                        true,
                        status("ready"),
                        start,
                        start.plusDays(30));
        DigitalLifeArchiveToolData.IntegrityStatus integrity =
                new DigitalLifeArchiveToolData.IntegrityStatus(
                        12L, true, 10, "abc", "SHA-256:1", start, null, null, "校验通过");
        return new DiagnosisInput(
                1L,
                task,
                new CollectionTaskToolData.BatchList(12L, List.of()),
                new GrowthRecordToolData.RecordList(12L, records),
                new RecognitionToolData.TaskImageList(12L, List.of()),
                new RecognitionToolData.TaskImageList(12L, List.of()),
                archive,
                new DigitalLifeArchiveToolData.StageList(12L, stages),
                integrity);
    }

    private DiagnosisInput withStages(
            DiagnosisInput input,
            List<DigitalLifeArchiveToolData.Stage> stages,
            List<GrowthRecordToolData.Overview> records) {
        return new DiagnosisInput(
                input.agentTaskId(),
                input.task(),
                input.batches(),
                new GrowthRecordToolData.RecordList(12L, records),
                input.unrecognizedImages(),
                input.lowConfidenceImages(),
                new DigitalLifeArchiveToolData.Overview(
                        12L,
                        "DL-12",
                        "黄连连续观测",
                        "黄连",
                        "标本园",
                        stages.size(),
                        (int)
                                records.stream()
                                        .filter(
                                                record ->
                                                        "approved"
                                                                .equals(
                                                                        record.reviewStatus()
                                                                                .code()))
                                        .count(),
                        stages.stream().mapToInt(stage -> stage.images().size()).sum(),
                        stages.size(),
                        false,
                        true,
                        status("ready"),
                        LocalDateTime.of(2026, 6, 1, 8, 0),
                        LocalDateTime.of(2026, 7, 1, 8, 0)),
                new DigitalLifeArchiveToolData.StageList(12L, stages),
                input.integrity());
    }

    private DigitalLifeArchiveToolData.Stage stage(
            int sequence,
            Long batchId,
            Long recordId,
            List<DigitalLifeArchiveToolData.ImageEvidence> images,
            String reviewStatus) {
        return new DigitalLifeArchiveToolData.Stage(
                batchId,
                sequence,
                batchId,
                "批次" + sequence,
                recordId,
                "阶段" + sequence,
                LocalDateTime.of(2026, 6, sequence * 5, 8, 0),
                "采集员甲",
                "标本园",
                "重庆市南川区",
                status(reviewStatus),
                status("valid"),
                images.size(),
                images,
                true,
                "rule",
                LocalDateTime.of(2026, 6, sequence * 5, 9, 0));
    }

    private DigitalLifeArchiveToolData.ImageEvidence image(Long id) {
        return new DigitalLifeArchiveToolData.ImageEvidence(
                id, "/api/public-files/" + id + "/content", "whole_plant", true);
    }

    private GrowthRecordToolData.Overview growth(
            Long recordId, Long batchId, String reviewStatus, String growthStage) {
        return new GrowthRecordToolData.Overview(
                recordId,
                12L,
                "黄连连续观测",
                batchId,
                "批次" + batchId,
                "黄连",
                "标本园",
                "采集员甲",
                LocalDateTime.of(2026, 6, Math.toIntExact(batchId - 96), 8, 0),
                status(reviewStatus),
                new GrowthRecordToolData.EnvironmentMetrics(
                        decimal("24.5"),
                        decimal("70"),
                        decimal("45"),
                        decimal("6.5"),
                        decimal("12000"),
                        "壤土",
                        "晴",
                        decimal("106.5"),
                        decimal("29.5")),
                new GrowthRecordToolData.GrowthMetrics(
                        growthStage,
                        decimal("12.5"),
                        decimal("1.2"),
                        "绿色",
                        "未开花",
                        decimal("20"),
                        "生长正常"),
                1,
                List.of());
    }

    private StatusValue status(String code) {
        return new StatusValue(code, code);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
