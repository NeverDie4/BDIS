package com.bdis.modules.mobile.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchImageRoleConstants;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchImageBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbBatchImageEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.service.HerbBatchStatusService;
import com.bdis.modules.collection.service.HerbBatchSummaryService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.herb.dto.HerbImageUploadRequest;
import com.bdis.modules.herb.service.HerbImageService;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.mobile.dto.MobileBatchIdentifyRequest;
import com.bdis.modules.mobile.dto.MobileBatchImageUploadRequest;
import com.bdis.modules.mobile.dto.MobileBatchSubmitRequest;
import com.bdis.modules.mobile.service.MobileHerbBatchService;
import com.bdis.modules.mobile.vo.MobileBatchDetailVO;
import com.bdis.modules.mobile.vo.MobileBatchImageUploadResultVO;
import com.bdis.modules.mobile.vo.MobileBatchImageVO;
import com.bdis.modules.mobile.vo.MobileIdentifyMissingItemVO;
import com.bdis.modules.mobile.vo.MobileIdentifyMissingResultVO;
import com.bdis.modules.mobile.vo.MobileImageIdentificationVO;
import com.bdis.modules.spectrum.dto.HerbIdentifyRequest;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MobileHerbBatchServiceImpl implements MobileHerbBatchService {

    private final HerbBatchMapper herbBatchMapper;
    private final HerbBatchImageMapper herbBatchImageMapper;
    private final HerbCollectionTaskMapper herbCollectionTaskMapper;
    private final HerbImageService herbImageService;
    private final HerbBatchImageService herbBatchImageService;
    private final HerbIdentificationService herbIdentificationService;
    private final HerbBatchSummaryService herbBatchSummaryService;
    private final HerbBatchStatusService herbBatchStatusService;

    public MobileHerbBatchServiceImpl(
            HerbBatchMapper herbBatchMapper,
            HerbBatchImageMapper herbBatchImageMapper,
            HerbCollectionTaskMapper herbCollectionTaskMapper,
            HerbImageService herbImageService,
            HerbBatchImageService herbBatchImageService,
            HerbIdentificationService herbIdentificationService,
            HerbBatchSummaryService herbBatchSummaryService,
            HerbBatchStatusService herbBatchStatusService) {
        this.herbBatchMapper = herbBatchMapper;
        this.herbBatchImageMapper = herbBatchImageMapper;
        this.herbCollectionTaskMapper = herbCollectionTaskMapper;
        this.herbImageService = herbImageService;
        this.herbBatchImageService = herbBatchImageService;
        this.herbIdentificationService = herbIdentificationService;
        this.herbBatchSummaryService = herbBatchSummaryService;
        this.herbBatchStatusService = herbBatchStatusService;
    }

    @Override
    public MobileBatchDetailVO detail(Long batchId, Long collectorId) {
        HerbBatchVO batch = getBatchDetail(batchId);
        validateBatchCollector(batch.getTaskId(), collectorId);
        MobileBatchDetailVO vo = toDetailVO(batch);
        HerbBatchImageQueryRequest query = new HerbBatchImageQueryRequest();
        List<HerbBatchImageVO> images = herbBatchImageService.listByBatch(batchId, query);
        vo.setImages(images.stream().map(this::toMobileImageVO).toList());
        return vo;
    }

    @Override
    @Transactional
    public MobileBatchImageUploadResultVO uploadImage(
            Long batchId, MultipartFile file, MobileBatchImageUploadRequest request) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        MobileBatchImageUploadRequest safeRequest =
                request == null ? new MobileBatchImageUploadRequest() : request;
        validateBatchCollector(batch.getTaskId(), safeRequest.getCollectorId());
        ensureUploadAllowed(batch);
        if (HerbBatchStatusConstants.DRAFT.equals(batch.getBatchStatus())) {
            herbBatchStatusService.startCollection(batchId);
            batch.setBatchStatus(HerbBatchStatusConstants.COLLECTING);
        }
        HerbImageVO image = herbImageService.upload(file, toImageUploadRequest(batch, safeRequest));
        HerbBatchImageVO binding;
        try {
            binding = herbBatchImageService.bind(batchId, toBindRequest(safeRequest, image));
        } catch (RuntimeException exception) {
            herbImageService.delete(image.getId());
            throw exception;
        }
        MobileBatchImageUploadResultVO result = toUploadResult(binding);
        result.setAutoIdentifySuccess(false);
        result.setMessage("uploaded and bound");
        if (Boolean.TRUE.equals(safeRequest.getAutoIdentify())) {
            identifyBoundImage(batchId, image.getId(), new MobileBatchIdentifyRequest(), result);
        }
        return result;
    }

    @Override
    @Transactional
    public MobileBatchImageUploadResultVO identifyImage(
            Long batchId, Long imageId, MobileBatchIdentifyRequest request) {
        getEditableBatch(batchId);
        HerbBatchImageEntity binding = getBoundBinding(batchId, imageId);
        HerbIdentificationVO identification = identify(imageId, request);
        updateBindingIdentification(binding, identification.getId());
        HerbBatchImageVO detail =
                herbBatchImageMapper.selectDetailByBatchIdAndImageId(batchId, imageId);
        MobileBatchImageUploadResultVO result = toUploadResult(detail);
        result.setAutoIdentifySuccess(true);
        result.setMessage("identified");
        return result;
    }

    @Override
    @Transactional
    public MobileIdentifyMissingResultVO identifyMissingImages(Long batchId) {
        getEditableBatch(batchId);
        HerbBatchImageQueryRequest query = new HerbBatchImageQueryRequest();
        List<HerbBatchImageVO> images =
                herbBatchImageService.listByBatch(batchId, query).stream()
                        .filter(image -> image.getIdentificationResultId() == null)
                        .toList();
        List<MobileIdentifyMissingItemVO> items = new ArrayList<>();
        int successCount = 0;
        for (HerbBatchImageVO image : images) {
            MobileIdentifyMissingItemVO item = new MobileIdentifyMissingItemVO();
            item.setImageId(image.getImageId());
            item.setBatchImageId(image.getId());
            try {
                HerbIdentificationVO identification = identify(image.getImageId(), null);
                HerbBatchImageEntity binding = getBoundBinding(batchId, image.getImageId());
                updateBindingIdentification(binding, identification.getId());
                item.setSuccess(true);
                item.setIdentificationResultId(identification.getId());
                item.setFinalSpeciesName(identification.getFinalSpeciesName());
                item.setMessage("identified");
                successCount++;
            } catch (RuntimeException exception) {
                item.setSuccess(false);
                item.setMessage(exception.getMessage());
            }
            items.add(item);
        }
        MobileIdentifyMissingResultVO result = new MobileIdentifyMissingResultVO();
        result.setTotalCount(items.size());
        result.setSuccessCount(successCount);
        result.setFailCount(items.size() - successCount);
        result.setItems(items);
        return result;
    }

    @Override
    public HerbBatchSummaryVO refreshSummary(Long batchId) {
        HerbBatchSummaryVO summary = herbBatchSummaryService.refreshSummary(batchId);
        if (summary.getNeedReviewCount() != null && summary.getNeedReviewCount() > 0) {
            summary.setEvaluationSummary(
                    appendText(summary.getEvaluationSummary(), "存在待复核图片，需要 PC 端或管理员复核。"));
        }
        return summary;
    }

    @Override
    @Transactional
    public MobileBatchDetailVO submit(Long batchId, MobileBatchSubmitRequest request) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        validateBatchCollector(
                batch.getTaskId(), request == null ? null : request.getCollectorId());
        if (HerbBatchStatusConstants.DRAFT.equals(batch.getBatchStatus())) {
            herbBatchStatusService.startCollection(batchId);
            batch.setBatchStatus(HerbBatchStatusConstants.COLLECTING);
        }
        if (!HerbBatchStatusConstants.COLLECTING.equals(batch.getBatchStatus())) {
            throw new BusinessException(
                    "Only draft or collecting batch can be submitted by mobile");
        }
        herbBatchStatusService.submit(batchId);
        HerbBatchEntity update = new HerbBatchEntity();
        update.setId(batchId);
        update.setCollectEndTime(
                batch.getCollectEndTime() == null
                        ? LocalDateTime.now()
                        : batch.getCollectEndTime());
        update.setRemark(
                appendText(batch.getRemark(), request == null ? null : request.getRemark()));
        update.setUpdatedAt(LocalDateTime.now());
        herbBatchMapper.updateMobileSubmitFields(update);
        return detail(batchId, request == null ? null : request.getCollectorId());
    }

    @Override
    public MobileImageIdentificationVO latestIdentification(Long imageId) {
        return toMobileIdentificationVO(herbIdentificationService.latest(imageId));
    }

    private void identifyBoundImage(
            Long batchId,
            Long imageId,
            MobileBatchIdentifyRequest request,
            MobileBatchImageUploadResultVO result) {
        try {
            HerbBatchImageEntity binding = getBoundBinding(batchId, imageId);
            HerbIdentificationVO identification = identify(imageId, request);
            updateBindingIdentification(binding, identification.getId());
            HerbBatchImageVO detail =
                    herbBatchImageMapper.selectDetailByBatchIdAndImageId(batchId, imageId);
            fillUploadResult(result, detail);
            result.setAutoIdentifySuccess(true);
            result.setMessage("uploaded, bound and identified");
        } catch (RuntimeException exception) {
            result.setAutoIdentifySuccess(false);
            result.setMessage(
                    "uploaded and bound, but identification failed: " + exception.getMessage());
        }
    }

    private HerbIdentificationVO identify(Long imageId, MobileBatchIdentifyRequest request) {
        HerbIdentifyRequest identifyRequest = new HerbIdentifyRequest();
        if (request != null) {
            identifyRequest.setForceRefresh(request.getForceRefresh());
            identifyRequest.setTopK(request.getTopK());
        }
        return herbIdentificationService.identify(imageId, identifyRequest);
    }

    private HerbImageUploadRequest toImageUploadRequest(
            HerbBatchEntity batch, MobileBatchImageUploadRequest request) {
        HerbImageUploadRequest uploadRequest = new HerbImageUploadRequest();
        uploadRequest.setSpeciesId(batch.getSpeciesId());
        uploadRequest.setBaseId(batch.getBaseId());
        uploadRequest.setUploadSource("mobile");
        uploadRequest.setCollectorId(request.getCollectorId());
        uploadRequest.setCollectPlace(
                StringUtils.hasText(request.getCollectPlace())
                        ? request.getCollectPlace()
                        : batch.getOriginPlace());
        uploadRequest.setCollectTime(
                request.getCollectTime() == null ? LocalDateTime.now() : request.getCollectTime());
        uploadRequest.setImageType(request.getImageType());
        uploadRequest.setGrowthStage(request.getGrowthStage());
        uploadRequest.setHealthStatus(request.getHealthStatus());
        return uploadRequest;
    }

    private HerbBatchImageBindRequest toBindRequest(
            MobileBatchImageUploadRequest request, Long identificationResultId) {
        HerbBatchImageBindRequest bindRequest = new HerbBatchImageBindRequest();
        bindRequest.setImageId(null);
        bindRequest.setIdentificationResultId(identificationResultId);
        bindRequest.setImageRole(
                StringUtils.hasText(request.getImageRole())
                        ? request.getImageRole()
                        : HerbBatchImageRoleConstants.OTHER);
        bindRequest.setIsPrimary(request.getIsPrimary() == null ? 0 : request.getIsPrimary());
        bindRequest.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        bindRequest.setRemark("mobile upload");
        return bindRequest;
    }

    private HerbBatchImageBindRequest toBindRequest(
            MobileBatchImageUploadRequest request, HerbImageVO image) {
        HerbBatchImageBindRequest bindRequest = toBindRequest(request, (Long) null);
        bindRequest.setImageId(image.getId());
        return bindRequest;
    }

    private HerbBatchVO getBatchDetail(Long batchId) {
        HerbBatchVO batch = herbBatchMapper.selectDetailById(batchId);
        if (batch == null) {
            throw new BusinessException("Batch not found");
        }
        return batch;
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

    private HerbBatchEntity getEditableBatch(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        if (HerbBatchStatusConstants.ARCHIVED.equals(batch.getBatchStatus())
                || HerbBatchStatusConstants.CANCELLED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Archived or cancelled batch cannot be operated by mobile");
        }
        return batch;
    }

    private void validateBatchCollector(Long taskId, Long collectorId) {
        if (collectorId == null || taskId == null) {
            return;
        }
        HerbCollectionTaskEntity task = herbCollectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        if (task.getCollectorId() != null && !collectorId.equals(task.getCollectorId())) {
            throw new BusinessException("Task does not belong to current collector");
        }
    }

    private void ensureUploadAllowed(HerbBatchEntity batch) {
        getEditableBatch(batch.getId());
        if (!HerbBatchStatusConstants.DRAFT.equals(batch.getBatchStatus())
                && !HerbBatchStatusConstants.COLLECTING.equals(batch.getBatchStatus())) {
            throw new BusinessException(
                    "Only draft or collecting batch can upload images by mobile");
        }
    }

    private HerbBatchImageEntity getBoundBinding(Long batchId, Long imageId) {
        HerbBatchImageEntity binding =
                herbBatchImageMapper.selectByBatchIdAndImageId(batchId, imageId);
        if (binding == null) {
            throw new BusinessException("Batch image binding not found");
        }
        return binding;
    }

    private void updateBindingIdentification(
            HerbBatchImageEntity binding, Long identificationResultId) {
        binding.setIdentificationResultId(identificationResultId);
        binding.setUpdatedAt(LocalDateTime.now());
        herbBatchImageMapper.updateIdentificationResultById(binding);
    }

    private MobileBatchDetailVO toDetailVO(HerbBatchVO batch) {
        MobileBatchDetailVO vo = new MobileBatchDetailVO();
        vo.setBatchId(batch.getId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setBatchName(batch.getBatchName());
        vo.setTaskId(batch.getTaskId());
        vo.setSpeciesId(batch.getSpeciesId());
        vo.setSpeciesName(batch.getSpeciesName());
        vo.setBaseId(batch.getBaseId());
        vo.setBaseName(batch.getBaseName());
        vo.setOriginPlace(batch.getOriginPlace());
        vo.setCollectStartTime(batch.getCollectStartTime());
        vo.setCollectEndTime(batch.getCollectEndTime());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setImageCount(batch.getImageCount());
        vo.setIdentifiedCount(batch.getIdentifiedCount());
        vo.setReviewedCount(batch.getReviewedCount());
        vo.setNeedReviewCount(batch.getNeedReviewCount());
        vo.setFinalSpeciesName(batch.getFinalSpeciesName());
        vo.setAvgSimilarity(batch.getAvgSimilarity());
        vo.setQualityLevel(batch.getQualityLevel());
        vo.setQualityScore(batch.getQualityScore());
        vo.setEvaluationSummary(batch.getEvaluationSummary());
        return vo;
    }

    private MobileBatchImageVO toMobileImageVO(HerbBatchImageVO image) {
        MobileBatchImageVO vo = new MobileBatchImageVO();
        vo.setBatchImageId(image.getId());
        vo.setImageId(image.getImageId());
        vo.setImageCode(image.getImageCode());
        vo.setImageUrl(image.getImageUrl());
        vo.setImageRole(image.getImageRole());
        vo.setIsPrimary(image.getIsPrimary());
        vo.setIdentificationResultId(image.getIdentificationResultId());
        vo.setFinalSpeciesName(image.getFinalSpeciesName());
        vo.setFinalConfidence(image.getFinalConfidence());
        vo.setNeedReview(image.getNeedReview());
        vo.setReviewStatus(image.getReviewStatus());
        vo.setResultSource(image.getResultSource());
        vo.setIdentifyTime(image.getIdentifyTime());
        return vo;
    }

    private MobileBatchImageUploadResultVO toUploadResult(HerbBatchImageVO image) {
        MobileBatchImageUploadResultVO result = new MobileBatchImageUploadResultVO();
        fillUploadResult(result, image);
        return result;
    }

    private void fillUploadResult(MobileBatchImageUploadResultVO result, HerbBatchImageVO image) {
        result.setBatchId(image.getBatchId());
        result.setBatchImageId(image.getId());
        result.setImageId(image.getImageId());
        result.setImageCode(image.getImageCode());
        result.setImageUrl(image.getImageUrl());
        result.setImageRole(image.getImageRole());
        result.setIsPrimary(image.getIsPrimary());
        result.setIdentificationResultId(image.getIdentificationResultId());
        result.setFinalSpeciesName(image.getFinalSpeciesName());
        result.setFinalConfidence(image.getFinalConfidence());
        result.setNeedReview(image.getNeedReview());
        result.setReviewStatus(image.getReviewStatus());
    }

    private MobileImageIdentificationVO toMobileIdentificationVO(
            HerbIdentificationVO identification) {
        MobileImageIdentificationVO vo = new MobileImageIdentificationVO();
        vo.setId(identification.getId());
        vo.setImageId(identification.getImageId());
        vo.setImageCode(identification.getImageCode());
        vo.setImageUrl(identification.getImageUrl());
        vo.setFinalSpeciesId(identification.getFinalSpeciesId());
        vo.setFinalSpeciesName(identification.getFinalSpeciesName());
        vo.setFinalConfidence(identification.getFinalConfidence());
        vo.setResultSource(identification.getResultSource());
        vo.setMatchResult(identification.getMatchResult());
        vo.setNeedReview(identification.getNeedReview());
        vo.setReviewStatus(identification.getReviewStatus());
        vo.setSuggestion(identification.getSuggestion());
        vo.setLocalCandidates(identification.getLocalCandidates());
        if (identification.getDoubaoRecognition() != null) {
            identification.getDoubaoRecognition().setRawResult(null);
        }
        vo.setDoubaoRecognition(identification.getDoubaoRecognition());
        vo.setIdentifyTime(identification.getIdentifyTime());
        return vo;
    }

    private String appendText(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + "；" + second;
    }
}
