package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.client.HerbRecognitionClient;
import com.bdis.modules.spectrum.client.HerbRecognitionClientResponse;
import com.bdis.modules.spectrum.dto.HerbRecognitionQueryRequest;
import com.bdis.modules.spectrum.entity.ImageRecognitionEntity;
import com.bdis.modules.spectrum.entity.SpectrumComparisonEntity;
import com.bdis.modules.spectrum.mapper.ImageRecognitionMapper;
import com.bdis.modules.spectrum.mapper.SpectrumComparisonMapper;
import com.bdis.modules.spectrum.service.HerbAtlasMatchResult;
import com.bdis.modules.spectrum.service.HerbAtlasMatchService;
import com.bdis.modules.spectrum.service.HerbRecognitionService;
import com.bdis.modules.spectrum.vo.HerbRecognitionCandidateVO;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HerbRecognitionServiceImpl implements HerbRecognitionService {

    private static final String STATUS_RECOGNIZING = "recognizing";
    private static final String STATUS_RECOGNIZED = "recognized";
    private static final String STATUS_FAILED = "failed";
    private static final String RECOGNITION_SUCCESS = "success";
    private static final String RECOGNITION_FAILED = "failed";
    private static final String RECOGNITION_REVIEW_REQUIRED = "review_required";
    private static final String SOURCE_LOCAL_ATLAS = "local_atlas";
    private static final String SOURCE_DOUBAO_AUXILIARY = "doubao_auxiliary";
    private static final BigDecimal HIGH_CONFIDENCE_THRESHOLD = new BigDecimal("0.85");
    private static final BigDecimal MEDIUM_CONFIDENCE_THRESHOLD = new BigDecimal("0.60");
    private static final int TOP_N = 5;

    private final HerbImageMapper herbImageMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final ImageRecognitionMapper imageRecognitionMapper;
    private final HerbRecognitionClient herbRecognitionClient;
    private final HerbAtlasMatchService herbAtlasMatchService;
    private final SpectrumComparisonMapper spectrumComparisonMapper;

    public HerbRecognitionServiceImpl(
            HerbImageMapper herbImageMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            ImageRecognitionMapper imageRecognitionMapper,
            HerbRecognitionClient herbRecognitionClient,
            HerbAtlasMatchService herbAtlasMatchService,
            SpectrumComparisonMapper spectrumComparisonMapper) {
        this.herbImageMapper = herbImageMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.imageRecognitionMapper = imageRecognitionMapper;
        this.herbRecognitionClient = herbRecognitionClient;
        this.herbAtlasMatchService = herbAtlasMatchService;
        this.spectrumComparisonMapper = spectrumComparisonMapper;
    }

    @Override
    public HerbRecognitionVO recognize(Long imageId) {
        HerbImageEntity image = getActiveImage(imageId);
        if (STATUS_RECOGNIZING.equals(image.getProcessStatus())) {
            throw new BusinessException("Herb image is recognizing");
        }
        updateImageStatus(image, STATUS_RECOGNIZING);
        try {
            List<HerbAtlasMatchResult> matches = herbAtlasMatchService.matchTopN(image, TOP_N);
            saveMatches(image, matches);
            HerbAtlasMatchResult bestMatch = matches.isEmpty() ? null : matches.get(0);
            if (isHighConfidence(bestMatch)) {
                return saveLocalRecognition(image, bestMatch, matches, false);
            }
            if (isMediumConfidence(bestMatch)) {
                return saveLocalRecognition(image, bestMatch, matches, true);
            }
            HerbRecognitionClientResponse response = herbRecognitionClient.recognize(image);
            return saveAuxiliaryRecognition(image, response, matches);
        } catch (RuntimeException exception) {
            updateImageStatus(image, STATUS_FAILED);
            throw exception;
        }
    }

    @Override
    public HerbRecognitionVO recognizeByDoubao(Long imageId) {
        HerbImageEntity image = getActiveImage(imageId);
        updateImageStatus(image, STATUS_RECOGNIZING);
        try {
            HerbRecognitionClientResponse response = herbRecognitionClient.recognize(image);
            return saveAuxiliaryRecognition(image, response, List.of());
        } catch (RuntimeException exception) {
            updateImageStatus(image, STATUS_FAILED);
            throw exception;
        }
    }

    @Override
    public HerbRecognitionVO latest(Long imageId) {
        getActiveImage(imageId);
        return imageRecognitionMapper.selectLatestByImageId(imageId);
    }

    @Override
    public HerbRecognitionVO getById(Long id) {
        if (id == null) {
            return null;
        }
        return imageRecognitionMapper.selectVoById(id);
    }

    @Override
    public List<HerbRecognitionVO> history(Long imageId) {
        getActiveImage(imageId);
        return imageRecognitionMapper.selectByImageId(imageId);
    }

    @Override
    public PageResult<HerbRecognitionVO> page(HerbRecognitionQueryRequest request) {
        HerbRecognitionQueryRequest safeRequest =
                request == null ? new HerbRecognitionQueryRequest() : request;
        normalizePageRequest(safeRequest);
        Long total = imageRecognitionMapper.countPage(safeRequest);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbRecognitionVO> records =
                imageRecognitionMapper.selectPage(safeRequest, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    private HerbImageEntity getActiveImage(Long imageId) {
        if (imageId == null) {
            throw new BusinessException("Herb image id is required");
        }
        HerbImageEntity image = herbImageMapper.selectActiveById(imageId);
        if (image == null) {
            throw new BusinessException("Herb image not found");
        }
        return image;
    }

    private void updateImageStatus(HerbImageEntity image, String processStatus) {
        HerbImageEntity statusUpdate = new HerbImageEntity();
        statusUpdate.setId(image.getId());
        statusUpdate.setProcessStatus(processStatus);
        statusUpdate.setUpdatedAt(LocalDateTime.now());
        herbImageMapper.updateProcessStatus(statusUpdate);
        image.setProcessStatus(processStatus);
        image.setUpdatedAt(statusUpdate.getUpdatedAt());
    }

    private ImageRecognitionEntity buildRecognition(
            HerbImageEntity image, HerbRecognitionClientResponse response) {
        LocalDateTime now = LocalDateTime.now();
        ImageRecognitionEntity recognition = new ImageRecognitionEntity();
        recognition.setImageId(image.getId());
        recognition.setRankNo(1);
        recognition.setRecognizedHerbName(
                response.getPredictedName() == null ? "UNKNOWN" : response.getPredictedName());
        recognition.setConfidence(response.getConfidence());
        recognition.setIsUncertain(isSuccess(response) ? 0 : 1);
        recognition.setRawResponse(response.getRawResult());
        recognition.setCreatedAt(now);
        return recognition;
    }

    private HerbRecognitionVO saveLocalRecognition(
            HerbImageEntity image,
            HerbAtlasMatchResult bestMatch,
            List<HerbAtlasMatchResult> matches,
            boolean needReview) {
        ImageRecognitionEntity recognition = buildLocalRecognition(image, bestMatch, needReview);
        HerbEntity species = matchSpecies(bestMatch.getSpeciesName());
        if (species != null) {
            recognition.setSpeciesId(species.getId());
        } else {
            recognition.setSpeciesId(bestMatch.getSpeciesId());
        }
        imageRecognitionMapper.insertRecognition(recognition);
        updateImageStatus(image, STATUS_RECOGNIZED);
        return toVO(image, recognition, species, needReview, SOURCE_LOCAL_ATLAS, matches);
    }

    private HerbRecognitionVO saveAuxiliaryRecognition(
            HerbImageEntity image,
            HerbRecognitionClientResponse response,
            List<HerbAtlasMatchResult> matches) {
        ImageRecognitionEntity recognition = buildRecognition(image, response);
        HerbEntity species = matchSpecies(response.getPredictedName());
        if (species != null) {
            recognition.setSpeciesId(species.getId());
        }
        imageRecognitionMapper.insertRecognition(recognition);
        updateImageStatus(image, isSuccess(response) ? STATUS_RECOGNIZED : STATUS_FAILED);
        return toVO(image, recognition, species, true, SOURCE_DOUBAO_AUXILIARY, matches);
    }

    private ImageRecognitionEntity buildLocalRecognition(
            HerbImageEntity image, HerbAtlasMatchResult bestMatch, boolean needReview) {
        LocalDateTime now = LocalDateTime.now();
        ImageRecognitionEntity recognition = new ImageRecognitionEntity();
        recognition.setImageId(image.getId());
        recognition.setRankNo(1);
        recognition.setSpeciesId(bestMatch.getSpeciesId());
        recognition.setRecognizedHerbName(bestMatch.getSpeciesName());
        recognition.setConfidence(bestMatch.getSimilarity());
        recognition.setIsUncertain(needReview ? 1 : 0);
        recognition.setRawResponse(
                "{\"source\":\"" + SOURCE_LOCAL_ATLAS + "\",\"needReview\":" + needReview + "}");
        recognition.setCreatedAt(now);
        return recognition;
    }

    private void saveMatches(HerbImageEntity image, List<HerbAtlasMatchResult> matches) {
        for (HerbAtlasMatchResult match : matches) {
            SpectrumComparisonEntity comparison = new SpectrumComparisonEntity();
            comparison.setImageId(image.getId());
            comparison.setAtlasId(match.getAtlasId());
            comparison.setImageSimilarity(match.getSimilarity());
            comparison.setFinalScore(match.getSimilarity());
            comparison.setMatchRank(match.getRank());
            comparison.setMatchLevel(matchLevel(match));
            comparison.setCreatedAt(LocalDateTime.now());
            comparison.setUpdatedAt(comparison.getCreatedAt());
            spectrumComparisonMapper.insert(comparison);
        }
    }

    private String matchLevel(HerbAtlasMatchResult match) {
        if (isHighConfidence(match)) {
            return "high";
        }
        if (isMediumConfidence(match)) {
            return "medium";
        }
        return "low";
    }

    private HerbEntity matchSpecies(String predictedName) {
        if (predictedName == null || predictedName.isBlank()) {
            return null;
        }
        return herbSpeciesMapper.selectByNameOrAlias(predictedName);
    }

    private boolean isSuccess(HerbRecognitionClientResponse response) {
        return Boolean.TRUE.equals(response.getSuccess());
    }

    private boolean isHighConfidence(HerbAtlasMatchResult match) {
        return match != null
                && match.getSimilarity() != null
                && match.getSimilarity().compareTo(HIGH_CONFIDENCE_THRESHOLD) >= 0;
    }

    private boolean isMediumConfidence(HerbAtlasMatchResult match) {
        return match != null
                && match.getSimilarity() != null
                && match.getSimilarity().compareTo(MEDIUM_CONFIDENCE_THRESHOLD) >= 0;
    }

    private HerbRecognitionVO toVO(
            HerbImageEntity image, ImageRecognitionEntity recognition, HerbEntity species) {
        return toVO(image, recognition, species, false, null, List.of());
    }

    private HerbRecognitionVO toVO(
            HerbImageEntity image,
            ImageRecognitionEntity recognition,
            HerbEntity species,
            boolean needReview,
            String recognitionSource,
            List<HerbAtlasMatchResult> matches) {
        HerbRecognitionVO vo = new HerbRecognitionVO();
        vo.setId(recognition.getId());
        vo.setImageId(recognition.getImageId());
        vo.setImageCode(image.getImageNo());
        vo.setImageUrl(image.getImageUrl());
        vo.setModelVersionId(recognition.getModelId());
        vo.setPredictedSpeciesId(recognition.getSpeciesId());
        vo.setPredictedSpeciesName(species == null ? null : species.getHerbName());
        vo.setPredictedName(recognition.getRecognizedHerbName());
        vo.setConfidence(recognition.getConfidence());
        vo.setRecognitionStatus(
                Integer.valueOf(1).equals(recognition.getIsUncertain())
                        ? RECOGNITION_REVIEW_REQUIRED
                        : RECOGNITION_SUCCESS);
        vo.setNeedReview(needReview);
        vo.setRecognitionSource(recognitionSource);
        vo.setCandidates(toCandidates(matches));
        vo.setRecognitionTime(recognition.getCreatedAt());
        vo.setRawResult(recognition.getRawResponse());
        return vo;
    }

    private List<HerbRecognitionCandidateVO> toCandidates(List<HerbAtlasMatchResult> matches) {
        return matches.stream().map(this::toCandidate).toList();
    }

    private HerbRecognitionCandidateVO toCandidate(HerbAtlasMatchResult match) {
        HerbRecognitionCandidateVO candidate = new HerbRecognitionCandidateVO();
        candidate.setAtlasId(match.getAtlasId());
        candidate.setSpeciesId(match.getSpeciesId());
        candidate.setName(match.getSpeciesName());
        candidate.setConfidence(match.getSimilarity());
        candidate.setSimilarity(match.getSimilarity());
        candidate.setRank(match.getRank());
        return candidate;
    }

    private void normalizePageRequest(HerbRecognitionQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }
}
