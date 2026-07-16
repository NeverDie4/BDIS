package com.bdis.modules.growth.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.DigitalLifeNarrationService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/digital-life/task")
@RequirePermission("growth:record:view")
public class HerbDigitalLifeArchiveController {

    private final HerbDigitalLifeArchiveService archiveService;
    private final DigitalLifeNarrationService narrationService;
    private final DigitalLifeIntegrityService integrityService;

    public HerbDigitalLifeArchiveController(
            HerbDigitalLifeArchiveService archiveService,
            DigitalLifeNarrationService narrationService,
            DigitalLifeIntegrityService integrityService) {
        this.archiveService = archiveService;
        this.narrationService = narrationService;
        this.integrityService = integrityService;
    }

    @GetMapping("/{taskId}")
    public Result<HerbDigitalLifeArchiveVO> getByTaskId(@PathVariable Long taskId) {
        return Result.success(archiveService.getByTaskId(taskId));
    }

    @PostMapping("/{taskId}/narrations/generate")
    public Result<DigitalLifeNarrationGenerationVO> generateNarrations(@PathVariable Long taskId) {
        return Result.success(narrationService.generateForTask(taskId));
    }

    @PostMapping("/{taskId}/integrity/generate")
    public Result<DigitalLifeIntegrityVO> generateIntegrity(@PathVariable Long taskId) {
        return Result.success(integrityService.generate(taskId));
    }

    @GetMapping("/{taskId}/integrity/verify")
    public Result<DigitalLifeIntegrityVO> verifyIntegrity(@PathVariable Long taskId) {
        return Result.success(integrityService.verify(taskId));
    }
}
