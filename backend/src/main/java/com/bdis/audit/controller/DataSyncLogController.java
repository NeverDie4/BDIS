package com.bdis.audit.controller;

import com.bdis.audit.query.DataSyncLogQuery;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.audit.vo.DataSyncLogVO;
import com.bdis.common.response.ApiResponse;
import com.bdis.common.response.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-sync-logs")
public class DataSyncLogController {

    private final DataSyncLogService dataSyncLogService;

    public DataSyncLogController(DataSyncLogService dataSyncLogService) {
        this.dataSyncLogService = dataSyncLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<DataSyncLogVO>> page(@Valid DataSyncLogQuery query) {
        return ApiResponse.success(dataSyncLogService.page(query));
    }
}
