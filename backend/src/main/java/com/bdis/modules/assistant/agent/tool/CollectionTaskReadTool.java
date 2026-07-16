package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class CollectionTaskReadTool implements AgentBusinessTool {

    public static final String OVERVIEW = "collection_task.overview";
    public static final String PROGRESS = "collection_task.progress";
    public static final String LIST_BATCHES = "collection_task.list_batches";

    private final HerbCollectionTaskService taskService;
    private final HerbDigitalLifeArchiveService archiveService;
    private final AgentReadToolSupport support;

    public CollectionTaskReadTool(
            HerbCollectionTaskService taskService,
            HerbDigitalLifeArchiveService archiveService,
            AgentReadToolSupport support) {
        this.taskService = taskService;
        this.archiveService = archiveService;
        this.support = support;
    }

    public CollectionTaskToolData.Overview getCollectionTaskOverview(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        HerbCollectionTaskVO task = taskService.getById(taskId);
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        CollectionTaskToolData.Progress progress = progress(archive);
        return new CollectionTaskToolData.Overview(
                task.getId(),
                task.getTaskCode(),
                task.getTaskName(),
                task.getSpeciesId(),
                task.getSpeciesName(),
                task.getBaseId(),
                task.getBaseName(),
                task.getCollectPlace(),
                task.getCollectorName(),
                support.status(task.getTaskStatus()),
                task.getPlannedStartTime(),
                task.getPlannedEndTime(),
                nullableInt(task.getBatchCount()),
                progress.growthRecordCount(),
                progress.imageCount(),
                progress.identifiedImageCount(),
                progress.pendingReviewCount(),
                progress.approvedCount(),
                progress.rejectedCount(),
                support.status(archive.getArchiveStatus()));
    }

    public CollectionTaskToolData.Progress getCollectionTaskProgress(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        taskService.getById(taskId);
        return progress(archiveService.getByTaskId(taskId));
    }

    public CollectionTaskToolData.BatchList listCollectionTaskBatches(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        taskService.getById(taskId);
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        List<CollectionTaskToolData.BatchSummary> batches =
                archive.getStages().stream()
                        .map(
                                stage ->
                                        new CollectionTaskToolData.BatchSummary(
                                                stage.getBatchId(),
                                                stage.getBatchCode(),
                                                stage.getBatchName(),
                                                support.status(stage.getDataStatus()),
                                                stage.getCollectedAt(),
                                                null,
                                                stage.getGrowthRecordId(),
                                                support.status(stage.getAuditStatus()),
                                                stage.getImages() == null
                                                        ? null
                                                        : stage.getImages().size()))
                        .toList();
        return new CollectionTaskToolData.BatchList(taskId, batches);
    }

    @Override
    public List<AgentToolDefinition> definitions() {
        return List.of(
                support.readDefinition(OVERVIEW, "采集任务概览", "读取采集任务及数字档案摘要"),
                support.readDefinition(PROGRESS, "采集任务进度", "读取采集、识别和审核进度"),
                support.readDefinition(LIST_BATCHES, "采集任务批次", "批量读取任务下的采集批次"));
    }

    private CollectionTaskToolData.Progress progress(HerbDigitalLifeArchiveVO archive) {
        List<HerbDigitalLifeStageVO> stages = archive.getStages();
        int growthRecords =
                (int) stages.stream().filter(s -> s.getGrowthRecordId() != null).count();
        int images =
                stages.stream()
                        .mapToInt(s -> s.getImages() == null ? 0 : s.getImages().size())
                        .sum();
        int identified =
                stages.stream()
                        .filter(s -> s.getRecognition() != null)
                        .mapToInt(
                                s ->
                                        s.getRecognition().getSpeciesName() == null
                                                        && s.getRecognition().getConfidence()
                                                                == null
                                                ? 0
                                                : 1)
                        .sum();
        int pendingReview =
                (int)
                        stages.stream()
                                .filter(s -> s.getRecognition() != null)
                                .filter(
                                        s ->
                                                Boolean.TRUE.equals(
                                                        s.getRecognition().getNeedReview()))
                                .count();
        int approved = countStatus(stages, "approved");
        int rejected = countStatus(stages, "rejected");
        return new CollectionTaskToolData.Progress(
                archive.getTaskId(),
                archive.getStageCount(),
                growthRecords,
                images,
                identified,
                pendingReview,
                approved,
                rejected,
                archive.getValidStageCount(),
                archive.getStageCount());
    }

    private int countStatus(List<HerbDigitalLifeStageVO> stages, String status) {
        return (int)
                stages.stream()
                        .map(HerbDigitalLifeStageVO::getAuditStatus)
                        .filter(Objects::nonNull)
                        .filter(status::equals)
                        .count();
    }

    private Integer nullableInt(Long value) {
        return value == null ? null : Math.toIntExact(value);
    }
}
