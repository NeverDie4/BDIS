package com.bdis.modules.spectrum.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.spectrum.dto.HerbAtlasImportRequest;
import com.bdis.modules.spectrum.service.HerbAtlasImportService;
import com.bdis.modules.spectrum.vo.HerbAtlasImportResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/atlas")
@RequirePermission("herb:atlas:import")
public class HerbAtlasImportController {

    private final HerbAtlasImportService herbAtlasImportService;

    public HerbAtlasImportController(HerbAtlasImportService herbAtlasImportService) {
        this.herbAtlasImportService = herbAtlasImportService;
    }

    @PostMapping("/import")
    public Result<HerbAtlasImportResultVO> importAtlas(
            @RequestBody(required = false) HerbAtlasImportRequest request) {
        return Result.success(herbAtlasImportService.importAtlas(request));
    }
}
