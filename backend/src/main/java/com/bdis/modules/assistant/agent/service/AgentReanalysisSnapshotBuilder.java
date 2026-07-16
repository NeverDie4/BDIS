package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.SnapshotImageObservation;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Built;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Finding;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Stage;
import com.bdis.modules.assistant.agent.entity.AgentFindingEntity;
import com.bdis.modules.assistant.agent.mapper.AgentFieldDataMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentReanalysisSnapshotBuilder {

  private final AgentFieldDataMapper fieldDataMapper;
  private final AgentFindingMapper findingMapper;
  private final ObjectMapper canonicalMapper;

  public AgentReanalysisSnapshotBuilder(
      AgentFieldDataMapper fieldDataMapper,
      AgentFindingMapper findingMapper,
      ObjectMapper objectMapper) {
    this.fieldDataMapper = fieldDataMapper;
    this.findingMapper = findingMapper;
    this.canonicalMapper = objectMapper.copy();
    this.canonicalMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    this.canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  public Built build(Long agentTaskId, List<Long> collectionTaskIds) {
    List<Stage> stages = new ArrayList<>();
    for (Long taskId : collectionTaskIds.stream().filter(Objects::nonNull).distinct().toList()) {
      stages.addAll(stages(taskId));
    }
    stages.sort(
        Comparator.comparing(Stage::collectedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Stage::batchId));
    List<Finding> findings =
        findingMapper.selectByTaskId(agentTaskId).stream()
            .map(this::finding)
            .sorted(Comparator.comparing(Finding::id))
            .toList();
    int score = completeness(stages);
    String readiness = readiness(stages, findings, score);
    AgentReanalysisSnapshot snapshot =
        new AgentReanalysisSnapshot(List.copyOf(stages), findings, score, readiness);
    String json = canonicalJson(snapshot);
    return new Built(snapshot, json, sha256(json));
  }

  private List<Stage> stages(Long taskId) {
    Map<Long, GrowthRecordEntity> records =
        fieldDataMapper.selectGrowthRecords(taskId).stream()
            .filter(record -> record.getBatchId() != null)
            .collect(
                Collectors.toMap(
                    GrowthRecordEntity::getBatchId, Function.identity(), (left, right) -> right));
    Map<Long, List<SnapshotImageObservation>> images =
        fieldDataMapper.selectSnapshotImages(taskId).stream()
            .collect(Collectors.groupingBy(SnapshotImageObservation::batchId));
    return fieldDataMapper.selectValidBatches(taskId).stream()
        .map(batch -> stage(taskId, batch, records.get(batch.getId()), images.get(batch.getId())))
        .toList();
  }

  private Stage stage(
      Long taskId,
      HerbBatchEntity batch,
      GrowthRecordEntity record,
      List<SnapshotImageObservation> observations) {
    List<SnapshotImageObservation> images = observations == null ? List.of() : observations;
    Map<String, Integer> imageCounts = new TreeMap<>();
    images.forEach(image -> imageCounts.merge(text(image.imageType(), "unknown"), 1, Integer::sum));
    LocalDateTime collectedAt =
        record != null && record.getCollectedAt() != null
            ? record.getCollectedAt()
            : batch.getCollectStartTime() != null
                ? batch.getCollectStartTime()
                : batch.getCreatedAt();
    return new Stage(
        taskId,
        batch.getId(),
        collectedAt,
        record == null ? null : record.getId(),
        record == null ? null : record.getReviewStatus(),
        record == null ? Map.of() : metrics(record),
        Map.copyOf(imageCounts),
        images.size(),
        (int) images.stream().filter(SnapshotImageObservation::recognitionReady).count());
  }

  private Map<String, Object> metrics(GrowthRecordEntity record) {
    Map<String, Object> values = new LinkedHashMap<>();
    put(values, "plantHeight", record.getPlantHeight());
    put(values, "stemDiameter", record.getStemDiameter());
    put(values, "temperature", record.getTemperature());
    put(values, "humidity", record.getHumidity());
    put(values, "soilMoisture", record.getSoilMoisture());
    put(values, "soilPh", record.getSoilPh());
    put(values, "light", record.getLight());
    put(values, "leafColor", record.getLeafColor());
    put(values, "floweringStatus", record.getFloweringStatus());
    return Map.copyOf(values);
  }

  private int completeness(List<Stage> stages) {
    if (stages.isEmpty()) {
      return 0;
    }
    int total = 0;
    for (Stage stage : stages) {
      total += stage.growthRecordId() == null ? 0 : 25;
      total += stage.metrics().isEmpty() ? 0 : 20;
      total += stage.imageCount() == 0 ? 0 : 20;
      total +=
          stage.imageCount() > 0 && stage.imageCount() == stage.recognizedImageCount() ? 15 : 0;
      total += "approved".equals(stage.reviewStatus()) ? 20 : 0;
    }
    return total / stages.size();
  }

  private String readiness(List<Stage> stages, List<Finding> findings, int score) {
    boolean blocking =
        findings.stream()
            .anyMatch(
                finding ->
                    "OPEN".equals(finding.status())
                        && List.of("HIGH", "CRITICAL").contains(finding.severity()));
    boolean stageReady =
        stages.size() >= 2
            && stages.stream()
                .allMatch(
                    stage ->
                        stage.growthRecordId() != null
                            && stage.imageCount() > 0
                            && stage.imageCount() == stage.recognizedImageCount()
                            && "approved".equals(stage.reviewStatus()));
    return score >= 85 && stageReady && !blocking ? "READY" : "NOT_READY";
  }

  private Finding finding(AgentFindingEntity entity) {
    return new Finding(
        entity.getId(), entity.getFindingType(), entity.getSeverity(), entity.getStatus());
  }

  private void put(Map<String, Object> values, String key, Object value) {
    if (value instanceof String stringValue && !StringUtils.hasText(stringValue)) {
      return;
    }
    if (value != null) {
      values.put(key, value);
    }
  }

  private String text(String value, String fallback) {
    return StringUtils.hasText(value) ? value : fallback;
  }

  private String canonicalJson(Object value) {
    try {
      return canonicalMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("Agent 分析快照序列化失败");
    }
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }
}
