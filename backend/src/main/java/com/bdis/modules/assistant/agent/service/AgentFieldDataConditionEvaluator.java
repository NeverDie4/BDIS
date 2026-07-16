package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ConditionDefinition;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ImageObservation;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ImageRequirement;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.MetricRequirement;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.mapper.AgentFieldDataMapper;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentFieldDataConditionEvaluator {

  private final AgentFieldDataMapper fieldDataMapper;

  public AgentFieldDataConditionEvaluator(AgentFieldDataMapper fieldDataMapper) {
    this.fieldDataMapper = fieldDataMapper;
  }

  public Snapshot evaluate(Long followUpTaskId, ConditionDefinition definition) {
    List<HerbBatchEntity> batches = fieldDataMapper.selectValidBatches(followUpTaskId);
    GrowthRecordEntity record = fieldDataMapper.selectLatestGrowthRecord(followUpTaskId);
    List<ImageObservation> images = fieldDataMapper.selectImages(followUpTaskId);
    List<String> completed = new ArrayList<>();
    List<String> missing = new ArrayList<>();

    completeOrMissing(!batches.isEmpty(), "已创建复测批次", "尚未创建有效复测批次", completed, missing);
    completeOrMissing(record != null, "已创建生长记录", "复测批次尚未创建生长记录", completed, missing);
    for (MetricRequirement requirement : safe(definition.requiredMetrics())) {
      if ("sampleWeight".equals(requirement.code())) {
        continue;
      }
      boolean present = record != null && metricPresent(record, requirement.code());
      completeOrMissing(
          present, "已填写" + requirement.name(), "缺少" + requirement.name(), completed, missing);
    }
    for (ImageRequirement requirement : safe(definition.requiredImages())) {
      long count =
          images.stream()
              .filter(image -> Objects.equals(requirement.type(), image.imageType()))
              .count();
      completeOrMissing(
          count >= requirement.minCount(),
          "已上传" + requirement.name(),
          requirement.name() + "图片至少需要 " + requirement.minCount() + " 张",
          completed,
          missing);
    }
    boolean recognitionReady =
        !images.isEmpty() && images.stream().allMatch(ImageObservation::recognitionReady);
    completeOrMissing(recognitionReady, "新上传图片已完成识别或转人工复核", "仍有新上传图片等待识别", completed, missing);
    if (definition.requireSubmitted()) {
      boolean submitted = submitted(record, batches);
      completeOrMissing(submitted, "复测数据已提交", "复测批次或生长记录尚未提交", completed, missing);
    }
    LocalDateTime latestDataTime = latestDataTime(batches, record, images);
    boolean afterSnapshot =
        latestDataTime != null
            && (definition.sourceSnapshotTime() == null
                || latestDataTime.isAfter(definition.sourceSnapshotTime()));
    completeOrMissing(afterSnapshot, "数据采集时间晚于原分析快照", "尚无晚于原分析快照的新数据", completed, missing);

    int total = completed.size() + missing.size();
    int progress = total == 0 ? 0 : completed.size() * 100 / total;
    return new Snapshot(
        missing.isEmpty(),
        batches.size(),
        record == null ? null : record.getId(),
        latestDataTime,
        List.copyOf(completed),
        List.copyOf(missing),
        progress);
  }

  private boolean metricPresent(GrowthRecordEntity record, String code) {
    return switch (code) {
      case "plantHeight" -> record.getPlantHeight() != null;
      case "stemDiameter" -> record.getStemDiameter() != null;
      case "temperature" -> record.getTemperature() != null;
      case "humidity" -> record.getHumidity() != null;
      case "soilMoisture" -> record.getSoilMoisture() != null;
      case "soilPh" -> record.getSoilPh() != null;
      case "light" -> record.getLight() != null;
      case "leafColor" -> StringUtils.hasText(record.getLeafColor());
      case "floweringStatus" -> StringUtils.hasText(record.getFloweringStatus());
      default -> false;
    };
  }

  private boolean submitted(GrowthRecordEntity record, List<HerbBatchEntity> batches) {
    if (record != null
        && (record.getSubmittedAt() != null
            || List.of("submitted", "approved", "archived").contains(record.getReviewStatus()))) {
      return true;
    }
    return batches.stream()
        .map(HerbBatchEntity::getBatchStatus)
        .anyMatch(status -> List.of("confirmed", "archived").contains(status));
  }

  private LocalDateTime latestDataTime(
      List<HerbBatchEntity> batches, GrowthRecordEntity record, List<ImageObservation> images) {
    List<LocalDateTime> values = new ArrayList<>();
    batches.forEach(
        batch -> {
          values.add(batch.getCollectStartTime());
          values.add(batch.getCreatedAt());
          values.add(batch.getUpdatedAt());
        });
    if (record != null) {
      values.add(record.getCollectedAt());
      values.add(record.getCreatedAt());
      values.add(record.getUpdatedAt());
    }
    images.forEach(image -> values.add(image.uploadTime()));
    return values.stream().filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
  }

  private void completeOrMissing(
      boolean condition,
      String completedText,
      String missingText,
      List<String> completed,
      List<String> missing) {
    (condition ? completed : missing).add(condition ? completedText : missingText);
  }

  private <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }
}
