package com.bdis.modules.collection.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.collection.dto.HerbBatchCreateRequest;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchUpdateRequest;
import com.bdis.modules.collection.service.HerbBatchService;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthRecordVO;
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
@RequestMapping("/herb/batch")
@RequirePermission("growth:record:view")
public class HerbBatchController {

    private final HerbBatchService herbBatchService;
    private final GrowthRecordService growthRecordService;

    public HerbBatchController(
            HerbBatchService herbBatchService, GrowthRecordService growthRecordService) {
        this.herbBatchService = herbBatchService;
        this.growthRecordService = growthRecordService;
    }

    @PostMapping
    @RequirePermission("growth:record:create")
    public Result<HerbBatchVO> create(@Valid @RequestBody HerbBatchCreateRequest request) {
        return Result.success(herbBatchService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("growth:record:update")
    public Result<HerbBatchVO> update(
            @PathVariable Long id, @Valid @RequestBody HerbBatchUpdateRequest request) {
        return Result.success(herbBatchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("growth:record:delete")
    public Result<Void> delete(@PathVariable Long id) {
        herbBatchService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<HerbBatchVO> getById(@PathVariable Long id) {
        return Result.success(herbBatchService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<HerbBatchVO>> page(@ModelAttribute HerbBatchQueryRequest request) {
        return Result.success(herbBatchService.page(request));
    }

    @GetMapping("/list")
    public Result<List<HerbBatchListVO>> list(@ModelAttribute HerbBatchQueryRequest request) {
        return Result.success(herbBatchService.list(request));
    }

    @GetMapping("/{batchId}/growth-record")
    public Result<GrowthRecordVO> getGrowthRecord(@PathVariable Long batchId) {
        return Result.success(growthRecordService.getByBatchId(batchId));
    }

    @PostMapping("/{batchId}/growth-record")
    @RequirePermission("growth:record:create")
    public Result<GrowthRecordVO> createGrowthRecord(
            @PathVariable Long batchId, @Valid @RequestBody GrowthRecordUpsertRequest request) {
        return Result.success(growthRecordService.createForBatch(batchId, request));
    }

    @PutMapping("/{batchId}/growth-record/{recordId}")
    @RequirePermission("growth:record:update")
    public Result<GrowthRecordVO> updateGrowthRecord(
            @PathVariable Long batchId,
            @PathVariable Long recordId,
            @Valid @RequestBody GrowthRecordUpsertRequest request) {
        return Result.success(growthRecordService.updateForBatch(batchId, recordId, request));
    }
}
