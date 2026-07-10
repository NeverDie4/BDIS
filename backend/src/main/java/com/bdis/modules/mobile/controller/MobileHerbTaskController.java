package com.bdis.modules.mobile.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.mobile.dto.MobileBatchCreateRequest;
import com.bdis.modules.mobile.dto.MobileTaskQueryRequest;
import com.bdis.modules.mobile.service.MobileHerbTaskService;
import com.bdis.modules.mobile.vo.MobileBatchVO;
import com.bdis.modules.mobile.vo.MobileTaskDetailVO;
import com.bdis.modules.mobile.vo.MobileTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mobile/herb/tasks")
public class MobileHerbTaskController {

    private final MobileHerbTaskService mobileHerbTaskService;

    public MobileHerbTaskController(MobileHerbTaskService mobileHerbTaskService) {
        this.mobileHerbTaskService = mobileHerbTaskService;
    }

    @GetMapping
    public Result<PageResult<MobileTaskVO>> myTasks(
            @ModelAttribute MobileTaskQueryRequest request) {
        return Result.success(mobileHerbTaskService.myTasks(request));
    }

    @GetMapping("/{taskId}")
    public Result<MobileTaskDetailVO> detail(
            @PathVariable Long taskId, @RequestParam(required = false) Long collectorId) {
        return Result.success(mobileHerbTaskService.detail(taskId, collectorId));
    }

    @GetMapping("/{taskId}/batches")
    public Result<PageResult<MobileBatchVO>> batches(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long collectorId,
            @RequestParam(required = false) String batchStatus,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(
                mobileHerbTaskService.batches(
                        taskId, collectorId, batchStatus, pageNum, pageSize));
    }

    @PostMapping("/{taskId}/batches")
    public Result<MobileBatchVO> createBatch(
            @PathVariable Long taskId, @RequestBody MobileBatchCreateRequest request) {
        return Result.success(mobileHerbTaskService.createBatch(taskId, request));
    }
}
