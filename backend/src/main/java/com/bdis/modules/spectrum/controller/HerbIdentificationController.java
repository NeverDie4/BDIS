package com.bdis.modules.spectrum.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.spectrum.dto.HerbIdentificationQueryRequest;
import com.bdis.modules.spectrum.dto.HerbIdentificationReviewRequest;
import com.bdis.modules.spectrum.dto.HerbIdentifyRequest;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import com.bdis.modules.spectrum.vo.HerbIdentificationPageVO;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb")
@RequirePermission("herb:identification:view")
public class HerbIdentificationController {

    private final HerbIdentificationService herbIdentificationService;

    public HerbIdentificationController(HerbIdentificationService herbIdentificationService) {
        this.herbIdentificationService = herbIdentificationService;
    }

    @PostMapping("/image/{imageId}/identify")
    @RequirePermission("herb:identification:execute")
    public Result<HerbIdentificationVO> identify(
            @PathVariable Long imageId,
            @RequestBody(required = false) HerbIdentifyRequest request) {
        return Result.success(herbIdentificationService.identify(imageId, request));
    }

    @GetMapping("/image/{imageId}/identification/latest")
    public Result<HerbIdentificationVO> latest(@PathVariable Long imageId) {
        return Result.success(herbIdentificationService.latest(imageId));
    }

    @GetMapping("/image/{imageId}/identifications")
    public Result<List<HerbIdentificationVO>> history(@PathVariable Long imageId) {
        return Result.success(herbIdentificationService.history(imageId));
    }

    @GetMapping("/identification/page")
    public Result<PageResult<HerbIdentificationPageVO>> page(
            @ModelAttribute HerbIdentificationQueryRequest request) {
        return Result.success(herbIdentificationService.page(request));
    }

    @PutMapping("/identification/{id}/review")
    @RequirePermission("herb:identification:review")
    public Result<HerbIdentificationVO> review(
            @PathVariable Long id,
            @RequestBody(required = false) HerbIdentificationReviewRequest request) {
        return Result.success(herbIdentificationService.review(id, request));
    }
}
