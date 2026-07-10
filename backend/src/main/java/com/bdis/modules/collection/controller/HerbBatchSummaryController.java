package com.bdis.modules.collection.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.collection.dto.HerbBatchConfirmRequest;
import com.bdis.modules.collection.service.HerbBatchSummaryService;
import com.bdis.modules.collection.vo.HerbBatchIdentificationItemVO;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/batch")
public class HerbBatchSummaryController {

    private final HerbBatchSummaryService herbBatchSummaryService;

    public HerbBatchSummaryController(HerbBatchSummaryService herbBatchSummaryService) {
        this.herbBatchSummaryService = herbBatchSummaryService;
    }

    @PostMapping("/{batchId}/summary/refresh")
    public Result<HerbBatchSummaryVO> refreshSummary(@PathVariable Long batchId) {
        return Result.success(herbBatchSummaryService.refreshSummary(batchId));
    }

    @GetMapping("/{batchId}/summary")
    public Result<HerbBatchSummaryVO> getSummary(@PathVariable Long batchId) {
        return Result.success(herbBatchSummaryService.getSummary(batchId));
    }

    @GetMapping("/{batchId}/identification-items")
    public Result<List<HerbBatchIdentificationItemVO>> listIdentificationItems(
            @PathVariable Long batchId) {
        return Result.success(herbBatchSummaryService.listIdentificationItems(batchId));
    }

    @PutMapping("/{batchId}/confirm")
    public Result<HerbBatchSummaryVO> confirm(
            @PathVariable Long batchId, @Valid @RequestBody HerbBatchConfirmRequest request) {
        return Result.success(herbBatchSummaryService.confirm(batchId, request));
    }
}
