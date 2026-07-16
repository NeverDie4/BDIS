package com.bdis.modules.assistant.agent.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record StageEvidenceSummaryVO(
        Long stageId,
        Long batchId,
        Integer sequence,
        LocalDateTime collectedAt,
        String locationName,
        String reviewStatus,
        Map<String, BigDecimal> metrics,
        Integer imageCount,
        Map<String, Integer> imageTypeCounts,
        List<String> facts) {}
