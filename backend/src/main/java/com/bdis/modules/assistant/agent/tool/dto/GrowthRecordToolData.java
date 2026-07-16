package com.bdis.modules.assistant.agent.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class GrowthRecordToolData {

    private GrowthRecordToolData() {}

    public record Overview(
            Long recordId,
            Long taskId,
            String taskName,
            Long batchId,
            String batchName,
            String speciesName,
            String baseName,
            String collectorName,
            LocalDateTime collectedAt,
            StatusValue reviewStatus,
            EnvironmentMetrics environmentMetrics,
            GrowthMetrics growthMetrics,
            Integer imageCount,
            List<String> emptyFields) {}

    public record RecordList(Long taskId, List<Overview> records) {}

    public record AuditHistory(Long recordId, List<AuditEvent> events) {}

    public record EnvironmentMetrics(
            BigDecimal temperature,
            BigDecimal humidity,
            BigDecimal soilMoisture,
            BigDecimal soilPh,
            BigDecimal light,
            String soilType,
            String weather,
            BigDecimal longitude,
            BigDecimal latitude) {}

    public record GrowthMetrics(
            String growthStage,
            BigDecimal plantHeight,
            BigDecimal stemDiameter,
            String leafColor,
            String floweringStatus,
            BigDecimal sampleWeight,
            String growthEvaluation) {}

    /** 审核意见、操作人 ID 等内部信息不进入 Agent 工具结果。 */
    public record AuditEvent(
            String actionType,
            StatusValue beforeStatus,
            StatusValue afterStatus,
            String operatorRole,
            LocalDateTime operateTime) {}
}
