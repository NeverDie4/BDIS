package com.bdis.modules.collection.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.collection.dto.HerbBatchCancelRequest;
import com.bdis.modules.collection.dto.HerbBatchConfirmStatusRequest;
import com.bdis.modules.collection.dto.HerbBatchReopenReviewRequest;
import com.bdis.modules.collection.service.HerbBatchStatusService;
import com.bdis.modules.collection.vo.HerbBatchStatusVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/batch")
@RequirePermission("growth:record:view")
public class HerbBatchStatusController {

    private final HerbBatchStatusService herbBatchStatusService;

    public HerbBatchStatusController(HerbBatchStatusService herbBatchStatusService) {
        this.herbBatchStatusService = herbBatchStatusService;
    }

    @PutMapping("/{batchId}/start-collection")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchVO> startCollection(@PathVariable Long batchId) {
        return Result.success(herbBatchStatusService.startCollection(batchId));
    }

    @PutMapping("/{batchId}/submit")
    @RequirePermission("growth:record:submit")
    public Result<HerbBatchVO> submit(@PathVariable Long batchId) {
        return Result.success(herbBatchStatusService.submit(batchId));
    }

    @PutMapping("/{batchId}/start-identification")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchVO> startIdentification(@PathVariable Long batchId) {
        return Result.success(herbBatchStatusService.startIdentification(batchId));
    }

    @PutMapping("/{batchId}/mark-reviewing")
    @RequirePermission("growth:record:audit")
    public Result<HerbBatchVO> markReviewing(@PathVariable Long batchId) {
        return Result.success(herbBatchStatusService.markReviewing(batchId));
    }

    @PutMapping("/{batchId}/confirm-status")
    @RequirePermission("growth:record:audit")
    public Result<HerbBatchVO> confirmStatus(
            @PathVariable Long batchId,
            @RequestBody(required = false) HerbBatchConfirmStatusRequest request) {
        return Result.success(herbBatchStatusService.confirmStatus(batchId, request));
    }

    @PutMapping("/{batchId}/archive")
    @RequirePermission("growth:record:audit")
    public Result<HerbBatchVO> archive(@PathVariable Long batchId) {
        return Result.success(herbBatchStatusService.archive(batchId));
    }

    @PutMapping("/{batchId}/cancel")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchVO> cancel(
            @PathVariable Long batchId,
            @RequestBody(required = false) HerbBatchCancelRequest request) {
        return Result.success(herbBatchStatusService.cancel(batchId, request));
    }

    @PutMapping("/{batchId}/reopen-review")
    @RequirePermission("growth:record:audit")
    public Result<HerbBatchVO> reopenReview(
            @PathVariable Long batchId,
            @RequestBody(required = false) HerbBatchReopenReviewRequest request) {
        return Result.success(herbBatchStatusService.reopenReview(batchId, request));
    }

    @GetMapping("/{batchId}/status")
    public Result<HerbBatchStatusVO> status(@PathVariable Long batchId) {
        return Result.success(herbBatchStatusService.status(batchId));
    }
}
