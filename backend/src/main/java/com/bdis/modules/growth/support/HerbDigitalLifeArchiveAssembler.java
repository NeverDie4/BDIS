package com.bdis.modules.growth.support;

import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeImageVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeRecognitionVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.bdis.modules.herb.vo.HerbImageVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HerbDigitalLifeArchiveAssembler {

    public HerbDigitalLifeArchiveVO assemble(
            HerbCollectionTaskVO task,
            List<HerbBatchVO> batches,
            Map<Long, GrowthRecordVO> growthRecordsByBatchId,
            Map<Long, List<HerbImageVO>> imagesByBatchId,
            Map<Long, List<HerbBatchImageVO>> batchImagesByBatchId) {
        Map<Long, GrowthRecordVO> growthRecords = safeMap(growthRecordsByBatchId);
        Map<Long, List<HerbImageVO>> images = safeMap(imagesByBatchId);
        Map<Long, List<HerbBatchImageVO>> batchImages = safeMap(batchImagesByBatchId);
        HerbDigitalLifeArchiveVO archive = mapTask(task);
        List<HerbBatchVO> sortedBatches =
                safeList(batches).stream()
                        .filter(Objects::nonNull)
                        .sorted(
                                Comparator.comparing(
                                                (HerbBatchVO batch) ->
                                                        resolveCollectedAt(
                                                                batch,
                                                                valueByKey(
                                                                        growthRecords,
                                                                        batch.getId())),
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(
                                                HerbBatchVO::getId,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();

        List<HerbDigitalLifeStageVO> stages = new ArrayList<>(sortedBatches.size());
        for (int index = 0; index < sortedBatches.size(); index++) {
            HerbBatchVO batch = sortedBatches.get(index);
            GrowthRecordVO growthRecord = valueByKey(growthRecords, batch.getId());
            List<HerbImageVO> stageImages = safeList(valueByKey(images, batch.getId()));
            List<HerbBatchImageVO> stageBatchImages =
                    safeList(valueByKey(batchImages, batch.getId()));
            stages.add(
                    mapStage(task, batch, growthRecord, stageImages, stageBatchImages, index + 1));
        }

        archive.setStages(stages);
        archive.setStageCount(stages.size());
        archive.setValidStageCount(
                (int)
                        stages.stream()
                                .filter(stage -> "complete".equals(stage.getDataStatus()))
                                .count());
        archive.setImageCount(stages.stream().mapToInt(stage -> stage.getImages().size()).sum());
        archive.setStartTime(
                stages.stream()
                        .map(HerbDigitalLifeStageVO::getCollectedAt)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(null));
        archive.setEndTime(
                stages.stream()
                        .map(HerbDigitalLifeStageVO::getCollectedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null));
        archive.setArchiveStatus(resolveArchiveStatus(archive));
        return archive;
    }

    private HerbDigitalLifeArchiveVO mapTask(HerbCollectionTaskVO task) {
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        if (task == null) {
            return archive;
        }
        archive.setTaskId(task.getId());
        archive.setTaskCode(task.getTaskCode());
        archive.setTaskName(task.getTaskName());
        archive.setSpeciesId(task.getSpeciesId());
        archive.setSpeciesName(task.getSpeciesName());
        archive.setBaseName(task.getBaseName());
        archive.setDescription(task.getDescription());
        archive.setTraceCode(task.getTraceCode());
        archive.setPublicVisible(Integer.valueOf(1).equals(task.getPublicVisible()));
        return archive;
    }

    private HerbDigitalLifeStageVO mapStage(
            HerbCollectionTaskVO task,
            HerbBatchVO batch,
            GrowthRecordVO growthRecord,
            List<HerbImageVO> images,
            List<HerbBatchImageVO> batchImages,
            int sequence) {
        HerbDigitalLifeStageVO stage = new HerbDigitalLifeStageVO();
        stage.setStageId(batch.getId());
        stage.setSequence(sequence);
        stage.setBatchId(batch.getId());
        stage.setBatchCode(batch.getBatchCode());
        stage.setBatchName(batch.getBatchName());
        stage.setCollectedAt(resolveCollectedAt(batch, growthRecord));
        stage.setLocationName(
                firstText(batch.getOriginPlace(), task == null ? null : task.getCollectPlace()));
        stage.setImages(mapImages(images, batchImages));
        stage.setDataStatus(resolveDataStatus(growthRecord, stage.getImages()));
        stage.setRecognition(mapRecognition(batch, batchImages));
        if (growthRecord == null) {
            stage.setBaseName(
                    firstText(batch.getBaseName(), task == null ? null : task.getBaseName()));
            stage.setCollectorName(task == null ? null : task.getCollectorName());
            return stage;
        }
        stage.setGrowthRecordId(growthRecord.getId());
        stage.setGrowthStage(growthRecord.getGrowthStage());
        stage.setCollectorName(
                firstText(
                        growthRecord.getCollectorName(),
                        task == null ? null : task.getCollectorName()));
        stage.setBaseName(
                firstText(
                        growthRecord.getBaseName(),
                        batch.getBaseName(),
                        task == null ? null : task.getBaseName()));
        stage.setLongitude(growthRecord.getLongitude());
        stage.setLatitude(growthRecord.getLatitude());
        stage.setAuditStatus(growthRecord.getReviewStatus());
        stage.setReviewedAt(growthRecord.getReviewedAt());
        stage.setMetrics(mapMetrics(growthRecord));
        return stage;
    }

    private List<HerbDigitalLifeImageVO> mapImages(
            List<HerbImageVO> images, List<HerbBatchImageVO> batchImages) {
        return images.stream()
                .filter(Objects::nonNull)
                .map(image -> mapImage(image, findBatchImage(image.getId(), batchImages)))
                .toList();
    }

    private HerbDigitalLifeImageVO mapImage(HerbImageVO image, HerbBatchImageVO batchImage) {
        HerbDigitalLifeImageVO result = new HerbDigitalLifeImageVO();
        result.setImageId(image.getId());
        result.setImageUrl(image.getImageUrl());
        result.setThumbnailUrl(image.getThumbnailUrl());
        result.setImageType(image.getImageType());
        result.setImageTypeName(HerbDigitalLifeImageType.displayNameOf(image.getImageType()));
        result.setUploadTime(image.getUploadTime());
        result.setUploaderName(image.getUploaderName());
        result.setPrimaryImage(
                batchImage != null && Integer.valueOf(1).equals(batchImage.getIsPrimary()));
        return result;
    }

    private HerbBatchImageVO findBatchImage(Long imageId, List<HerbBatchImageVO> batchImages) {
        return batchImages.stream()
                .filter(Objects::nonNull)
                .filter(batchImage -> Objects.equals(imageId, batchImage.getImageId()))
                .findFirst()
                .orElse(null);
    }

    private HerbDigitalLifeRecognitionVO mapRecognition(
            HerbBatchVO batch, List<HerbBatchImageVO> batchImages) {
        HerbBatchImageVO recognition =
                batchImages.stream()
                        .filter(Objects::nonNull)
                        .filter(this::hasRecognition)
                        .sorted(
                                Comparator.comparing(
                                        item -> !Integer.valueOf(1).equals(item.getIsPrimary())))
                        .findFirst()
                        .orElse(null);
        if (recognition == null) {
            return null;
        }
        HerbDigitalLifeRecognitionVO result = new HerbDigitalLifeRecognitionVO();
        result.setSpeciesName(recognition.getFinalSpeciesName());
        result.setConfidence(recognition.getFinalConfidence());
        result.setSimilarity(batch.getAvgSimilarity());
        result.setNeedReview(recognition.getNeedReview());
        result.setRecognitionSource(recognition.getResultSource());
        result.setConclusion(firstText(recognition.getSuggestion(), recognition.getMatchResult()));
        return result;
    }

    private boolean hasRecognition(HerbBatchImageVO image) {
        return image.getIdentificationResultId() != null
                || StringUtils.hasText(image.getFinalSpeciesName())
                || image.getFinalConfidence() != null;
    }

    private HerbDigitalLifeMetricsVO mapMetrics(GrowthRecordVO growthRecord) {
        HerbDigitalLifeMetricsVO metrics = new HerbDigitalLifeMetricsVO();
        metrics.setPlantHeight(growthRecord.getPlantHeight());
        metrics.setStemDiameter(growthRecord.getStemDiameter());
        metrics.setLeafColor(growthRecord.getLeafColor());
        metrics.setFloweringStatus(growthRecord.getFloweringStatus());
        metrics.setTemperature(growthRecord.getTemperature());
        metrics.setHumidity(growthRecord.getHumidity());
        metrics.setSoilMoisture(growthRecord.getSoilMoisture());
        metrics.setSoilPh(growthRecord.getSoilPh());
        metrics.setLight(growthRecord.getLight());
        metrics.setGrowthEvaluation(growthRecord.getGrowthEvaluation());
        metrics.setRemark(growthRecord.getRemark());
        return metrics;
    }

    private LocalDateTime resolveCollectedAt(HerbBatchVO batch, GrowthRecordVO growthRecord) {
        if (growthRecord != null && growthRecord.getCollectedAt() != null) {
            return growthRecord.getCollectedAt();
        }
        if (batch.getCollectStartTime() != null) {
            return batch.getCollectStartTime();
        }
        return batch.getCreateTime();
    }

    private String resolveDataStatus(
            GrowthRecordVO growthRecord, List<HerbDigitalLifeImageVO> images) {
        if (growthRecord == null) {
            return "missing_growth_record";
        }
        if (images.isEmpty()) {
            return "missing_images";
        }
        if (growthRecord.getId() != null) {
            return "complete";
        }
        return "incomplete";
    }

    private String resolveArchiveStatus(HerbDigitalLifeArchiveVO archive) {
        if (archive.getStageCount() == 0) {
            return "empty";
        }
        if (Objects.equals(archive.getStageCount(), archive.getValidStageCount())) {
            return "complete";
        }
        return "incomplete";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> values) {
        return values == null ? Map.of() : values;
    }

    private static <K, V> V valueByKey(Map<K, V> values, K key) {
        return key == null ? null : values.get(key);
    }
}
