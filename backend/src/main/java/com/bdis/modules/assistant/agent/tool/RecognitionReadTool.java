package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.tool.dto.RecognitionToolData;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbImageBatchVO;
import com.bdis.modules.spectrum.service.HerbRecognitionService;
import com.bdis.modules.spectrum.vo.HerbRecognitionCandidateVO;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class RecognitionReadTool implements AgentBusinessTool {

    public static final String IMAGE_OVERVIEW = "recognition.image_overview";
    public static final String BATCH_OVERVIEW = "recognition.batch_overview";
    public static final String LOW_CONFIDENCE = "recognition.low_confidence_images";
    public static final String UNRECOGNIZED = "recognition.unrecognized_images";
    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = new BigDecimal("0.70");

    private final HerbRecognitionService recognitionService;
    private final HerbBatchImageService batchImageService;
    private final HerbBatchService batchService;
    private final AgentReadToolSupport support;

    public RecognitionReadTool(
            HerbRecognitionService recognitionService,
            HerbBatchImageService batchImageService,
            HerbBatchService batchService,
            AgentReadToolSupport support) {
        this.recognitionService = recognitionService;
        this.batchImageService = batchImageService;
        this.batchService = batchService;
        this.support = support;
    }

    public RecognitionToolData.ImageOverview getImageRecognitionOverview(
            Long imageId, AgentToolExecutionContext context) {
        HerbImageBatchVO binding = batchImageService.getBatchByImageId(imageId);
        if (binding == null || binding.getBatchId() == null) {
            throw new com.bdis.common.exception.BusinessException("图片未关联采集批次");
        }
        Long taskId = batchService.getById(binding.getBatchId()).getTaskId();
        support.requireTaskContext(context, taskId);
        return toOverview(imageId, recognitionService.latest(imageId));
    }

    public RecognitionToolData.BatchOverview getBatchRecognitionOverview(
            Long batchId, AgentToolExecutionContext context) {
        Long taskId = batchService.getById(batchId).getTaskId();
        support.requireTaskContext(context, taskId);
        List<HerbBatchImageVO> images =
                batchImageService.listByBatch(batchId, new HerbBatchImageQueryRequest());
        List<RecognitionToolData.ImageOverview> overviews =
                images.stream().map(this::toOverview).toList();
        return aggregate(batchId, overviews);
    }

    public RecognitionToolData.TaskImageList listLowConfidenceImages(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        List<RecognitionToolData.ImageOverview> images =
                batchImageService.listByTask(taskId).stream()
                        .filter(image -> image.getFinalConfidence() != null)
                        .filter(
                                image ->
                                        image.getFinalConfidence()
                                                        .compareTo(LOW_CONFIDENCE_THRESHOLD)
                                                < 0)
                        .map(this::toOverview)
                        .toList();
        return new RecognitionToolData.TaskImageList(taskId, images);
    }

    public RecognitionToolData.TaskImageList listUnrecognizedImages(
            Long taskId, AgentToolExecutionContext context) {
        support.requireTaskContext(context, taskId);
        List<RecognitionToolData.ImageOverview> images =
                batchImageService.listByTask(taskId).stream()
                        .filter(image -> image.getIdentificationResultId() == null)
                        .map(this::toOverview)
                        .toList();
        return new RecognitionToolData.TaskImageList(taskId, images);
    }

    @Override
    public List<AgentToolDefinition> definitions() {
        return List.of(
                support.readDefinition(IMAGE_OVERVIEW, "图片识别概览", "读取图片已有识别结果和 TopK"),
                support.readDefinition(BATCH_OVERVIEW, "批次识别概览", "批量读取批次图片识别摘要"),
                support.readDefinition(LOW_CONFIDENCE, "低置信度图片", "批量筛选已有低置信度结果"),
                support.readDefinition(UNRECOGNIZED, "未识别图片", "批量筛选尚无识别结果的图片"));
    }

    private RecognitionToolData.BatchOverview aggregate(
            Long batchId, List<RecognitionToolData.ImageOverview> images) {
        int recognized =
                (int) images.stream().filter(i -> i.recognitionStatus().code() != null).count();
        int low =
                (int)
                        images.stream()
                                .filter(i -> i.highestSimilarity() != null)
                                .filter(
                                        i ->
                                                i.highestSimilarity()
                                                                .compareTo(LOW_CONFIDENCE_THRESHOLD)
                                                        < 0)
                                .count();
        int needReview =
                (int) images.stream().filter(i -> Boolean.TRUE.equals(i.needReview())).count();
        return new RecognitionToolData.BatchOverview(
                batchId,
                images.size(),
                recognized,
                images.size() - recognized,
                low,
                needReview,
                images);
    }

    private RecognitionToolData.ImageOverview toOverview(HerbBatchImageVO image) {
        return new RecognitionToolData.ImageOverview(
                image.getImageId(),
                image.getBatchId(),
                image.getImageCode(),
                support.browserUrl(image.getImageUrl()),
                image.getImageType(),
                support.status(image.getIdentificationResultId() == null ? null : "success"),
                List.of(),
                image.getFinalConfidence(),
                "doubao".equalsIgnoreCase(image.getResultSource())
                        ? image.getFinalSpeciesName()
                        : null,
                image.getNeedReview(),
                image.getFinalSpeciesName(),
                image.getResultSource(),
                null,
                image.getIdentifyTime());
    }

    private RecognitionToolData.ImageOverview toOverview(Long imageId, HerbRecognitionVO result) {
        if (result == null) {
            return new RecognitionToolData.ImageOverview(
                    imageId,
                    null,
                    null,
                    null,
                    null,
                    support.status(null),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        List<RecognitionToolData.Candidate> candidates =
                result.getCandidates() == null
                        ? List.of()
                        : result.getCandidates().stream().map(this::toCandidate).toList();
        BigDecimal highest =
                candidates.stream()
                        .map(RecognitionToolData.Candidate::similarity)
                        .filter(value -> value != null)
                        .max(Comparator.naturalOrder())
                        .orElse(result.getConfidence());
        return new RecognitionToolData.ImageOverview(
                imageId,
                null,
                result.getImageCode(),
                support.browserUrl(result.getImageUrl()),
                null,
                support.status(result.getRecognitionStatus()),
                candidates,
                highest,
                "doubao".equalsIgnoreCase(result.getRecognitionSource())
                        ? result.getPredictedSpeciesName()
                        : null,
                result.getNeedReview(),
                result.getPredictedSpeciesName(),
                result.getRecognitionSource(),
                null,
                result.getRecognitionTime());
    }

    private RecognitionToolData.Candidate toCandidate(HerbRecognitionCandidateVO candidate) {
        return new RecognitionToolData.Candidate(
                candidate.getSpeciesId(),
                candidate.getName(),
                candidate.getConfidence(),
                candidate.getSimilarity(),
                candidate.getRank());
    }
}
