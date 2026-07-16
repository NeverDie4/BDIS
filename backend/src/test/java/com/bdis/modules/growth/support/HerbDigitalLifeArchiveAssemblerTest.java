package com.bdis.modules.growth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.bdis.modules.herb.vo.HerbImageVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HerbDigitalLifeArchiveAssemblerTest {

    private final HerbDigitalLifeArchiveAssembler assembler = new HerbDigitalLifeArchiveAssembler();

    @Test
    void sortsStagesByResolvedCollectionTimeThenBatchId() {
        HerbBatchVO batch30 = batch(30L, LocalDateTime.of(2026, 7, 3, 9, 0));
        HerbBatchVO batch20 = batch(20L, LocalDateTime.of(2026, 7, 2, 9, 0));
        HerbBatchVO batch10 = batch(10L, LocalDateTime.of(2026, 7, 1, 9, 0));
        GrowthRecordVO record20 = growthRecord(200L, LocalDateTime.of(2026, 7, 1, 9, 0));

        HerbDigitalLifeArchiveVO archive =
                assembler.assemble(
                        task(),
                        List.of(batch30, batch20, batch10),
                        Map.of(20L, record20),
                        Map.of(),
                        Map.of());

        assertThat(archive.getStages())
                .extracting(HerbDigitalLifeStageVO::getBatchId)
                .containsExactly(10L, 20L, 30L);
        assertThat(archive.getStages())
                .extracting(HerbDigitalLifeStageVO::getSequence)
                .containsExactly(1, 2, 3);
        assertThat(archive.getStartTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(archive.getEndTime()).isEqualTo(LocalDateTime.of(2026, 7, 3, 9, 0));
    }

    @Test
    void mapsMetricsRecognitionAndStageDataStatuses() {
        HerbBatchVO completeBatch = batch(1L, LocalDateTime.of(2026, 7, 1, 9, 0));
        completeBatch.setAvgSimilarity(new BigDecimal("0.91"));
        HerbBatchVO missingGrowthBatch = batch(2L, LocalDateTime.of(2026, 7, 2, 9, 0));
        HerbBatchVO missingImagesBatch = batch(3L, LocalDateTime.of(2026, 7, 3, 9, 0));
        GrowthRecordVO completeRecord = growthRecord(101L, null);
        completeRecord.setPlantHeight(new BigDecimal("12.30"));
        completeRecord.setStemDiameter(new BigDecimal("1.20"));
        completeRecord.setLeafColor("深绿");
        completeRecord.setFloweringStatus("未开花");
        completeRecord.setTemperature(new BigDecimal("23.50"));
        completeRecord.setHumidity(new BigDecimal("68.00"));
        completeRecord.setSoilMoisture(new BigDecimal("41.00"));
        completeRecord.setSoilPh(new BigDecimal("6.50"));
        completeRecord.setLight(new BigDecimal("8500"));
        completeRecord.setGrowthEvaluation("长势良好");
        completeRecord.setRemark("现场记录");
        GrowthRecordVO missingImagesRecord = growthRecord(103L, null);
        HerbImageVO image = image(1001L, "leaf");
        HerbBatchImageVO binding = binding(1001L, true);
        binding.setIdentificationResultId(501L);
        binding.setFinalSpeciesName("黄连");
        binding.setFinalConfidence(new BigDecimal("0.95"));
        binding.setResultSource("feature_match");
        binding.setNeedReview(false);
        binding.setSuggestion("识别结果可信");

        HerbDigitalLifeArchiveVO archive =
                assembler.assemble(
                        task(),
                        List.of(missingImagesBatch, missingGrowthBatch, completeBatch),
                        Map.of(1L, completeRecord, 3L, missingImagesRecord),
                        Map.of(1L, List.of(image), 2L, List.of(image)),
                        Map.of(1L, List.of(binding), 2L, List.of(binding)));

        HerbDigitalLifeStageVO completeStage = archive.getStages().get(0);
        assertThat(completeStage.getDataStatus()).isEqualTo("complete");
        assertThat(completeStage.getMetrics().getPlantHeight()).isEqualByComparingTo("12.30");
        assertThat(completeStage.getMetrics().getGrowthEvaluation()).isEqualTo("长势良好");
        assertThat(completeStage.getImages().get(0).getImageTypeName()).isEqualTo("叶片");
        assertThat(completeStage.getImages().get(0).getPrimaryImage()).isTrue();
        assertThat(completeStage.getRecognition().getSpeciesName()).isEqualTo("黄连");
        assertThat(completeStage.getRecognition().getSimilarity()).isEqualByComparingTo("0.91");
        assertThat(archive.getStages().get(1).getDataStatus()).isEqualTo("missing_growth_record");
        assertThat(archive.getStages().get(2).getDataStatus()).isEqualTo("missing_images");
        assertThat(archive.getValidStageCount()).isEqualTo(1);
        assertThat(archive.getImageCount()).isEqualTo(2);
        assertThat(archive.getArchiveStatus()).isEqualTo("incomplete");
    }

    @Test
    void mapsEverySupportedImageTypeToChinese() {
        assertThat(HerbDigitalLifeImageType.displayNameOf("leaf")).isEqualTo("叶片");
        assertThat(HerbDigitalLifeImageType.displayNameOf("stem")).isEqualTo("茎部");
        assertThat(HerbDigitalLifeImageType.displayNameOf("root")).isEqualTo("根部");
        assertThat(HerbDigitalLifeImageType.displayNameOf("flower")).isEqualTo("花");
        assertThat(HerbDigitalLifeImageType.displayNameOf("fruit")).isEqualTo("果实");
        assertThat(HerbDigitalLifeImageType.displayNameOf("whole_plant")).isEqualTo("整株");
        assertThat(HerbDigitalLifeImageType.displayNameOf("medicinal_part")).isEqualTo("药用部位");
        assertThat(HerbDigitalLifeImageType.displayNameOf("environment")).isEqualTo("生长环境");
        assertThat(HerbDigitalLifeImageType.displayNameOf("other")).isEqualTo("其他");
        assertThat(HerbDigitalLifeImageType.displayNameOf(null)).isEqualTo("其他");
    }

    @Test
    void handlesNullInputsAndEmptyFieldsWithoutNullPointerException() {
        HerbBatchVO emptyBatch = new HerbBatchVO();
        HerbDigitalLifeArchiveVO archive =
                assembler.assemble(
                        new HerbCollectionTaskVO(), List.of(emptyBatch), null, null, null);

        assertThat(archive.getStages()).hasSize(1);
        assertThat(archive.getStageCount()).isEqualTo(1);
        assertThat(archive.getValidStageCount()).isZero();
        assertThat(archive.getImageCount()).isZero();
        assertThat(archive.getStartTime()).isNull();
        assertThat(archive.getEndTime()).isNull();
        assertThat(archive.getArchiveStatus()).isEqualTo("incomplete");
        assertThat(archive.getStages().get(0).getDataStatus()).isEqualTo("missing_growth_record");
    }

    private static HerbCollectionTaskVO task() {
        HerbCollectionTaskVO task = new HerbCollectionTaskVO();
        task.setId(9L);
        task.setTaskCode("TASK-009");
        task.setTaskName("黄连连续观测");
        task.setSpeciesId(8L);
        task.setSpeciesName("黄连");
        task.setBaseName("石柱黄连基地");
        task.setCollectPlace("重庆石柱");
        task.setCollectorName("采集员1");
        task.setDescription("连续观测任务");
        return task;
    }

    private static HerbBatchVO batch(Long id, LocalDateTime collectStartTime) {
        HerbBatchVO batch = new HerbBatchVO();
        batch.setId(id);
        batch.setBatchCode("BATCH-" + id);
        batch.setBatchName("观测批次" + id);
        batch.setBaseName("石柱黄连基地");
        batch.setOriginPlace("重庆石柱");
        batch.setCollectStartTime(collectStartTime);
        batch.setCreateTime(collectStartTime);
        return batch;
    }

    private static GrowthRecordVO growthRecord(Long id, LocalDateTime collectedAt) {
        GrowthRecordVO record = new GrowthRecordVO();
        record.setId(id);
        record.setGrowthStage("幼苗期");
        record.setCollectedAt(collectedAt);
        record.setCollectorName("采集员1");
        record.setReviewStatus("approved");
        return record;
    }

    private static HerbImageVO image(Long id, String imageType) {
        HerbImageVO image = new HerbImageVO();
        image.setId(id);
        image.setImageUrl("/api/files/" + id + "/content");
        image.setImageType(imageType);
        image.setUploaderName("采集员1");
        image.setUploadTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        return image;
    }

    private static HerbBatchImageVO binding(Long imageId, boolean primary) {
        HerbBatchImageVO binding = new HerbBatchImageVO();
        binding.setImageId(imageId);
        binding.setIsPrimary(primary ? 1 : 0);
        return binding;
    }
}
