package com.bdis.audit.controller;

import com.bdis.audit.query.DataSyncLogQuery;
import com.bdis.audit.service.DataSyncLogService;
import com.bdis.audit.vo.DataSyncLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-sync-logs")
@RequirePermission("audit:log:view")
public class DataSyncLogController {

    private final DataSyncLogService dataSyncLogService;

    public DataSyncLogController(DataSyncLogService dataSyncLogService) {
        this.dataSyncLogService = dataSyncLogService;
    }

    @GetMapping
    public Result<PageResult<DataSyncLogVO>> page(@Valid DataSyncLogQuery query) {
        return Result.success(dataSyncLogService.page(query));
    }
}
