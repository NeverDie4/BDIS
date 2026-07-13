package com.bdis.modules.collection.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.collection.dto.HerbBatchImageBatchBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageBindRequest;
import com.bdis.modules.collection.dto.HerbBatchImageQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchImageUpdateRequest;
import com.bdis.modules.collection.service.HerbBatchImageService;
import com.bdis.modules.collection.vo.HerbBatchImageBindResultVO;
import com.bdis.modules.collection.vo.HerbBatchImageStatisticsVO;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbImageBatchVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequirePermission("growth:record:view")
public class HerbBatchImageController {

    private final HerbBatchImageService herbBatchImageService;

    public HerbBatchImageController(HerbBatchImageService herbBatchImageService) {
        this.herbBatchImageService = herbBatchImageService;
    }

    @PostMapping("/herb/batch/{batchId}/image")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchImageVO> bind(
            @PathVariable Long batchId, @Valid @RequestBody HerbBatchImageBindRequest request) {
        return Result.success(herbBatchImageService.bind(batchId, request));
    }

    @PostMapping("/herb/batch/{batchId}/images")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchImageBindResultVO> batchBind(
            @PathVariable Long batchId,
            @Valid @RequestBody HerbBatchImageBatchBindRequest request) {
        return Result.success(herbBatchImageService.batchBind(batchId, request));
    }

    @DeleteMapping("/herb/batch/{batchId}/image/{imageId}")
    @RequirePermission("growth:record:update")
    public Result<Void> unbind(@PathVariable Long batchId, @PathVariable Long imageId) {
        herbBatchImageService.unbind(batchId, imageId);
        return Result.success();
    }

    @GetMapping("/herb/batch/{batchId}/images")
    public Result<List<HerbBatchImageVO>> listByBatch(
            @PathVariable Long batchId, @ModelAttribute HerbBatchImageQueryRequest request) {
        return Result.success(herbBatchImageService.listByBatch(batchId, request));
    }

    @GetMapping("/herb/image/{imageId}/batch")
    public Result<HerbImageBatchVO> getBatchByImageId(@PathVariable Long imageId) {
        return Result.success(herbBatchImageService.getBatchByImageId(imageId));
    }

    @PutMapping("/herb/batch/{batchId}/image/{imageId}/primary")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchImageVO> setPrimary(
            @PathVariable Long batchId, @PathVariable Long imageId) {
        return Result.success(herbBatchImageService.setPrimary(batchId, imageId));
    }

    @PutMapping("/herb/batch/{batchId}/image/{imageId}")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchImageVO> update(
            @PathVariable Long batchId,
            @PathVariable Long imageId,
            @Valid @RequestBody HerbBatchImageUpdateRequest request) {
        return Result.success(herbBatchImageService.update(batchId, imageId, request));
    }

    @PostMapping("/herb/batch/{batchId}/image/statistics/refresh")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchImageStatisticsVO> refreshStatistics(@PathVariable Long batchId) {
        return Result.success(herbBatchImageService.refreshStatistics(batchId));
    }
}
