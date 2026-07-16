package com.bdis.modules.collection.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.dto.HerbBatchCancelRequest;
import com.bdis.modules.collection.dto.HerbBatchConfirmStatusRequest;
import com.bdis.modules.collection.dto.HerbBatchReopenReviewRequest;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.service.HerbBatchStatusService;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.util.HerbBatchStatusFlowUtils;
import com.bdis.modules.collection.vo.HerbBatchStatusVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbBatchStatusServiceImpl implements HerbBatchStatusService {

    private final HerbBatchMapper herbBatchMapper;
    private final CollectionAccessService collectionAccessService;

    public HerbBatchStatusServiceImpl(
            HerbBatchMapper herbBatchMapper, CollectionAccessService collectionAccessService) {
        this.herbBatchMapper = herbBatchMapper;
        this.collectionAccessService = collectionAccessService;
    }

    @Override
    @Transactional
    public HerbBatchVO startCollection(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchOwner(batch);
        updateStatus(batch, HerbBatchStatusConstants.COLLECTING, null, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO submit(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchOwner(batch);
        requireTransition(batch, HerbBatchStatusConstants.SUBMITTED);
        Long count = herbBatchMapper.countBoundImagesByBatchId(batch.getId());
        if (count == null || count == 0) {
            throw new BusinessException("该批次尚未绑定采集图片，不能提交");
        }
        updateStatus(batch, HerbBatchStatusConstants.SUBMITTED, null, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO startIdentification(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchOwner(batch);
        updateStatus(batch, HerbBatchStatusConstants.IDENTIFYING, null, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO markReviewing(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchReview(batch);
        updateStatus(batch, HerbBatchStatusConstants.REVIEWING, null, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO confirmStatus(Long batchId, HerbBatchConfirmStatusRequest request) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchReview(batch);
        HerbBatchConfirmStatusRequest safeRequest =
                request == null ? new HerbBatchConfirmStatusRequest() : request;
        requireTransition(batch, HerbBatchStatusConstants.CONFIRMED);
        if (!Boolean.TRUE.equals(safeRequest.getForce()) && !isReadyToConfirm(batch)) {
            throw new BusinessException("该批次仍存在未识别或待复核图片，不能确认");
        }
        String remark = safeRequest.getRemark();
        if (Boolean.TRUE.equals(safeRequest.getForce())) {
            remark = appendText("人工强制确认", remark);
        }
        updateStatus(batch, HerbBatchStatusConstants.CONFIRMED, remark, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO archive(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchReview(batch);
        requireTransition(batch, HerbBatchStatusConstants.ARCHIVED);
        if (!StringUtils.hasText(batch.getFinalSpeciesName())) {
            throw new BusinessException("Batch final species name is required before archive");
        }
        if (!StringUtils.hasText(batch.getQualityLevel())) {
            throw new BusinessException("Batch quality level is required before archive");
        }
        if (!StringUtils.hasText(batch.getEvaluationSummary())) {
            throw new BusinessException("Batch evaluation summary is required before archive");
        }
        updateStatus(batch, HerbBatchStatusConstants.ARCHIVED, null, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO cancel(Long batchId, HerbBatchCancelRequest request) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchOwner(batch);
        if (HerbBatchStatusConstants.ARCHIVED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Archived batch cannot be cancelled");
        }
        if (HerbBatchStatusConstants.CANCELLED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Cancelled batch cannot be cancelled again");
        }
        String remark =
                request == null ? null : appendText(request.getReason(), request.getRemark());
        updateStatus(batch, HerbBatchStatusConstants.CANCELLED, remark, null);
        return latest(batchId);
    }

    @Override
    @Transactional
    public HerbBatchVO reopenReview(Long batchId, HerbBatchReopenReviewRequest request) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchReview(batch);
        if (!HerbBatchStatusConstants.CONFIRMED.equals(batch.getBatchStatus())) {
            throw new BusinessException("Only confirmed batch can be reopened for review");
        }
        String remark =
                request == null ? null : appendText(request.getReason(), request.getRemark());
        updateStatus(batch, HerbBatchStatusConstants.REVIEWING, remark, null);
        return latest(batchId);
    }

    @Override
    public HerbBatchStatusVO status(Long batchId) {
        HerbBatchEntity batch = getActiveBatch(batchId);
        collectionAccessService.requireBatchAccess(batch);
        HerbBatchStatusVO vo = new HerbBatchStatusVO();
        vo.setBatchId(batch.getId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setBatchName(batch.getBatchName());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setImageCount(valueOrZero(batch.getImageCount()));
        vo.setIdentifiedCount(valueOrZero(batch.getIdentifiedCount()));
        vo.setReviewedCount(valueOrZero(batch.getReviewedCount()));
        vo.setNeedReviewCount(valueOrZero(batch.getNeedReviewCount()));
        vo.setFinalSpeciesName(batch.getFinalSpeciesName());
        vo.setQualityLevel(batch.getQualityLevel());
        vo.setAllowedActions(HerbBatchStatusFlowUtils.getAllowedActions(batch));
        return vo;
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

    private void updateStatus(
            HerbBatchEntity batch, String targetStatus, String remark, String evaluationSummary) {
        requireTransition(batch, targetStatus);
        batch.setBatchStatus(targetStatus);
        batch.setRemark(appendText(batch.getRemark(), remark));
        if (StringUtils.hasText(evaluationSummary)) {
            batch.setEvaluationSummary(appendText(batch.getEvaluationSummary(), evaluationSummary));
        }
        batch.setUpdatedAt(LocalDateTime.now());
        batch.setUpdatedBy(collectionAccessService.currentUserId());
        int affected = herbBatchMapper.updateStatusById(batch);
        if (affected == 0) {
            throw new BusinessException("Failed to update batch status");
        }
    }

    private void requireTransition(HerbBatchEntity batch, String targetStatus) {
        if (!HerbBatchStatusFlowUtils.canTransition(batch.getBatchStatus(), targetStatus)) {
            throw new BusinessException(
                    "Invalid batch status transition: "
                            + batch.getBatchStatus()
                            + " -> "
                            + targetStatus);
        }
    }

    private boolean isReadyToConfirm(HerbBatchEntity batch) {
        int imageCount = valueOrZero(batch.getImageCount());
        return imageCount > 0
                && valueOrZero(batch.getIdentifiedCount()) == imageCount
                && valueOrZero(batch.getNeedReviewCount()) == 0;
    }

    private HerbBatchVO latest(Long batchId) {
        HerbBatchVO detail = herbBatchMapper.selectDetailById(batchId);
        if (detail == null) {
            throw new BusinessException("Batch not found");
        }
        return detail;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
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
