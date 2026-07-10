package com.bdis.modules.mobile.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.mobile.service.MobileHerbBatchService;
import com.bdis.modules.mobile.vo.MobileImageIdentificationVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mobile/herb/images")
public class MobileHerbImageController {

    private final MobileHerbBatchService mobileHerbBatchService;

    public MobileHerbImageController(MobileHerbBatchService mobileHerbBatchService) {
        this.mobileHerbBatchService = mobileHerbBatchService;
    }

    @GetMapping("/{imageId}/identification/latest")
    public Result<MobileImageIdentificationVO> latestIdentification(@PathVariable Long imageId) {
        return Result.success(mobileHerbBatchService.latestIdentification(imageId));
    }
}
