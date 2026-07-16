package com.bdis.modules.assistant.agent.tool.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class DigitalLifeArchiveToolData {

    private DigitalLifeArchiveToolData() {}

    public record Overview(
            Long taskId,
            String archiveNo,
            String taskName,
            String speciesName,
            String baseName,
            Integer stageCount,
            Integer validStageCount,
            Integer imageCount,
            Integer narratedStageCount,
            Boolean publicVisible,
            Boolean qrCodeGenerated,
            StatusValue archiveStatus,
            LocalDateTime startTime,
            LocalDateTime endTime) {}

    public record StageList(Long taskId, List<Stage> stages) {}

    public record Stage(
            Long stageId,
            Integer sequence,
            Long batchId,
            String batchName,
            Long growthRecordId,
            String growthStage,
            LocalDateTime collectedAt,
            String collectorName,
            String baseName,
            String locationName,
            StatusValue reviewStatus,
            StatusValue dataStatus,
            Integer imageCount,
            List<ImageEvidence> images,
            Boolean narrationGenerated,
            String narrationSource,
            LocalDateTime narrationGeneratedTime) {}

    public record ImageEvidence(
            Long imageId, String imageUrl, String imageType, Boolean primaryImage) {}

    public record IntegrityStatus(
            Long taskId,
            Boolean verified,
            Integer eventCount,
            String rootHash,
            String hashVersion,
            LocalDateTime generatedTime,
            Integer failedSequence,
            String failedEventType,
            String message) {}
}
