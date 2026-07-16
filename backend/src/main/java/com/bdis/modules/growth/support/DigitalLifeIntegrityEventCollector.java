package com.bdis.modules.growth.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.growth.entity.GrowthTraceEventEntity;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.mapper.GrowthTraceEventMapper;
import com.bdis.modules.growth.support.DigitalLifeHashChain.CanonicalEvent;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.vo.HerbImageVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DigitalLifeIntegrityEventCollector {

    private static final Set<String> TRACE_EVENT_TYPES =
            Set.of("submitted", "approved", "rejected", "resubmitted", "archived");

    private final HerbCollectionTaskMapper taskMapper;
    private final HerbBatchMapper batchMapper;
    private final GrowthRecordMapper growthRecordMapper;
    private final HerbImageMapper imageMapper;
    private final HerbBatchImageMapper batchImageMapper;
    private final GrowthTraceEventMapper traceEventMapper;

    public DigitalLifeIntegrityEventCollector(
            HerbCollectionTaskMapper taskMapper,
            HerbBatchMapper batchMapper,
            GrowthRecordMapper growthRecordMapper,
            HerbImageMapper imageMapper,
            HerbBatchImageMapper batchImageMapper,
            GrowthTraceEventMapper traceEventMapper) {
        this.taskMapper = taskMapper;
        this.batchMapper = batchMapper;
        this.growthRecordMapper = growthRecordMapper;
        this.imageMapper = imageMapper;
        this.batchImageMapper = batchImageMapper;
        this.traceEventMapper = traceEventMapper;
    }

    public List<CanonicalEvent> collect(HerbDigitalLifeArchiveVO archive) {
        List<CanonicalEvent> events = new ArrayList<>();
        HerbCollectionTaskEntity task = taskMapper.selectById(archive.getTaskId());
        if (task != null && task.getCreatedAt() != null) {
            events.add(taskCreated(task));
        }

        List<HerbBatchVO> batches = safeList(batchMapper.selectByTaskId(archive.getTaskId()));
        batches.stream()
                .filter(batch -> batch.getId() != null && batch.getCreateTime() != null)
                .map(this::batchCreated)
                .forEach(events::add);
        List<Long> batchIds =
                batches.stream().map(HerbBatchVO::getId).filter(Objects::nonNull).toList();
        if (batchIds.isEmpty()) {
            return events;
        }

        List<GrowthRecordVO> records =
                safeList(growthRecordMapper.selectJoinedByBatchIds(batchIds));
        records.stream()
                .filter(record -> record.getId() != null && record.getCreatedAt() != null)
                .map(this::growthRecordCreated)
                .forEach(events::add);
        records.stream()
                .filter(
                        record ->
                                record.getId() != null
                                        && record.getTraceGeneratedTime() != null
                                        && record.getTraceCode() != null)
                .map(this::traceGenerated)
                .forEach(events::add);

        safeList(imageMapper.selectByBatchIds(batchIds)).stream()
                .filter(image -> image.getId() != null && image.getUploadTime() != null)
                .map(this::imageUploaded)
                .forEach(events::add);
        safeList(batchImageMapper.selectBatchImagesWithIdentificationByBatchIds(batchIds)).stream()
                .filter(
                        recognition ->
                                recognition.getIdentificationResultId() != null
                                        && recognition.getIdentifyTime() != null)
                .map(this::recognitionCompleted)
                .forEach(events::add);

        List<Long> recordIds =
                records.stream().map(GrowthRecordVO::getId).filter(Objects::nonNull).toList();
        if (!recordIds.isEmpty()) {
            safeList(
                            traceEventMapper.selectList(
                                    new LambdaQueryWrapper<GrowthTraceEventEntity>()
                                            .in(GrowthTraceEventEntity::getRecordId, recordIds)))
                    .stream()
                    .filter(
                            event ->
                                    event.getRecordId() != null
                                            && event.getEventTime() != null
                                            && TRACE_EVENT_TYPES.contains(event.getEventType()))
                    .map(this::traceEvent)
                    .forEach(events::add);
        }
        return events;
    }

    private CanonicalEvent taskCreated(HerbCollectionTaskEntity task) {
        Map<String, Object> payload = payload();
        payload.put("taskCode", task.getTaskCode());
        payload.put("taskName", task.getTaskName());
        payload.put("speciesId", task.getSpeciesId());
        payload.put("speciesName", task.getSpeciesName());
        payload.put("baseId", task.getBaseId());
        payload.put("baseName", task.getBaseName());
        payload.put("collectorId", task.getCollectorId());
        payload.put("taskStatus", task.getTaskStatus());
        payload.put("traceCode", task.getTraceCode());
        payload.put("publicVisible", task.getPublicVisible());
        return new CanonicalEvent(
                "collection_task",
                task.getId(),
                "task_created",
                task.getCreatedAt(),
                task.getCreatedBy(),
                null,
                payload);
    }

    private CanonicalEvent batchCreated(HerbBatchVO batch) {
        Map<String, Object> payload = payload();
        payload.put("batchCode", batch.getBatchCode());
        payload.put("batchName", batch.getBatchName());
        payload.put("speciesId", batch.getSpeciesId());
        payload.put("speciesName", batch.getSpeciesName());
        payload.put("baseId", batch.getBaseId());
        payload.put("baseName", batch.getBaseName());
        payload.put("originPlace", batch.getOriginPlace());
        payload.put("collectStartTime", batch.getCollectStartTime());
        payload.put("collectEndTime", batch.getCollectEndTime());
        payload.put("batchStatus", batch.getBatchStatus());
        return new CanonicalEvent(
                "collection_batch",
                batch.getId(),
                "batch_created",
                batch.getCreateTime(),
                null,
                null,
                payload);
    }

    private CanonicalEvent growthRecordCreated(GrowthRecordVO record) {
        Map<String, Object> payload = payload();
        payload.put("batchId", record.getBatchId());
        payload.put("growthStage", record.getGrowthStage());
        payload.put("collectedAt", record.getCollectedAt());
        payload.put("longitude", record.getLongitude());
        payload.put("latitude", record.getLatitude());
        payload.put("plantHeight", record.getPlantHeight());
        payload.put("stemDiameter", record.getStemDiameter());
        payload.put("leafColor", record.getLeafColor());
        payload.put("floweringStatus", record.getFloweringStatus());
        payload.put("temperature", record.getTemperature());
        payload.put("humidity", record.getHumidity());
        payload.put("soilMoisture", record.getSoilMoisture());
        payload.put("soilPh", record.getSoilPh());
        payload.put("light", record.getLight());
        payload.put("reviewStatus", record.getReviewStatus());
        return new CanonicalEvent(
                "growth_record",
                record.getId(),
                "growth_record_created",
                record.getCreatedAt(),
                record.getCollectorId(),
                record.getCollectorName(),
                payload);
    }

    private CanonicalEvent imageUploaded(HerbImageVO image) {
        Map<String, Object> payload = payload();
        payload.put("batchId", image.getBatchId());
        payload.put("imageCode", image.getImageCode());
        payload.put("imageType", image.getImageType());
        payload.put("imageRole", image.getImageRole());
        payload.put("collectTime", image.getCollectTime());
        payload.put("collectPlace", image.getCollectPlace());
        payload.put("growthStage", image.getGrowthStage());
        payload.put("healthStatus", image.getHealthStatus());
        payload.put("processStatus", image.getProcessStatus());
        return new CanonicalEvent(
                "herb_image",
                image.getId(),
                "image_uploaded",
                image.getUploadTime(),
                image.getCollectorId(),
                image.getUploaderName(),
                payload);
    }

    private CanonicalEvent recognitionCompleted(HerbBatchImageVO recognition) {
        Map<String, Object> payload = payload();
        payload.put("imageId", recognition.getImageId());
        payload.put("finalSpeciesId", recognition.getFinalSpeciesId());
        payload.put("finalSpeciesName", recognition.getFinalSpeciesName());
        payload.put("finalConfidence", recognition.getFinalConfidence());
        payload.put("resultSource", recognition.getResultSource());
        payload.put("matchResult", recognition.getMatchResult());
        payload.put("needReview", recognition.getNeedReview());
        payload.put("reviewStatus", recognition.getReviewStatus());
        return new CanonicalEvent(
                "identification_result",
                recognition.getIdentificationResultId(),
                "recognition_completed",
                recognition.getIdentifyTime(),
                null,
                null,
                payload);
    }

    private CanonicalEvent traceEvent(GrowthTraceEventEntity event) {
        Map<String, Object> payload = payload();
        payload.put("beforeStatus", event.getBeforeStatus());
        payload.put("afterStatus", event.getAfterStatus());
        payload.put("eventTitle", event.getEventTitle());
        payload.put("operatorRole", event.getOperatorRole());
        return new CanonicalEvent(
                "growth_record",
                event.getRecordId(),
                event.getEventType(),
                event.getEventTime(),
                event.getOperatorId(),
                event.getOperatorName(),
                payload);
    }

    private CanonicalEvent traceGenerated(GrowthRecordVO record) {
        Map<String, Object> payload = payload();
        payload.put("traceCode", record.getTraceCode());
        payload.put("publicVisible", record.getPublicVisible());
        return new CanonicalEvent(
                "growth_record",
                record.getId(),
                "trace_generated",
                record.getTraceGeneratedTime(),
                record.getTraceGeneratedBy(),
                record.getTraceGeneratedByName(),
                payload);
    }

    private Map<String, Object> payload() {
        return new LinkedHashMap<>();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
