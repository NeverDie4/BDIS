package com.bdis.modules.mobile.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import com.bdis.modules.mobile.dto.MobileBatchIdentifyRequest;
import com.bdis.modules.mobile.dto.MobileBatchImageUploadRequest;
import com.bdis.modules.mobile.dto.MobileBatchSubmitRequest;
import com.bdis.modules.mobile.service.MobileHerbBatchService;
import com.bdis.modules.mobile.vo.MobileBatchDetailVO;
import com.bdis.modules.mobile.vo.MobileBatchImageUploadResultVO;
import com.bdis.modules.mobile.vo.MobileIdentifyMissingResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/mobile/herb/batches")
public class MobileHerbBatchController {

    private final MobileHerbBatchService mobileHerbBatchService;

    public MobileHerbBatchController(MobileHerbBatchService mobileHerbBatchService) {
        this.mobileHerbBatchService = mobileHerbBatchService;
    }

    @GetMapping("/{batchId}")
    public Result<MobileBatchDetailVO> detail(
            @PathVariable Long batchId, @RequestParam(required = false) Long collectorId) {
        return Result.success(mobileHerbBatchService.detail(batchId, collectorId));
    }

    @PostMapping("/{batchId}/images/upload")
    public Result<MobileBatchImageUploadResultVO> uploadImage(
            @PathVariable Long batchId,
            @RequestParam MultipartFile file,
            @ModelAttribute MobileBatchImageUploadRequest request) {
        return Result.success(mobileHerbBatchService.uploadImage(batchId, file, request));
    }

    @PostMapping("/{batchId}/images/{imageId}/identify")
    public Result<MobileBatchImageUploadResultVO> identifyImage(
            @PathVariable Long batchId,
            @PathVariable Long imageId,
            @RequestBody(required = false) MobileBatchIdentifyRequest request) {
        return Result.success(mobileHerbBatchService.identifyImage(batchId, imageId, request));
    }

    @PostMapping("/{batchId}/identify-missing-images")
    public Result<MobileIdentifyMissingResultVO> identifyMissingImages(@PathVariable Long batchId) {
        return Result.success(mobileHerbBatchService.identifyMissingImages(batchId));
    }

    @PostMapping("/{batchId}/summary/refresh")
    public Result<HerbBatchSummaryVO> refreshSummary(@PathVariable Long batchId) {
        return Result.success(mobileHerbBatchService.refreshSummary(batchId));
    }

    @PutMapping("/{batchId}/submit")
    public Result<MobileBatchDetailVO> submit(
            @PathVariable Long batchId,
            @RequestBody(required = false) MobileBatchSubmitRequest request) {
        return Result.success(mobileHerbBatchService.submit(batchId, request));
    }
}
