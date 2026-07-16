package com.bdis.modules.assistant.agent.tool.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class CollectionTaskToolData {

    private CollectionTaskToolData() {}

    public record Overview(
            Long taskId,
            String taskCode,
            String taskName,
            Long speciesId,
            String speciesName,
            Long baseId,
            String baseName,
            String collectPlace,
            String collectorName,
            StatusValue taskStatus,
            LocalDateTime plannedStartTime,
            LocalDateTime plannedEndTime,
            Integer batchCount,
            Integer growthRecordCount,
            Integer imageCount,
            Integer identifiedImageCount,
            Integer pendingReviewCount,
            Integer approvedCount,
            Integer rejectedCount,
            StatusValue archiveStatus) {}

    public record Progress(
            Long taskId,
            Integer batchCount,
            Integer growthRecordCount,
            Integer imageCount,
            Integer identifiedImageCount,
            Integer pendingReviewCount,
            Integer approvedCount,
            Integer rejectedCount,
            Integer validStageCount,
            Integer stageCount) {}

    public record BatchList(Long taskId, List<BatchSummary> batches) {}

    public record BatchSummary(
            Long batchId,
            String batchCode,
            String batchName,
            StatusValue batchStatus,
            LocalDateTime collectStartTime,
            LocalDateTime collectEndTime,
            Long growthRecordId,
            StatusValue reviewStatus,
            Integer imageCount) {}
}
