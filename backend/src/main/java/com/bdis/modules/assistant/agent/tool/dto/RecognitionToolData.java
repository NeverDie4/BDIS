package com.bdis.modules.assistant.agent.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class RecognitionToolData {

    private RecognitionToolData() {}

    public record ImageOverview(
            Long imageId,
            Long batchId,
            String imageCode,
            String imageUrl,
            String imageType,
            StatusValue recognitionStatus,
            List<Candidate> localAtlasTopK,
            BigDecimal highestSimilarity,
            String aiAssistedResult,
            Boolean needReview,
            String finalSpeciesName,
            String recognitionSource,
            String errorMessage,
            LocalDateTime recognitionTime) {}

    public record BatchOverview(
            Long batchId,
            Integer imageCount,
            Integer recognizedCount,
            Integer unrecognizedCount,
            Integer lowConfidenceCount,
            Integer needReviewCount,
            List<ImageOverview> images) {}

    public record TaskImageList(Long taskId, List<ImageOverview> images) {}

    public record Candidate(
            Long speciesId,
            String speciesName,
            BigDecimal confidence,
            BigDecimal similarity,
            Integer rank) {}
}
