package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.SnapshotImageObservation;
import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.Built;
import com.bdis.modules.assistant.agent.entity.AgentFindingEntity;
import com.bdis.modules.assistant.agent.mapper.AgentFieldDataMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentReanalysisSnapshotBuilderTest {

  @Mock private AgentFieldDataMapper fieldDataMapper;
  @Mock private AgentFindingMapper findingMapper;

  private AgentReanalysisSnapshotBuilder builder;

  @BeforeEach
  void setUp() {
    builder =
        new AgentReanalysisSnapshotBuilder(
            fieldDataMapper, findingMapper, new ObjectMapper().findAndRegisterModules());
  }

  @Test
  void normalizedSnapshotCombinesStagesAndProducesStableSha256WithoutBinaryData() {
    prepareTask(12L, 101L, LocalDateTime.of(2026, 7, 1, 8, 0));
    prepareTask(200L, 201L, LocalDateTime.of(2026, 7, 10, 8, 0));
    AgentFindingEntity finding = new AgentFindingEntity();
    finding.setId(1L);
    finding.setFindingType("MISSING_ROOT_IMAGE");
    finding.setSeverity("HIGH");
    finding.setStatus("RESOLVED");
    when(findingMapper.selectByTaskId(1L)).thenReturn(List.of(finding));

    Built first = builder.build(1L, List.of(12L, 200L));
    Built second = builder.build(1L, List.of(200L, 12L));

    assertThat(first.snapshot().stages()).hasSize(2);
    assertThat(first.snapshot().completenessScore()).isEqualTo(100);
    assertThat(first.snapshot().archiveReadiness()).isEqualTo("READY");
    assertThat(first.sha256()).hasSize(64).isEqualTo(second.sha256());
    assertThat(first.canonicalJson())
        .doesNotContain(
            "featureVector", "imageUrl", "collectorName", "binary", "sampleWeight");
  }

  private void prepareTask(Long taskId, Long batchId, LocalDateTime time) {
    HerbBatchEntity batch = new HerbBatchEntity();
    batch.setId(batchId);
    batch.setTaskId(taskId);
    batch.setCollectStartTime(time);
    GrowthRecordEntity record = new GrowthRecordEntity();
    record.setId(batchId + 1000);
    record.setTaskId(taskId);
    record.setBatchId(batchId);
    record.setCollectedAt(time);
    record.setPlantHeight(new BigDecimal("10.5"));
    record.setSampleWeight(new BigDecimal("12.5"));
    record.setReviewStatus("approved");
    SnapshotImageObservation image =
        new SnapshotImageObservation(batchId, batchId + 2000, "root", true, time);
    when(fieldDataMapper.selectValidBatches(taskId)).thenReturn(List.of(batch));
    when(fieldDataMapper.selectGrowthRecords(taskId)).thenReturn(List.of(record));
    when(fieldDataMapper.selectSnapshotImages(taskId)).thenReturn(List.of(image));
  }
}
