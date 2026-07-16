package com.bdis.modules.assistant.agent.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MetricTrendVO(
        String metricCode,
        String metricLabel,
        String unit,
        Integer observedCount,
        String analysisMethod,
        List<MetricPointVO> points,
        List<Long> missingStageIds,
        List<MetricChangeVO> changes,
        Integer consecutiveRiseCount,
        Integer consecutiveDeclineCount,
        BigDecimal historicalMean,
        BigDecimal currentDeviationPercent,
        List<String> observations) {

    public record MetricPointVO(
            Long stageId,
            Long batchId,
            Integer sequence,
            LocalDateTime collectedAt,
            BigDecimal value) {}

    public record MetricChangeVO(
            Long fromStageId,
            Long toStageId,
            BigDecimal difference,
            BigDecimal changeRatePercent) {}
}
