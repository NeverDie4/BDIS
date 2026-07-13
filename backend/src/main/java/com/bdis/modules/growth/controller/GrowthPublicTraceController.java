package com.bdis.modules.growth.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthPublicTraceArchiveVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trace/growth")
public class GrowthPublicTraceController {

    private final GrowthRecordService growthRecordService;

    public GrowthPublicTraceController(GrowthRecordService growthRecordService) {
        this.growthRecordService = growthRecordService;
    }

    @GetMapping("/{traceCode}")
    public Result<GrowthPublicTraceArchiveVO> publicTrace(@PathVariable String traceCode) {
        return Result.success(growthRecordService.publicTrace(traceCode));
    }
}
