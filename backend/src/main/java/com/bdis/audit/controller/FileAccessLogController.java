package com.bdis.audit.controller;

import com.bdis.audit.query.FileAccessLogQuery;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.audit.vo.FileAccessLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file-access-logs")
public class FileAccessLogController {

    private final FileAccessLogService fileAccessLogService;

    public FileAccessLogController(FileAccessLogService fileAccessLogService) {
        this.fileAccessLogService = fileAccessLogService;
    }

    @GetMapping
    public Result<PageResult<FileAccessLogVO>> page(@Valid FileAccessLogQuery query) {
        return Result.success(fileAccessLogService.page(query));
    }
}
