package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ConditionDefinition;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ImageObservation;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ImageRequirement;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.MetricRequirement;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.mapper.AgentFieldDataMapper;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentFieldDataConditionEvaluatorTest {

  @Mock private AgentFieldDataMapper fieldDataMapper;

  private AgentFieldDataConditionEvaluator evaluator;
  private final LocalDateTime sourceTime = LocalDateTime.of(2026, 7, 1, 8, 0);

  @BeforeEach
  void setUp() {
    evaluator = new AgentFieldDataConditionEvaluator(fieldDataMapper);
  }

  @Test
  void incompleteMetricsAndImagesRemainPartialAndCannotResume() {
    HerbBatchEntity batch = batch();
    GrowthRecordEntity record = record();
    when(fieldDataMapper.selectValidBatches(200L)).thenReturn(List.of(batch));
    when(fieldDataMapper.selectLatestGrowthRecord(200L)).thenReturn(record);
    when(fieldDataMapper.selectImages(200L)).thenReturn(List.of());

    Snapshot result = evaluator.evaluate(200L, definition());

    assertThat(result.satisfied()).isFalse();
    assertThat(result.completedRequirements()).contains("已创建复测批次", "已创建生长记录");
    assertThat(result.missingRequirements()).contains("缺少土壤 pH", "根部图片至少需要 1 张", "仍有新上传图片等待识别");
    assertThat(result.missingRequirements()).doesNotContain("缺少采样重量");
  }

  @Test
  void requiredMetricsImagesRecognitionSubmissionAndNewTimeSatisfyCompositeCondition() {
    HerbBatchEntity batch = batch();
    batch.setBatchStatus("submitted");
    GrowthRecordEntity record = record();
    record.setSoilPh(new BigDecimal("6.50"));
    record.setReviewStatus("submitted");
    record.setSubmittedAt(sourceTime.plusDays(2));
    when(fieldDataMapper.selectValidBatches(200L)).thenReturn(List.of(batch));
    when(fieldDataMapper.selectLatestGrowthRecord(200L)).thenReturn(record);
    when(fieldDataMapper.selectImages(200L))
        .thenReturn(List.of(new ImageObservation(10L, "root", true, sourceTime.plusDays(2))));

    Snapshot result = evaluator.evaluate(200L, definition());

    assertThat(result.satisfied()).isTrue();
    assertThat(result.missingRequirements()).isEmpty();
    assertThat(result.progressPercent()).isEqualTo(100);
  }

  private ConditionDefinition definition() {
    return new ConditionDefinition(
        List.of(
            new MetricRequirement("soilPh", "土壤 pH"),
            new MetricRequirement("sampleWeight", "采样重量")),
        List.of(new ImageRequirement("root", "根部", 1)),
        true,
        sourceTime);
  }

  private HerbBatchEntity batch() {
    HerbBatchEntity value = new HerbBatchEntity();
    value.setId(20L);
    value.setBatchStatus("collecting");
    value.setCreatedAt(sourceTime.plusDays(1));
    return value;
  }

  private GrowthRecordEntity record() {
    GrowthRecordEntity value = new GrowthRecordEntity();
    value.setId(30L);
    value.setCollectedAt(sourceTime.plusDays(1));
    value.setReviewStatus("draft");
    return value;
  }
}
