package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.config.HerbIdentificationProperties;
import com.bdis.modules.spectrum.constant.HerbMatchResultConstants;
import com.bdis.modules.spectrum.constant.HerbProcessStatusConstants;
import com.bdis.modules.spectrum.constant.HerbResultSourceConstants;
import com.bdis.modules.spectrum.constant.HerbReviewStatusConstants;
import com.bdis.modules.spectrum.dto.HerbIdentificationQueryRequest;
import com.bdis.modules.spectrum.dto.HerbIdentificationReviewRequest;
import com.bdis.modules.spectrum.dto.HerbIdentifyRequest;
import com.bdis.modules.spectrum.dto.HerbImageMatchRequest;
import com.bdis.modules.spectrum.entity.HerbIdentificationResultEntity;
import com.bdis.modules.spectrum.mapper.HerbIdentificationResultMapper;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbImageMatchMapper;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import com.bdis.modules.spectrum.service.HerbImageMatchService;
import com.bdis.modules.spectrum.service.HerbRecognitionService;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import com.bdis.modules.spectrum.vo.HerbIdentificationPageVO;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchCandidateVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchVO;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class HerbIdentificationServiceImpl implements HerbIdentificationService {

    private final HerbImageMapper herbImageMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final HerbImageFeatureMapper herbImageFeatureMapper;
    private final HerbImageMatchMapper herbImageMatchMapper;
    private final HerbIdentificationResultMapper identificationResultMapper;
    private final HerbFeatureService herbFeatureService;
    private final HerbImageMatchService herbImageMatchService;
    private final HerbRecognitionService herbRecognitionService;
    private final HerbIdentificationProperties properties;
    private final ObjectMapper objectMapper;

    public HerbIdentificationServiceImpl(
            HerbImageMapper herbImageMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            HerbImageFeatureMapper herbImageFeatureMapper,
            HerbImageMatchMapper herbImageMatchMapper,
            HerbIdentificationResultMapper identificationResultMapper,
            HerbFeatureService herbFeatureService,
            HerbImageMatchService herbImageMatchService,
            HerbRecognitionService herbRecognitionService,
            HerbIdentificationProperties properties,
            ObjectMapper objectMapper) {
        this.herbImageMapper = herbImageMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.herbImageFeatureMapper = herbImageFeatureMapper;
        this.herbImageMatchMapper = herbImageMatchMapper;
        this.identificationResultMapper = identificationResultMapper;
        this.herbFeatureService = herbFeatureService;
        this.herbImageMatchService = herbImageMatchService;
        this.herbRecognitionService = herbRecognitionService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public HerbIdentificationVO identify(Long imageId, HerbIdentifyRequest request) {
        HerbImageEntity image = getActiveImage(imageId);
        HerbIdentifyRequest safeRequest = request == null ? new HerbIdentifyRequest() : request;
        ensureImageFeature(imageId, Boolean.TRUE.equals(safeRequest.getForceRefresh()));
        HerbImageMatchVO localMatch =
                herbImageMatchService.match(imageId, toMatchRequest(safeRequest));
        HerbRecognitionVO doubaoRecognition = null;
        String doubaoError = null;
        HerbIdentificationResultEntity result =
                buildLocalResult(image, localMatch, LocalDateTime.now());
        if (isLowConfidence(localMatch)) {
            DoubaoReviewResult reviewResult = tryDoubaoReview(imageId);
            doubaoRecognition = reviewResult.recognition();
            doubaoError = reviewResult.errorMessage();
            applyLowConfidenceResult(result, localMatch, doubaoRecognition, doubaoError);
        }
        result.setRawSummary(rawSummary(localMatch, doubaoRecognition, doubaoError));
        identificationResultMapper.insertResult(result);
        updateImageProcessStatus(imageId, HerbProcessStatusConstants.IDENTIFIED);
        return toVO(result, image, localMatch.getCandidates(), doubaoRecognition);
    }

    @Override
    public HerbIdentificationVO latest(Long imageId) {
        HerbImageEntity image = getActiveImage(imageId);
        HerbIdentificationPageVO result = identificationResultMapper.selectLatestByImageId(imageId);
        if (result == null) {
            return emptyResult(image);
        }
        return toVO(result, image);
    }

    @Override
    public List<HerbIdentificationVO> history(Long imageId) {
        HerbImageEntity image = getActiveImage(imageId);
        return identificationResultMapper.selectByImageId(imageId).stream()
                .map(result -> toVO(result, image))
                .toList();
    }

    @Override
    public PageResult<HerbIdentificationPageVO> page(HerbIdentificationQueryRequest request) {
        HerbIdentificationQueryRequest safeRequest =
                request == null ? new HerbIdentificationQueryRequest() : request;
        normalizePageRequest(safeRequest);
        Long total = identificationResultMapper.countPage(safeRequest);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbIdentificationPageVO> records =
                identificationResultMapper.selectPage(
                        safeRequest, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    @Override
    @Transactional
    public HerbIdentificationVO review(Long id, HerbIdentificationReviewRequest request) {
        HerbIdentificationResultEntity result = identificationResultMapper.selectActiveById(id);
        if (result == null) {
            throw new BusinessException("Identification result not found");
        }
        HerbIdentificationReviewRequest safeRequest =
                request == null ? new HerbIdentificationReviewRequest() : request;
        HerbEntity species = resolveReviewSpecies(safeRequest);
        result.setFinalSpeciesId(
                species == null ? safeRequest.getFinalSpeciesId() : species.getId());
        result.setFinalSpeciesName(resolveReviewSpeciesName(safeRequest, species));
        applyReviewStatus(result, safeRequest);
        result.setReviewComment(safeRequest.getReviewComment());
        result.setReviewerId(safeRequest.getReviewerId());
        result.setReviewerName(safeRequest.getReviewerName());
        result.setReviewTime(LocalDateTime.now());
        result.setUpdatedAt(result.getReviewTime());
        identificationResultMapper.updateReviewResult(result);
        HerbImageEntity image = getActiveImage(result.getImageId());
        updateImageProcessStatus(image.getId(), HerbProcessStatusConstants.REVIEWED);
        return latest(image.getId());
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

    private void ensureImageFeature(Long imageId, boolean forceRefresh) {
        FeatureExtractResultVO feature =
                herbImageFeatureMapper.selectLatestSuccessByImageId(imageId);
        if (forceRefresh || feature == null) {
            herbFeatureService.extractImageFeature(imageId);
        }
    }

    private HerbImageMatchRequest toMatchRequest(HerbIdentifyRequest request) {
        HerbImageMatchRequest matchRequest = new HerbImageMatchRequest();
        matchRequest.setTopK(request.getTopK() == null ? properties.getTopK() : request.getTopK());
        matchRequest.setSpeciesId(request.getSpeciesId());
        matchRequest.setForceRefresh(request.getForceRefresh());
        return matchRequest;
    }

    private HerbIdentificationResultEntity buildLocalResult(
            HerbImageEntity image, HerbImageMatchVO localMatch, LocalDateTime now) {
        HerbIdentificationResultEntity result = new HerbIdentificationResultEntity();
        result.setImageId(image.getId());
        result.setBestMatchId(resolveBestMatchId(image.getId()));
        result.setFinalSpeciesId(localMatch.getBestSpeciesId());
        result.setFinalSpeciesName(localMatch.getBestSpeciesName());
        result.setFinalConfidence(localMatch.getBestSimilarity());
        result.setResultSource(HerbResultSourceConstants.LOCAL_MATCH);
        result.setMatchResult(resolveMatchResult(localMatch.getBestSimilarity()));
        boolean needReview = !HerbMatchResultConstants.MATCHED.equals(result.getMatchResult());
        result.setNeedReview(needReview ? 1 : 0);
        result.setReviewStatus(
                needReview
                        ? HerbReviewStatusConstants.PENDING
                        : HerbReviewStatusConstants.CONFIRMED);
        result.setSuggestion(localSuggestion(result.getMatchResult()));
        result.setIdentifyTime(now);
        result.setCreatedAt(now);
        result.setUpdatedAt(now);
        result.setIsDeleted(0);
        result.setStatus(1);
        result.setVersion(0);
        return result;
    }

    private void applyLowConfidenceResult(
            HerbIdentificationResultEntity result,
            HerbImageMatchVO localMatch,
            HerbRecognitionVO doubaoRecognition,
            String doubaoError) {
        result.setMatchResult(HerbMatchResultConstants.LOW_CONFIDENCE);
        result.setNeedReview(1);
        result.setReviewStatus(HerbReviewStatusConstants.PENDING);
        if (doubaoRecognition == null) {
            result.setResultSource(HerbResultSourceConstants.LOCAL_MATCH);
            result.setFinalSpeciesId(localMatch.getBestSpeciesId());
            result.setFinalSpeciesName(localMatch.getBestSpeciesName());
            result.setFinalConfidence(localMatch.getBestSimilarity());
            result.setSuggestion(lowConfidenceSuggestion(doubaoError));
            return;
        }
        result.setResultSource(HerbResultSourceConstants.LOCAL_MATCH);
        result.setRecognitionId(doubaoRecognition.getId());
        result.setFinalSpeciesId(localMatch.getBestSpeciesId());
        result.setFinalSpeciesName(localMatch.getBestSpeciesName());
        result.setFinalConfidence(localMatch.getBestSimilarity());
        result.setSuggestion(doubaoSuggestion(localMatch, doubaoRecognition));
    }

    private DoubaoReviewResult tryDoubaoReview(Long imageId) {
        if (!Boolean.TRUE.equals(properties.getEnableDoubaoReview())) {
            return new DoubaoReviewResult(null, "Doubao review is disabled");
        }
        try {
            return new DoubaoReviewResult(herbRecognitionService.recognizeByDoubao(imageId), null);
        } catch (RuntimeException exception) {
            return new DoubaoReviewResult(null, exception.getMessage());
        }
    }

    private boolean isLowConfidence(HerbImageMatchVO localMatch) {
        BigDecimal similarity = localMatch.getBestSimilarity();
        return similarity == null || similarity.compareTo(properties.getMiddleThreshold()) < 0;
    }

    private String resolveMatchResult(BigDecimal similarity) {
        if (similarity != null && similarity.compareTo(properties.getHighThreshold()) >= 0) {
            return HerbMatchResultConstants.MATCHED;
        }
        if (similarity != null && similarity.compareTo(properties.getMiddleThreshold()) >= 0) {
            return HerbMatchResultConstants.UNCERTAIN;
        }
        return HerbMatchResultConstants.LOW_CONFIDENCE;
    }

    private String localSuggestion(String matchResult) {
        return switch (matchResult) {
            case HerbMatchResultConstants.MATCHED ->
                    "Local atlas similarity is high and can be used as a preliminary result";
            case HerbMatchResultConstants.UNCERTAIN ->
                    "Local atlas has candidates but confidence is insufficient, manual review is required";
            default -> "Local atlas cannot make a reliable judgment, manual review is required";
        };
    }

    private Long resolveBestMatchId(Long imageId) {
        List<com.bdis.modules.spectrum.vo.HerbImageMatchPageVO> matches =
                herbImageMatchMapper.selectLatestByImageId(imageId);
        if (CollectionUtils.isEmpty(matches)) {
            return null;
        }
        return matches.get(0).getId();
    }

    private HerbEntity resolveReviewSpecies(HerbIdentificationReviewRequest request) {
        if (request.getFinalSpeciesId() == null) {
            return null;
        }
        HerbEntity species = herbSpeciesMapper.selectActiveById(request.getFinalSpeciesId());
        if (species == null) {
            throw new BusinessException("Herb species not found");
        }
        return species;
    }

    private String resolveReviewSpeciesName(
            HerbIdentificationReviewRequest request, HerbEntity species) {
        if (StringUtils.hasText(request.getFinalSpeciesName())) {
            return request.getFinalSpeciesName();
        }
        return species == null ? null : species.getHerbName();
    }

    private String rawSummary(
            HerbImageMatchVO localMatch, HerbRecognitionVO doubaoRecognition, String doubaoError) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("localCandidates", localMatch.getCandidates());
        summary.put("bestSimilarity", localMatch.getBestSimilarity());
        summary.put("localMatchResult", localMatch.getMatchResult());
        summary.put("doubaoRecognition", doubaoRecognition);
        summary.put("doubaoError", doubaoError);
        summary.put(
                "doubaoAgreedWithLocalCandidate",
                doubaoAgreedWithLocalCandidate(localMatch, doubaoRecognition));
        summary.put(
                "thresholds",
                Map.of(
                        "highThreshold",
                        properties.getHighThreshold(),
                        "middleThreshold",
                        properties.getMiddleThreshold()));
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Failed to serialize identification summary");
        }
    }

    private HerbIdentificationVO toVO(
            HerbIdentificationResultEntity result,
            HerbImageEntity image,
            List<HerbImageMatchCandidateVO> candidates,
            HerbRecognitionVO doubaoRecognition) {
        HerbIdentificationVO vo = new HerbIdentificationVO();
        vo.setId(result.getId());
        vo.setImageId(result.getImageId());
        vo.setImageCode(image.getImageNo());
        vo.setImageUrl(image.getImageUrl());
        vo.setBestMatchId(result.getBestMatchId());
        vo.setRecognitionId(result.getRecognitionId());
        vo.setFinalSpeciesId(result.getFinalSpeciesId());
        vo.setFinalSpeciesName(result.getFinalSpeciesName());
        vo.setFinalConfidence(result.getFinalConfidence());
        vo.setResultSource(result.getResultSource());
        vo.setMatchResult(result.getMatchResult());
        vo.setNeedReview(result.getNeedReview() != null && result.getNeedReview() == 1);
        vo.setReviewStatus(result.getReviewStatus());
        vo.setSuggestion(result.getSuggestion());
        vo.setReviewComment(result.getReviewComment());
        vo.setReviewerId(result.getReviewerId());
        vo.setReviewerName(result.getReviewerName());
        vo.setReviewTime(result.getReviewTime());
        vo.setLocalCandidates(candidates);
        vo.setDoubaoRecognition(doubaoRecognition);
        vo.setIdentifyTime(result.getIdentifyTime());
        return vo;
    }

    private HerbIdentificationVO toVO(HerbIdentificationPageVO result, HerbImageEntity image) {
        HerbIdentificationVO vo = new HerbIdentificationVO();
        vo.setId(result.getId());
        vo.setImageId(result.getImageId());
        vo.setImageCode(result.getImageCode());
        vo.setImageUrl(result.getImageUrl());
        vo.setFinalSpeciesId(result.getFinalSpeciesId());
        vo.setFinalSpeciesName(result.getFinalSpeciesName());
        vo.setFinalConfidence(result.getFinalConfidence());
        vo.setResultSource(result.getResultSource());
        vo.setMatchResult(result.getMatchResult());
        vo.setNeedReview(result.getNeedReview());
        vo.setReviewStatus(result.getReviewStatus());
        vo.setSuggestion(result.getSuggestion());
        vo.setReviewComment(result.getReviewComment());
        vo.setReviewerId(result.getReviewerId());
        vo.setReviewerName(result.getReviewerName());
        vo.setReviewTime(result.getReviewTime());
        vo.setLocalCandidates(herbImageMatchService.latest(image.getId()).getCandidates());
        vo.setDoubaoRecognition(resolveDoubaoRecognition(result, image));
        vo.setIdentifyTime(result.getIdentifyTime());
        return vo;
    }

    private HerbRecognitionVO resolveDoubaoRecognition(
            HerbIdentificationPageVO result, HerbImageEntity image) {
        HerbRecognitionVO recognition = herbRecognitionService.getById(result.getRecognitionId());
        if (recognition != null) {
            return recognition;
        }
        return herbRecognitionService.latest(image.getId());
    }

    private HerbIdentificationVO emptyResult(HerbImageEntity image) {
        HerbIdentificationVO vo = new HerbIdentificationVO();
        vo.setImageId(image.getId());
        vo.setImageCode(image.getImageNo());
        vo.setImageUrl(image.getImageUrl());
        vo.setNeedReview(true);
        vo.setSuggestion("No identification result found");
        return vo;
    }

    private void normalizePageRequest(HerbIdentificationQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private void updateImageProcessStatus(Long imageId, String processStatus) {
        HerbImageEntity update = new HerbImageEntity();
        update.setId(imageId);
        update.setProcessStatus(processStatus);
        update.setUpdatedAt(LocalDateTime.now());
        herbImageMapper.updateProcessStatus(update);
    }

    private void applyReviewStatus(
            HerbIdentificationResultEntity result, HerbIdentificationReviewRequest request) {
        if (HerbReviewStatusConstants.REJECTED.equals(request.getReviewStatus())) {
            result.setResultSource(HerbResultSourceConstants.MANUAL_REVIEW);
            result.setNeedReview(1);
            result.setReviewStatus(HerbReviewStatusConstants.REJECTED);
            result.setSuggestion("Manual review rejected, re-processing is required");
            return;
        }
        result.setResultSource(HerbResultSourceConstants.MANUAL_REVIEW);
        result.setNeedReview(0);
        result.setReviewStatus(HerbReviewStatusConstants.CONFIRMED);
        result.setSuggestion("Manual review confirmed");
    }

    private String doubaoSuggestion(
            HerbImageMatchVO localMatch, HerbRecognitionVO doubaoRecognition) {
        if (doubaoAgreedWithLocalCandidate(localMatch, doubaoRecognition)) {
            markDoubaoAgreedCandidate(localMatch, doubaoRecognition);
            return "Doubao auxiliary recognition agrees with a local atlas candidate; manual review should focus on that herb";
        }
        return "Doubao auxiliary recognition differs from local atlas candidates; manual review is required";
    }

    private String lowConfidenceSuggestion(String doubaoError) {
        if (StringUtils.hasText(doubaoError)) {
            return "Local atlas cannot make a reliable judgment; Doubao review failed: "
                    + doubaoError;
        }
        return "Local atlas cannot make a reliable judgment, manual review is required";
    }

    private boolean doubaoAgreedWithLocalCandidate(
            HerbImageMatchVO localMatch, HerbRecognitionVO doubaoRecognition) {
        if (doubaoRecognition == null
                || !StringUtils.hasText(doubaoRecognition.getPredictedName())) {
            return false;
        }
        if (localMatch == null || CollectionUtils.isEmpty(localMatch.getCandidates())) {
            return false;
        }
        return localMatch.getCandidates().stream()
                .anyMatch(
                        candidate ->
                                doubaoRecognition
                                        .getPredictedName()
                                        .equals(candidate.getSpeciesName()));
    }

    private void markDoubaoAgreedCandidate(
            HerbImageMatchVO localMatch, HerbRecognitionVO doubaoRecognition) {
        if (localMatch == null || CollectionUtils.isEmpty(localMatch.getCandidates())) {
            return;
        }
        localMatch.getCandidates().forEach(candidate -> candidate.setDoubaoAgreed(false));
        localMatch.getCandidates().stream()
                .filter(
                        candidate ->
                                doubaoRecognition
                                        .getPredictedName()
                                        .equals(candidate.getSpeciesName()))
                .forEach(candidate -> candidate.setDoubaoAgreed(true));
    }

    private record DoubaoReviewResult(HerbRecognitionVO recognition, String errorMessage) {}
}
