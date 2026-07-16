package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.tool.dto.HerbBatchToolData;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class HerbBatchReadTool implements AgentBusinessTool {

    public static final String OVERVIEW = "herb_batch.overview";
    public static final String COMPLETENESS = "herb_batch.completeness_snapshot";
    public static final String LIST_IMAGES = "herb_batch.list_images";

    private final HerbBatchService batchService;
    private final HerbBatchImageService batchImageService;
    private final HerbCollectionTaskService taskService;
    private final HerbDigitalLifeArchiveService archiveService;
    private final AgentReadToolSupport support;

    public HerbBatchReadTool(
            HerbBatchService batchService,
            HerbBatchImageService batchImageService,
            HerbCollectionTaskService taskService,
            HerbDigitalLifeArchiveService archiveService,
            AgentReadToolSupport support) {
        this.batchService = batchService;
        this.batchImageService = batchImageService;
        this.taskService = taskService;
        this.archiveService = archiveService;
        this.support = support;
    }

    public HerbBatchToolData.Overview getBatchOverview(
            Long batchId, AgentToolExecutionContext context) {
        Snapshot snapshot = snapshot(batchId, context);
        HerbBatchVO batch = snapshot.batch();
        HerbDigitalLifeStageVO stage = snapshot.stage();
        List<HerbBatchImageVO> images = snapshot.images();
        int identified =
                (int) images.stream().filter(i -> i.getIdentificationResultId() != null).count();
        int needReview =
                (int) images.stream().filter(i -> Boolean.TRUE.equals(i.getNeedReview())).count();
        return new HerbBatchToolData.Overview(
                batch.getId(),
                batch.getBatchCode(),
                batch.getBatchName(),
                batch.getTaskId(),
                batch.getTaskName(),
                batch.getSpeciesId(),
                batch.getSpeciesName(),
                batch.getBaseId(),
                batch.getBaseName(),
                snapshot.task().getCollectorName(),
                batch.getCollectStartTime(),
                batch.getCollectEndTime(),
                support.status(batch.getBatchStatus()),
                stage == null ? null : stage.getGrowthRecordId(),
                support.status(stage == null ? null : stage.getAuditStatus()),
                images.size(),
                identified,
                needReview,
                canSubmit(stage, images));
    }

    public HerbBatchToolData.CompletenessSnapshot getBatchCompletenessSnapshot(
            Long batchId, AgentToolExecutionContext context) {
        Snapshot snapshot = snapshot(batchId, context);
        List<HerbBatchImageVO> images = snapshot.images();
        int identified =
                (int) images.stream().filter(i -> i.getIdentificationResultId() != null).count();
        int needReview =
                (int) images.stream().filter(i -> Boolean.TRUE.equals(i.getNeedReview())).count();
        return new HerbBatchToolData.CompletenessSnapshot(
                batchId,
                snapshot.stage() != null && snapshot.stage().getGrowthRecordId() != null,
                images.size(),
                identified,
                images.size() - identified,
                needReview,
                emptyFields(snapshot.stage()),
                canSubmit(snapshot.stage(), images));
    }

    public HerbBatchToolData.ImageList listBatchImages(
            Long batchId, AgentToolExecutionContext context) {
        Snapshot snapshot = snapshot(batchId, context);
        return new HerbBatchToolData.ImageList(
                batchId, snapshot.images().stream().map(this::toImage).toList());
    }

    @Override
    public List<AgentToolDefinition> definitions() {
        return List.of(
                support.readDefinition(OVERVIEW, "采集批次概览", "读取批次、记录和图片摘要"),
                support.readDefinition(COMPLETENESS, "批次完整度快照", "读取批次已有字段快照，不执行诊断"),
                support.readDefinition(LIST_IMAGES, "批次现场图片", "读取批次图片及已有识别摘要"));
    }

    private Snapshot snapshot(Long batchId, AgentToolExecutionContext context) {
        HerbBatchVO batch = batchService.getById(batchId);
        support.requireTaskContext(context, batch.getTaskId());
        HerbCollectionTaskVO task = taskService.getById(batch.getTaskId());
        HerbDigitalLifeStageVO stage =
                archiveService.getByTaskId(batch.getTaskId()).getStages().stream()
                        .filter(item -> batchId.equals(item.getBatchId()))
                        .findFirst()
                        .orElse(null);
        List<HerbBatchImageVO> images =
                batchImageService.listByBatch(batchId, new HerbBatchImageQueryRequest());
        return new Snapshot(batch, task, stage, images);
    }

    private HerbBatchToolData.ImageSummary toImage(HerbBatchImageVO image) {
        return new HerbBatchToolData.ImageSummary(
                image.getImageId(),
                image.getImageCode(),
                support.browserUrl(image.getImageUrl()),
                image.getImageType(),
                image.getImageRole(),
                image.getCollectTime(),
                Integer.valueOf(1).equals(image.getIsPrimary()),
                image.getIdentificationResultId(),
                image.getFinalSpeciesName(),
                image.getFinalConfidence(),
                image.getNeedReview(),
                support.status(image.getReviewStatus()),
                image.getResultSource());
    }

    private List<String> emptyFields(HerbDigitalLifeStageVO stage) {
        List<String> fields = new ArrayList<>();
        if (stage == null || stage.getGrowthRecordId() == null) {
            fields.add("growthRecord");
            return fields;
        }
        HerbDigitalLifeMetricsVO metrics = stage.getMetrics();
        if (metrics == null) {
            fields.add("metrics");
            return fields;
        }
        addEmpty(fields, "growthStage", stage.getGrowthStage());
        addEmpty(fields, "plantHeight", metrics.getPlantHeight());
        addEmpty(fields, "temperature", metrics.getTemperature());
        addEmpty(fields, "humidity", metrics.getHumidity());
        addEmpty(fields, "soilMoisture", metrics.getSoilMoisture());
        addEmpty(fields, "soilPh", metrics.getSoilPh());
        return fields;
    }

    private void addEmpty(List<String> fields, String name, Object value) {
        if (value == null || value instanceof String text && !StringUtils.hasText(text)) {
            fields.add(name);
        }
    }

    private boolean canSubmit(HerbDigitalLifeStageVO stage, List<HerbBatchImageVO> images) {
        return stage != null
                && stage.getGrowthRecordId() != null
                && !images.isEmpty()
                && "draft".equals(stage.getAuditStatus());
    }

    private record Snapshot(
            HerbBatchVO batch,
            HerbCollectionTaskVO task,
            HerbDigitalLifeStageVO stage,
            List<HerbBatchImageVO> images) {}
}
