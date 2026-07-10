package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.spectrum.constant.HerbMatchResultConstants;
import com.bdis.modules.spectrum.constant.HerbProcessStatusConstants;
import com.bdis.modules.spectrum.dto.HerbImageMatchQueryRequest;
import com.bdis.modules.spectrum.dto.HerbImageMatchRequest;
import com.bdis.modules.spectrum.entity.SpectrumComparisonEntity;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbImageMatchMapper;
import com.bdis.modules.spectrum.service.HerbImageMatchService;
import com.bdis.modules.spectrum.util.VectorSimilarityUtils;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import com.bdis.modules.spectrum.vo.HerbAtlasFeatureCandidateVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchCandidateVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchPageVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class HerbImageMatchServiceImpl implements HerbImageMatchService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 50;
    private static final BigDecimal HIGH_CONFIDENCE = new BigDecimal("0.8500");
    private static final BigDecimal MEDIUM_CONFIDENCE = new BigDecimal("0.6000");
    private static final DateTimeFormatter BATCH_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HerbImageMapper herbImageMapper;
    private final HerbImageFeatureMapper herbImageFeatureMapper;
    private final HerbImageMatchMapper herbImageMatchMapper;
    private final ObjectMapper objectMapper;

    public HerbImageMatchServiceImpl(
            HerbImageMapper herbImageMapper,
            HerbImageFeatureMapper herbImageFeatureMapper,
            HerbImageMatchMapper herbImageMatchMapper,
            ObjectMapper objectMapper) {
        this.herbImageMapper = herbImageMapper;
        this.herbImageFeatureMapper = herbImageFeatureMapper;
        this.herbImageMatchMapper = herbImageMatchMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public HerbImageMatchVO match(Long imageId, HerbImageMatchRequest request) {
        HerbImageEntity image = getActiveImage(imageId);
        HerbImageMatchRequest safeRequest =
                request == null ? new HerbImageMatchRequest() : request;
        if (!Boolean.TRUE.equals(safeRequest.getForceRefresh())
                && herbImageMatchMapper.countActiveByImageId(imageId) > 0) {
            return toMatchVO(image, herbImageMatchMapper.selectLatestByImageId(imageId));
        }

        FeatureExtractResultVO imageFeature =
                herbImageFeatureMapper.selectLatestSuccessByImageId(imageId);
        if (imageFeature == null || !StringUtils.hasText(imageFeature.getFeatureVector())) {
            throw new BusinessException("Image feature not found, please extract feature first");
        }
        List<Double> imageVector = parseVector(imageFeature.getFeatureVector(), "image feature");
        List<HerbAtlasFeatureCandidateVO> atlasFeatures =
                herbImageMatchMapper.selectAtlasFeatureCandidates(safeRequest.getSpeciesId());
        if (CollectionUtils.isEmpty(atlasFeatures)) {
            throw new BusinessException(
                    "Atlas feature not found, please run atlas feature batch extraction first");
        }

        List<RankedCandidate> rankedCandidates =
                atlasFeatures.stream()
                        .map(candidate -> tryRank(candidate, imageVector))
                        .filter(candidate -> candidate != null)
                        .sorted(Comparator.comparing(RankedCandidate::similarity).reversed())
                        .limit(normalizeTopK(safeRequest.getTopK()))
                        .toList();
        if (CollectionUtils.isEmpty(rankedCandidates)) {
            throw new BusinessException("No valid atlas feature can be matched");
        }

        LocalDateTime now = LocalDateTime.now();
        String matchBatchNo = generateMatchBatchNo(now);
        String finalResult = resolveMatchResult(rankedCandidates.get(0).similarity());
        List<SpectrumComparisonEntity> matches =
                buildMatches(imageId, rankedCandidates, finalResult, matchBatchNo, now);
        herbImageMatchMapper.batchInsertMatches(matches);
        updateImageProcessStatus(imageId, HerbProcessStatusConstants.MATCHED);
        return toMatchVO(image, herbImageMatchMapper.selectLatestByImageId(imageId));
    }

    @Override
    public HerbImageMatchVO latest(Long imageId) {
        HerbImageEntity image = getActiveImage(imageId);
        return toMatchVO(image, herbImageMatchMapper.selectLatestByImageId(imageId));
    }

    @Override
    public List<HerbImageMatchPageVO> listByImageId(Long imageId) {
        getActiveImage(imageId);
        return herbImageMatchMapper.selectByImageId(imageId);
    }

    @Override
    public PageResult<HerbImageMatchPageVO> page(HerbImageMatchQueryRequest request) {
        HerbImageMatchQueryRequest safeRequest =
                request == null ? new HerbImageMatchQueryRequest() : request;
        normalizePageRequest(safeRequest);
        Long total = herbImageMatchMapper.countPage(safeRequest);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbImageMatchPageVO> records =
                herbImageMatchMapper.selectPage(safeRequest, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    private HerbImageEntity getActiveImage(Long imageId) {
        if (imageId == null) {
            throw new BusinessException("Image id is required");
        }
        HerbImageEntity image = herbImageMapper.selectActiveById(imageId);
        if (image == null) {
            throw new BusinessException("Herb image not found");
        }
        return image;
    }

    private RankedCandidate tryRank(
            HerbAtlasFeatureCandidateVO candidate, List<Double> imageVector) {
        try {
            List<Double> atlasVector = parseVector(candidate.getFeatureVector(), "atlas feature");
            BigDecimal similarity =
                    VectorSimilarityUtils.toScore(
                            VectorSimilarityUtils.cosineSimilarity(imageVector, atlasVector));
            return new RankedCandidate(candidate, similarity);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private List<Double> parseVector(String featureVector, String targetName) {
        if (!StringUtils.hasText(featureVector)) {
            throw new BusinessException(targetName + " vector is empty");
        }
        try {
            return objectMapper.readValue(featureVector, new TypeReference<List<Double>>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Invalid " + targetName + " vector");
        }
    }

    private List<SpectrumComparisonEntity> buildMatches(
            Long imageId,
            List<RankedCandidate> rankedCandidates,
            String finalResult,
            String matchBatchNo,
            LocalDateTime now) {
        return rankedCandidates.stream()
                .map(
                        candidate -> {
                            int rank = rankedCandidates.indexOf(candidate) + 1;
                            SpectrumComparisonEntity match = new SpectrumComparisonEntity();
                            match.setImageId(imageId);
                            match.setAtlasId(candidate.atlas().getAtlasId());
                            match.setSimilarityScore(candidate.similarity());
                            match.setMatchRank(rank);
                            match.setMatchResult(
                                    rank == 1 ? finalResult : HerbMatchResultConstants.CANDIDATE);
                            match.setMatchBatchNo(matchBatchNo);
                            match.setMatchTime(now);
                            match.setCreateTime(now);
                            match.setUpdateTime(now);
                            match.setDeleted(0);
                            return match;
                        })
                .toList();
    }

    private HerbImageMatchVO toMatchVO(
            HerbImageEntity image, List<HerbImageMatchPageVO> records) {
        HerbImageMatchVO vo = new HerbImageMatchVO();
        vo.setImageId(image.getId());
        vo.setImageCode(image.getImageCode());
        if (CollectionUtils.isEmpty(records)) {
            vo.setNeedReview(true);
            vo.setSuggestion("No local atlas match records found");
            return vo;
        }
        HerbImageMatchPageVO best = records.get(0);
        String finalResult = best.getMatchResult();
        vo.setBestSpeciesId(best.getSpeciesId());
        vo.setBestSpeciesName(best.getSpeciesName());
        vo.setBestAtlasId(best.getAtlasId());
        vo.setBestAtlasImageUrl(best.getAtlasImageUrl());
        vo.setBestSimilarity(best.getSimilarityScore());
        vo.setMatchResult(finalResult);
        vo.setNeedReview(!HerbMatchResultConstants.MATCHED.equals(finalResult));
        vo.setSuggestion(suggestion(finalResult));
        vo.setCandidates(records.stream().map(this::toCandidateVO).toList());
        return vo;
    }

    private HerbImageMatchCandidateVO toCandidateVO(HerbImageMatchPageVO record) {
        HerbImageMatchCandidateVO candidate = new HerbImageMatchCandidateVO();
        candidate.setRank(record.getMatchRank());
        candidate.setAtlasId(record.getAtlasId());
        candidate.setAtlasCode(record.getAtlasCode());
        candidate.setAtlasImageUrl(record.getAtlasImageUrl());
        candidate.setSpeciesId(record.getSpeciesId());
        candidate.setSpeciesName(record.getSpeciesName());
        candidate.setSimilarity(record.getSimilarityScore());
        candidate.setMatchResult(record.getMatchResult());
        return candidate;
    }

    private String resolveMatchResult(BigDecimal bestSimilarity) {
        if (bestSimilarity.compareTo(HIGH_CONFIDENCE) >= 0) {
            return HerbMatchResultConstants.MATCHED;
        }
        if (bestSimilarity.compareTo(MEDIUM_CONFIDENCE) >= 0) {
            return HerbMatchResultConstants.UNCERTAIN;
        }
        return HerbMatchResultConstants.LOW_CONFIDENCE;
    }

    private String suggestion(String matchResult) {
        return switch (matchResult) {
            case HerbMatchResultConstants.MATCHED -> "Local atlas similarity is high and can be used as a preliminary result";
            case HerbMatchResultConstants.UNCERTAIN -> "Local atlas returned candidates, manual review is required";
            case HerbMatchResultConstants.LOW_CONFIDENCE -> "Local atlas cannot make a reliable judgment, manual review is required";
            default -> "Candidate result for local atlas review";
        };
    }

    private String generateMatchBatchNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "MATCH_" + now.format(BATCH_TIME_FORMAT) + "_" + random;
    }

    private void updateImageProcessStatus(Long imageId, String processStatus) {
        HerbImageEntity update = new HerbImageEntity();
        update.setId(imageId);
        update.setProcessStatus(processStatus);
        update.setUpdateTime(LocalDateTime.now());
        herbImageMapper.updateProcessStatus(update);
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK < 1) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private void normalizePageRequest(HerbImageMatchQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private record RankedCandidate(
            HerbAtlasFeatureCandidateVO atlas, BigDecimal similarity) {}
}
