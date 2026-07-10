package com.bdis.soap.controller;

import com.bdis.common.response.ApiResponse;
import com.bdis.common.response.PageResult;
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
public class SoapSyncTaskController {

    private final SoapSyncTaskService soapSyncTaskService;

    public SoapSyncTaskController(SoapSyncTaskService soapSyncTaskService) {
        this.soapSyncTaskService = soapSyncTaskService;
    }

    @PostMapping
    public ApiResponse<SoapExchangeRecordVO> create(@Valid @RequestBody SoapSyncTaskDTO dto) {
        return ApiResponse.success(soapSyncTaskService.createAndExecute(dto));
    }

    @GetMapping
    public ApiResponse<PageResult<SoapSyncTaskVO>> page(@Valid SoapSyncTaskQuery query) {
        return ApiResponse.success(soapSyncTaskService.page(query));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<SoapExchangeRecordVO> detail(@PathVariable Long jobId) {
        return ApiResponse.success(soapSyncTaskService.detail(jobId));
    }

    @PostMapping("/{jobId}/retries")
    public ApiResponse<SoapExchangeRecordVO> retry(
            @PathVariable Long jobId, @RequestBody(required = false) SoapRetryDTO dto) {
        return ApiResponse.success(soapSyncTaskService.retry(jobId, dto));
    }
}
