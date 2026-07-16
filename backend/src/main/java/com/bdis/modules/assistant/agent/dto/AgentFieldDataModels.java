package com.bdis.modules.assistant.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class AgentFieldDataModels {

  private AgentFieldDataModels() {}

  public record ConditionDefinition(
      List<MetricRequirement> requiredMetrics,
      List<ImageRequirement> requiredImages,
      boolean requireSubmitted,
      LocalDateTime sourceSnapshotTime) {}

  public record MetricRequirement(String code, String name) {}

  public record ImageRequirement(String type, String name, int minCount) {}

  public record Snapshot(
      boolean satisfied,
      int batchCount,
      Long growthRecordId,
      LocalDateTime latestDataTime,
      List<String> completedRequirements,
      List<String> missingRequirements,
      int progressPercent) {}

  public record ImageObservation(
      Long imageId, String imageType, boolean recognitionReady, LocalDateTime uploadTime) {}

  public record SnapshotImageObservation(
      Long batchId,
      Long imageId,
      String imageType,
      boolean recognitionReady,
      LocalDateTime uploadTime) {}
}
