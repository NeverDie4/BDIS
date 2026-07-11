package com.bdis.modules.assistant.tool;

import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.mapper.HerbAssistantImageContextMapper;
import com.bdis.modules.assistant.tool.dto.HerbAssistantBatchToolResult;
import com.bdis.modules.assistant.tool.dto.HerbAssistantImageToolResult;
import com.bdis.modules.assistant.tool.dto.HerbAssistantTaskToolResult;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainContextVO;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HerbAssistantTools {

    private static final int MAX_MY_TASKS = 20;

    private final HerbBatchMapper batchMapper;
    private final HerbAssistantImageContextMapper imageContextMapper;
    private final HerbCollectionTaskMapper taskMapper;

    public HerbAssistantTools(
            HerbBatchMapper batchMapper,
            HerbAssistantImageContextMapper imageContextMapper,
            HerbCollectionTaskMapper taskMapper) {
        this.batchMapper = batchMapper;
        this.imageContextMapper = imageContextMapper;
        this.taskMapper = taskMapper;
    }

    @Tool(description = "根据批次 ID 查询批次状态、识别结果、复核数量和质量摘要")
    public HerbAssistantBatchToolResult getBatchSummaryById(
            @ToolParam(description = "批次 ID，必须为正整数") Long batchId) {
        if (batchId == null || batchId <= 0) {
            return batchError("请提供有效的批次 ID");
        }
        try {
            HerbBatchEntity batch = batchMapper.selectById(batchId);
            return batch == null || !canAccessBatch(batch)
                    ? batchError("未找到批次 ID " + batchId)
                    : toBatchResult(batch);
        } catch (RuntimeException exception) {
            return batchError("批次信息查询失败，请稍后重试");
        }
    }

    @Tool(description = "根据批次编码查询批次状态、识别结果、复核数量和质量摘要")
    public HerbAssistantBatchToolResult getBatchSummaryByCode(
            @ToolParam(description = "完整批次编码，例如 BATCH_20260710_001") String batchCode) {
        if (!StringUtils.hasText(batchCode)) {
            return batchError("请提供有效的批次编码");
        }
        String normalizedCode = batchCode.trim();
        try {
            HerbBatchEntity batch = batchMapper.selectByBatchCode(normalizedCode);
            return batch == null || !canAccessBatch(batch)
                    ? batchError("未找到批次 " + normalizedCode)
                    : toBatchResult(batch);
        } catch (RuntimeException exception) {
            return batchError("批次信息查询失败，请稍后重试");
        }
    }

    @Tool(description = "根据图片 ID 查询图片的最终识别结果、置信度和复核状态")
    public HerbAssistantImageToolResult getImageIdentificationById(
            @ToolParam(description = "采集图片 ID，必须为正整数") Long imageId) {
        if (imageId == null || imageId <= 0) {
            return imageError("请提供有效的图片 ID");
        }
        try {
            HerbAssistantImageExplainContextVO context =
                    imageContextMapper.selectImageContextById(
                            imageId,
                            SecurityUtils.currentUser().getUserId(),
                            hasAllAssistantDataScope());
            return context == null ? imageError("未找到图片 ID " + imageId) : toImageResult(context);
        } catch (RuntimeException exception) {
            return imageError("图片识别结果查询失败，请稍后重试");
        }
    }

    @Tool(description = "查询当前所有处于复核中状态的批次摘要")
    public List<HerbAssistantBatchToolResult> listReviewingBatches() {
        try {
            HerbBatchQueryRequest query = new HerbBatchQueryRequest();
            query.setBatchStatus(HerbBatchStatusConstants.REVIEWING);
            return batchMapper.selectList(query).stream().map(this::toBatchListResult).toList();
        } catch (RuntimeException exception) {
            return List.of(batchError("复核中批次查询失败，请稍后重试"));
        }
    }

    @Tool(description = "根据采集任务 ID 查询任务状态、计划时间、采集人和批次数量")
    public HerbAssistantTaskToolResult getTaskSummaryById(
            @ToolParam(description = "采集任务 ID，必须为正整数") Long taskId) {
        if (taskId == null || taskId <= 0) {
            return taskError("请提供有效的采集任务 ID");
        }
        try {
            HerbCollectionTaskVO task = taskMapper.selectDetailById(taskId);
            return task == null || !canAccessTask(task)
                    ? taskError("未找到采集任务 ID " + taskId)
                    : toTaskResult(task);
        } catch (RuntimeException exception) {
            return taskError("采集任务查询失败，请稍后重试");
        }
    }

    @Tool(description = "查询当前登录用户负责的采集任务，最多返回 20 条摘要")
    public List<HerbAssistantTaskToolResult> listMyTasks() {
        try {
            HerbCollectionTaskMyQueryRequest query = new HerbCollectionTaskMyQueryRequest();
            query.setCollectorId(SecurityUtils.currentUser().getUserId());
            query.setPageNum(1);
            query.setPageSize(MAX_MY_TASKS);
            return taskMapper.selectMyTasks(query, 0L, MAX_MY_TASKS).stream()
                    .map(this::toTaskResult)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of(taskError("当前用户的采集任务查询失败，请确认已登录后重试"));
        }
    }

    private HerbAssistantBatchToolResult toBatchResult(HerbBatchEntity batch) {
        HerbAssistantBatchToolResult result = new HerbAssistantBatchToolResult();
        result.setSuccess(true);
        result.setMessage("查询成功");
        result.setBatchId(batch.getId());
        result.setBatchCode(batch.getBatchCode());
        result.setBatchName(batch.getBatchName());
        result.setBatchStatus(batch.getBatchStatus());
        result.setSpeciesName(batch.getSpeciesName());
        result.setImageCount(batch.getImageCount());
        result.setIdentifiedCount(batch.getIdentifiedCount());
        result.setReviewedCount(batch.getReviewedCount());
        result.setNeedReviewCount(batch.getNeedReviewCount());
        result.setFinalSpeciesName(batch.getFinalSpeciesName());
        result.setAvgSimilarity(batch.getAvgSimilarity());
        result.setQualityLevel(batch.getQualityLevel());
        result.setEvaluationSummary(batch.getEvaluationSummary());
        return result;
    }

    private HerbAssistantBatchToolResult toBatchListResult(HerbBatchListVO batch) {
        HerbAssistantBatchToolResult result = new HerbAssistantBatchToolResult();
        result.setSuccess(true);
        result.setMessage("查询成功");
        result.setBatchId(batch.getId());
        result.setBatchCode(batch.getBatchCode());
        result.setBatchName(batch.getBatchName());
        result.setBatchStatus(batch.getBatchStatus());
        result.setSpeciesName(batch.getSpeciesName());
        return result;
    }

    private HerbAssistantImageToolResult toImageResult(HerbAssistantImageExplainContextVO context) {
        HerbAssistantImageToolResult result = new HerbAssistantImageToolResult();
        result.setSuccess(true);
        result.setMessage("查询成功");
        result.setImageId(context.getImageId());
        result.setImageCode(context.getImageCode());
        result.setImageType(context.getImageType());
        result.setFinalSpeciesName(context.getFinalSpeciesName());
        result.setFinalConfidence(context.getFinalConfidence());
        result.setResultSource(context.getResultSource());
        result.setMatchResult(context.getMatchResult());
        result.setNeedReview(context.getNeedReview());
        result.setReviewStatus(context.getReviewStatus());
        result.setSuggestion(context.getSuggestion());
        return result;
    }

    private HerbAssistantTaskToolResult toTaskResult(HerbCollectionTaskVO task) {
        HerbAssistantTaskToolResult result = new HerbAssistantTaskToolResult();
        result.setSuccess(true);
        result.setMessage("查询成功");
        result.setTaskId(task.getId());
        result.setTaskCode(task.getTaskCode());
        result.setTaskName(task.getTaskName());
        result.setTaskStatus(task.getTaskStatus());
        result.setSpeciesName(task.getSpeciesName());
        result.setBaseName(task.getBaseName());
        result.setCollectPlace(task.getCollectPlace());
        result.setPlannedStartTime(task.getPlannedStartTime());
        result.setPlannedEndTime(task.getPlannedEndTime());
        result.setCollectorName(task.getCollectorName());
        result.setBatchCount(task.getBatchCount());
        return result;
    }

    private HerbAssistantBatchToolResult batchError(String message) {
        HerbAssistantBatchToolResult result = new HerbAssistantBatchToolResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    private HerbAssistantImageToolResult imageError(String message) {
        HerbAssistantImageToolResult result = new HerbAssistantImageToolResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    private HerbAssistantTaskToolResult taskError(String message) {
        HerbAssistantTaskToolResult result = new HerbAssistantTaskToolResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    private boolean canAccessBatch(HerbBatchEntity batch) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (hasAllAssistantDataScope(currentUser)) {
            return true;
        }
        if (currentUser.getUserId().equals(batch.getCreatedBy())) {
            return true;
        }
        if (batch.getTaskId() == null) {
            return false;
        }
        HerbCollectionTaskEntity task = taskMapper.selectById(batch.getTaskId());
        return task != null
                && (currentUser.getUserId().equals(task.getCollectorId())
                        || currentUser.getUserId().equals(task.getCreatedBy()));
    }

    private boolean canAccessTask(HerbCollectionTaskVO task) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return hasAllAssistantDataScope(currentUser)
                || currentUser.getUserId().equals(task.getCollectorId());
    }

    private boolean hasAllAssistantDataScope() {
        return hasAllAssistantDataScope(SecurityUtils.currentUser());
    }

    private boolean hasAllAssistantDataScope(CurrentUser currentUser) {
        return currentUser.getRoleCodes().contains("ADMIN")
                || currentUser.getPermissions().contains("*")
                || currentUser.getPermissions().contains("herb:assistant:data:all");
    }
}
