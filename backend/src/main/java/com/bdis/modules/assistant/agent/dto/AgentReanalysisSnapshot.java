package com.bdis.modules.assistant.agent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentReanalysisSnapshot(
    List<Stage> stages, List<Finding> findings, int completenessScore, String archiveReadiness) {

  public record Stage(
      Long taskId,
      Long batchId,
      LocalDateTime collectedAt,
      Long growthRecordId,
      String reviewStatus,
      Map<String, Object> metrics,
      Map<String, Integer> imageTypeCounts,
      int imageCount,
      int recognizedImageCount) {}

  public record Finding(Long id, String type, String severity, String status) {}

  public record MetricChange(
      String metricCode, BigDecimal beforeValue, BigDecimal afterValue, BigDecimal difference) {}

  public record Built(AgentReanalysisSnapshot snapshot, String canonicalJson, String sha256) {}
}
