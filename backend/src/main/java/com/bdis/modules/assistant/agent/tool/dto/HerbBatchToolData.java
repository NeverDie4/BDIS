package com.bdis.modules.assistant.agent.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class HerbBatchToolData {

    private HerbBatchToolData() {}

    public record Overview(
            Long batchId,
            String batchCode,
            String batchName,
            Long taskId,
            String taskName,
            Long speciesId,
            String speciesName,
            Long baseId,
            String baseName,
            String collectorName,
            LocalDateTime collectStartTime,
            LocalDateTime collectEndTime,
            StatusValue batchStatus,
            Long growthRecordId,
            StatusValue growthRecordStatus,
            Integer imageCount,
            Integer identifiedCount,
            Integer pendingRecognitionReviewCount,
            Boolean canSubmitReview) {}

    public record CompletenessSnapshot(
            Long batchId,
            Boolean hasGrowthRecord,
            Integer imageCount,
            Integer identifiedCount,
            Integer unrecognizedCount,
            Integer pendingRecognitionReviewCount,
            List<String> emptyGrowthFields,
            Boolean canSubmitReview) {}

    public record ImageList(Long batchId, List<ImageSummary> images) {}

    public record ImageSummary(
            Long imageId,
            String imageCode,
            String imageUrl,
            String imageType,
            String imageRole,
            LocalDateTime collectTime,
            Boolean primaryImage,
            Long recognitionResultId,
            String recognizedSpeciesName,
            BigDecimal confidence,
            Boolean needReview,
            StatusValue reviewStatus,
            String recognitionSource) {}
}
