package com.bdis.modules.collection.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchImageStatusConstants;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.constant.HerbQualityLevelConstants;
import com.bdis.modules.collection.dto.HerbBatchConfirmRequest;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.service.HerbBatchSummaryService;
import com.bdis.modules.collection.vo.HerbBatchIdentificationItemVO;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchSpeciesStatVO;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbBatchSummaryServiceImpl implements HerbBatchSummaryService {

    private static final BigDecimal MAIN_SPECIES_THRESHOLD = new BigDecimal("0.6000");

    private final HerbBatchMapper herbBatchMapper;
    private final HerbBatchImageMapper herbBatchImageMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;

    public HerbBatchSummaryServiceImpl(
            HerbBatchMapper herbBatchMapper,
            HerbBatchImageMapper herbBatchImageMapper,
            HerbSpeciesMapper herbSpeciesMapper) {
        this.herbBatchMapper = herbBatchMapper;
        this.herbBatchImageMapper = herbBatchImageMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
    }

    @Override
    @Transactional
    public HerbBatchSummaryVO refreshSummary(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        if (HerbBatchStatusConstants.ARCHIVED.equals(batch.getBatchStatus())
                || HerbBatchStatusConstants.CANCELLED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Archived or cancelled batch cannot refresh summary");
        }

        List<HerbBatchIdentificationItemVO> items = listIdentificationItems(batch.getId());
        HerbBatchSummaryVO summary = calculateSummary(batch, items);
        applySummary(batch, summary);
        int affected = herbBatchMapper.updateStatisticsById(batch);
        if (affected == 0) {
            throw new BusinessException("Failed to update batch summary");
        }
        return summary;
    }

    @Override
    public HerbBatchSummaryVO getSummary(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        List<HerbBatchIdentificationItemVO> items = listIdentificationItems(batch.getId());
        HerbBatchSummaryVO summary = baseSummary(batch, items);
        summary.setSpeciesStats(buildSpeciesStats(items, identifiedCount(items)));
        return summary;
    }

    @Override
    public List<HerbBatchIdentificationItemVO> listIdentificationItems(Long batchId) {
        getActiveBatch(batchId);
        HerbBatchImageQueryRequest query = new HerbBatchImageQueryRequest();
        query.setBindStatus(HerbBatchImageStatusConstants.BOUND);
        return herbBatchImageMapper.selectBatchImagesWithIdentification(batchId, query).stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    @Transactional
    public HerbBatchSummaryVO confirm(Long batchId, HerbBatchConfirmRequest request) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        if (HerbBatchStatusConstants.ARCHIVED.equals(batch.getBatchStatus())
                || HerbBatchStatusConstants.CANCELLED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Archived or cancelled batch cannot confirm summary");
        }
        if (request == null) {
            throw new BusinessException("Confirm request is required");
        }
        if (StringUtils.hasText(request.getQualityLevel())
                && !HerbQualityLevelConstants.VALID_LEVELS.contains(request.getQualityLevel())) {
            throw new BusinessException("Invalid quality level");
        }

        String finalSpeciesName = request.getFinalSpeciesName();
        if (request.getFinalSpeciesId() != null) {
            HerbEntity species = herbSpeciesMapper.selectActiveById(request.getFinalSpeciesId());
            if (species == null) {
                throw new BusinessException("Herb species not found");
            }
            if (!StringUtils.hasText(finalSpeciesName)) {
                finalSpeciesName = species.getHerbName();
            }
        }

        batch.setFinalSpeciesId(request.getFinalSpeciesId());
        batch.setFinalSpeciesName(finalSpeciesName);
        batch.setQualityLevel(request.getQualityLevel());
        batch.setQualityScore(request.getQualityScore());
        batch.setEvaluationSummary(request.getEvaluationSummary());
        batch.setBatchStatus(HerbBatchStatusConstants.CONFIRMED);
        batch.setRemark(buildConfirmRemark(request));
        batch.setUpdatedAt(LocalDateTime.now());
        int affected = herbBatchMapper.updateStatisticsById(batch);
        if (affected == 0) {
            throw new BusinessException("Failed to confirm batch summary");
        }
        return getSummary(batch.getId());
    }

    private HerbBatchSummaryVO calculateSummary(
            HerbBatchEntity batch, List<HerbBatchIdentificationItemVO> items) {
        int imageCount = items.size();
        int identifiedCount = identifiedCount(items);
        int reviewedCount =
                (int)
                        items.stream()
                                .filter(item -> "confirmed".equals(item.getReviewStatus()))
                                .count();
        int needReviewCount =
                (int)
                        items.stream()
                                .filter(
                                        item ->
                                                Boolean.TRUE.equals(item.getNeedReview())
                                                        || "pending".equals(item.getReviewStatus()))
                                .count();
        List<HerbBatchSpeciesStatVO> speciesStats = buildSpeciesStats(items, identifiedCount);
        HerbBatchSpeciesStatVO mainSpecies = speciesStats.isEmpty() ? null : speciesStats.get(0);
        BigDecimal avgSimilarity = calculateAvgSimilarity(items);
        BigDecimal qualityScore =
                avgSimilarity == null
                        ? null
                        : avgSimilarity
                                .multiply(new BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP);
        String qualityLevel = resolveQualityLevel(qualityScore);
        String batchStatus =
                resolveBatchStatus(
                        batch, imageCount, identifiedCount, needReviewCount, mainSpecies);
        String evaluationSummary =
                buildEvaluationSummary(
                        imageCount,
                        identifiedCount,
                        reviewedCount,
                        needReviewCount,
                        mainSpecies,
                        avgSimilarity,
                        qualityLevel,
                        batchStatus);

        HerbBatchSummaryVO summary = baseSummary(batch, items);
        summary.setImageCount(imageCount);
        summary.setIdentifiedCount(identifiedCount);
        summary.setReviewedCount(reviewedCount);
        summary.setNeedReviewCount(needReviewCount);
        summary.setFinalSpeciesId(mainSpecies == null ? null : mainSpecies.getFinalSpeciesId());
        summary.setFinalSpeciesName(mainSpecies == null ? null : mainSpecies.getFinalSpeciesName());
        summary.setMainSpeciesRatio(
                mainSpecies == null ? BigDecimal.ZERO.setScale(4) : mainSpecies.getRatio());
        summary.setAvgSimilarity(avgSimilarity);
        summary.setQualityScore(qualityScore);
        summary.setQualityLevel(qualityLevel);
        summary.setBatchStatus(batchStatus);
        summary.setEvaluationSummary(evaluationSummary);
        summary.setSpeciesStats(speciesStats);
        return summary;
    }

    private void applySummary(HerbBatchEntity batch, HerbBatchSummaryVO summary) {
        batch.setImageCount(summary.getImageCount());
        batch.setIdentifiedCount(summary.getIdentifiedCount());
        batch.setReviewedCount(summary.getReviewedCount());
        batch.setNeedReviewCount(summary.getNeedReviewCount());
        batch.setFinalSpeciesId(summary.getFinalSpeciesId());
        batch.setFinalSpeciesName(summary.getFinalSpeciesName());
        batch.setAvgSimilarity(summary.getAvgSimilarity());
        batch.setQualityLevel(summary.getQualityLevel());
        batch.setQualityScore(summary.getQualityScore());
        batch.setEvaluationSummary(summary.getEvaluationSummary());
        batch.setBatchStatus(summary.getBatchStatus());
        batch.setUpdatedAt(LocalDateTime.now());
    }

    private HerbBatchSummaryVO baseSummary(
            HerbBatchEntity batch, List<HerbBatchIdentificationItemVO> items) {
        HerbBatchSummaryVO summary = new HerbBatchSummaryVO();
        summary.setBatchId(batch.getId());
        summary.setBatchCode(batch.getBatchCode());
        summary.setBatchName(batch.getBatchName());
        summary.setImageCount(batch.getImageCount());
        summary.setIdentifiedCount(batch.getIdentifiedCount());
        summary.setReviewedCount(batch.getReviewedCount());
        summary.setNeedReviewCount(batch.getNeedReviewCount());
        summary.setFinalSpeciesId(batch.getFinalSpeciesId());
        summary.setFinalSpeciesName(batch.getFinalSpeciesName());
        summary.setAvgSimilarity(batch.getAvgSimilarity());
        summary.setQualityScore(batch.getQualityScore());
        summary.setQualityLevel(batch.getQualityLevel());
        summary.setBatchStatus(batch.getBatchStatus());
        summary.setEvaluationSummary(batch.getEvaluationSummary());
        summary.setItems(items);
        return summary;
    }

    private List<HerbBatchSpeciesStatVO> buildSpeciesStats(
            List<HerbBatchIdentificationItemVO> items, int identifiedCount) {
        Map<String, HerbBatchSpeciesStatVO> stats = new LinkedHashMap<>();
        for (HerbBatchIdentificationItemVO item : items) {
            if (item.getIdentificationResultId() == null
                    || (item.getFinalSpeciesId() == null
                            && !StringUtils.hasText(item.getFinalSpeciesName()))) {
                continue;
            }
            String key =
                    item.getFinalSpeciesId() != null
                            ? "id:" + item.getFinalSpeciesId()
                            : "name:" + item.getFinalSpeciesName();
            HerbBatchSpeciesStatVO stat =
                    stats.computeIfAbsent(key, unused -> new HerbBatchSpeciesStatVO());
            stat.setFinalSpeciesId(item.getFinalSpeciesId());
            stat.setFinalSpeciesName(item.getFinalSpeciesName());
            stat.setCount(stat.getCount() == null ? 1 : stat.getCount() + 1);
        }
        List<HerbBatchSpeciesStatVO> result = new ArrayList<>(stats.values());
        for (HerbBatchSpeciesStatVO stat : result) {
            stat.setRatio(
                    identifiedCount == 0
                            ? BigDecimal.ZERO.setScale(4)
                            : BigDecimal.valueOf(stat.getCount())
                                    .divide(
                                            BigDecimal.valueOf(identifiedCount),
                                            4,
                                            RoundingMode.HALF_UP));
        }
        result.sort(Comparator.comparing(HerbBatchSpeciesStatVO::getCount).reversed());
        return result;
    }

    private BigDecimal calculateAvgSimilarity(List<HerbBatchIdentificationItemVO> items) {
        List<BigDecimal> values =
                items.stream()
                        .map(HerbBatchIdentificationItemVO::getFinalConfidence)
                        .filter(Objects::nonNull)
                        .toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private String resolveQualityLevel(BigDecimal qualityScore) {
        if (qualityScore == null) {
            return HerbQualityLevelConstants.UNKNOWN;
        }
        if (qualityScore.compareTo(new BigDecimal("90")) >= 0) {
            return HerbQualityLevelConstants.EXCELLENT;
        }
        if (qualityScore.compareTo(new BigDecimal("80")) >= 0) {
            return HerbQualityLevelConstants.GOOD;
        }
        if (qualityScore.compareTo(new BigDecimal("60")) >= 0) {
            return HerbQualityLevelConstants.NORMAL;
        }
        return HerbQualityLevelConstants.POOR;
    }

    private String resolveBatchStatus(
            HerbBatchEntity batch,
            int imageCount,
            int identifiedCount,
            int needReviewCount,
            HerbBatchSpeciesStatVO mainSpecies) {
        if (HerbBatchStatusConstants.DRAFT.equals(batch.getBatchStatus())
                || HerbBatchStatusConstants.COLLECTING.equals(batch.getBatchStatus())) {
            return batch.getBatchStatus();
        }
        if (imageCount == 0) {
            return batch.getBatchStatus();
        }
        if (identifiedCount < imageCount) {
            return HerbBatchStatusConstants.IDENTIFYING;
        }
        if (mainSpecies == null || mainSpecies.getRatio().compareTo(MAIN_SPECIES_THRESHOLD) < 0) {
            return HerbBatchStatusConstants.REVIEWING;
        }
        if (needReviewCount > 0) {
            return HerbBatchStatusConstants.REVIEWING;
        }
        return HerbBatchStatusConstants.CONFIRMED;
    }

    private String buildEvaluationSummary(
            int imageCount,
            int identifiedCount,
            int reviewedCount,
            int needReviewCount,
            HerbBatchSpeciesStatVO mainSpecies,
            BigDecimal avgSimilarity,
            String qualityLevel,
            String batchStatus) {
        if (imageCount == 0) {
            return "该批次暂无绑定图片。";
        }
        if (identifiedCount < imageCount) {
            return "该批次仍有图片未生成识别结论，建议先完成图谱识别和人工复核。";
        }
        if (HerbBatchStatusConstants.REVIEWING.equals(batchStatus)
                && (mainSpecies == null
                        || mainSpecies.getRatio().compareTo(MAIN_SPECIES_THRESHOLD) < 0)) {
            return "该批次识别结果存在不一致情况，最高占比药材不足 60%，建议人工复核批次下所有图片后再确认。";
        }
        String speciesName = mainSpecies == null ? "未知药材" : mainSpecies.getFinalSpeciesName();
        if (needReviewCount > 0) {
            return "该批次共绑定 "
                    + imageCount
                    + " 张图片，"
                    + identifiedCount
                    + " 张已完成识别，其中 "
                    + needReviewCount
                    + " 张仍需复核，主要识别结果为"
                    + speciesName
                    + "，建议人工完成复核后再确认批次结果。";
        }
        return "该批次共绑定 "
                + imageCount
                + " 张图片，"
                + identifiedCount
                + " 张已完成识别，"
                + reviewedCount
                + " 张已确认，主要识别结果为"
                + speciesName
                + "，平均置信度为 "
                + (avgSimilarity == null ? "未知" : avgSimilarity)
                + "，质量等级为 "
                + qualityLevel
                + "。";
    }

    private int identifiedCount(List<HerbBatchIdentificationItemVO> items) {
        return (int)
                items.stream().filter(item -> item.getIdentificationResultId() != null).count();
    }

    private HerbBatchIdentificationItemVO toItem(HerbBatchImageVO image) {
        HerbBatchIdentificationItemVO item = new HerbBatchIdentificationItemVO();
        item.setBatchImageId(image.getId());
        item.setImageId(image.getImageId());
        item.setImageCode(image.getImageCode());
        item.setImageUrl(image.getImageUrl());
        item.setImageRole(image.getImageRole());
        item.setIsPrimary(image.getIsPrimary());
        item.setIdentificationResultId(image.getIdentificationResultId());
        item.setFinalSpeciesId(image.getFinalSpeciesId());
        item.setFinalSpeciesName(image.getFinalSpeciesName());
        item.setFinalConfidence(image.getFinalConfidence());
        item.setResultSource(image.getResultSource());
        item.setMatchResult(image.getMatchResult());
        item.setNeedReview(image.getNeedReview());
        item.setReviewStatus(image.getReviewStatus());
        item.setSuggestion(image.getSuggestion());
        item.setIdentifyTime(image.getIdentifyTime());
        return item;
    }

    private String buildConfirmRemark(HerbBatchConfirmRequest request) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(request.getRemark())) {
            builder.append(request.getRemark());
        }
        if (request.getReviewerId() != null || StringUtils.hasText(request.getReviewerName())) {
            if (!builder.isEmpty()) {
                builder.append("；");
            }
            builder.append("确认人：");
            if (request.getReviewerId() != null) {
                builder.append(request.getReviewerId());
            }
            if (StringUtils.hasText(request.getReviewerName())) {
                if (request.getReviewerId() != null) {
                    builder.append("/");
                }
                builder.append(request.getReviewerName());
            }
        }
        return builder.toString();
    }

    private HerbBatchEntity getActiveBatch(Long batchId) {
        if (batchId == null) {
            throw new BusinessException("Batch id is required");
        }
        HerbBatchEntity batch = herbBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("Batch not found");
        }
        return batch;
    }
}
