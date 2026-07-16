package com.bdis.modules.assistant.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FollowUpCollectionPlan(
    String objective,
    String recommendedTimeType,
    Integer recommendedAfterDays,
    LocalDateTime recommendedStartTime,
    LocalDateTime recommendedEndTime,
    List<RequiredMetricItem> requiredMetrics,
    List<RequiredImageItem> requiredImages,
    List<String> optionalItems,
    List<String> completionCriteria,
    List<Long> sourceFindingIds,
    String rationale,
    String uncertainty,
    String priority) {

  public record RequiredMetricItem(
      String metricCode,
      String metricName,
      Boolean required,
      String reason,
      String inputHint,
      String unit) {}

  public record RequiredImageItem(
      String imageType,
      String imageTypeName,
      Integer minCount,
      String shootingGuidance,
      String reason) {}
}
