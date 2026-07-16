package com.bdis.soap.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.soap.dto.SoapRetryDTO;
import com.bdis.soap.dto.SoapSyncTaskDTO;
import com.bdis.soap.query.SoapSyncTaskQuery;
import com.bdis.soap.service.SoapSyncTaskService;
import com.bdis.soap.vo.SoapExchangeRecordVO;
import com.bdis.soap.vo.SoapSyncTaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/soap-exchange-jobs")
@RequirePermission("soap:exchange:view")
public class SoapSyncTaskController {

    private final SoapSyncTaskService soapSyncTaskService;

    public SoapSyncTaskController(SoapSyncTaskService soapSyncTaskService) {
        this.soapSyncTaskService = soapSyncTaskService;
    }

    @PostMapping
    @RequirePermission("soap:exchange:execute")
    public Result<SoapExchangeRecordVO> create(@Valid @RequestBody SoapSyncTaskDTO dto) {
        return Result.success(soapSyncTaskService.createAndExecute(dto));
    }

    @GetMapping
    public Result<PageResult<SoapSyncTaskVO>> page(@Valid SoapSyncTaskQuery query) {
        return Result.success(soapSyncTaskService.page(query));
    }

    @GetMapping("/{jobId}")
    public Result<SoapExchangeRecordVO> detail(@PathVariable Long jobId) {
        return Result.success(soapSyncTaskService.detail(jobId));
    }

    @PostMapping("/{jobId}/retries")
    @RequirePermission("soap:exchange:execute")
    public Result<SoapExchangeRecordVO> retry(
            @PathVariable Long jobId, @RequestBody(required = false) SoapRetryDTO dto) {
        return Result.success(soapSyncTaskService.retry(jobId, dto));
    }
}
