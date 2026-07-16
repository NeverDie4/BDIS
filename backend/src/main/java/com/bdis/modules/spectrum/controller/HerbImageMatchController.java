package com.bdis.modules.spectrum.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.spectrum.dto.HerbImageMatchQueryRequest;
import com.bdis.modules.spectrum.dto.HerbImageMatchRequest;
import com.bdis.modules.spectrum.service.HerbImageMatchService;
import com.bdis.modules.spectrum.vo.HerbImageMatchPageVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb")
@RequirePermission("herb:identification:view")
public class HerbImageMatchController {

    private final HerbImageMatchService herbImageMatchService;

    public HerbImageMatchController(HerbImageMatchService herbImageMatchService) {
        this.herbImageMatchService = herbImageMatchService;
    }

    @PostMapping("/image/{imageId}/match")
    @RequirePermission("herb:identification:execute")
    public Result<HerbImageMatchVO> match(
            @PathVariable Long imageId,
            @RequestBody(required = false) HerbImageMatchRequest request) {
        return Result.success(herbImageMatchService.match(imageId, request));
    }

    @GetMapping("/image/{imageId}/match/latest")
    public Result<HerbImageMatchVO> latest(@PathVariable Long imageId) {
        return Result.success(herbImageMatchService.latest(imageId));
    }

    @GetMapping("/image/{imageId}/matches")
    public Result<List<HerbImageMatchPageVO>> listByImageId(@PathVariable Long imageId) {
        return Result.success(herbImageMatchService.listByImageId(imageId));
    }

    @GetMapping("/match/page")
    public Result<PageResult<HerbImageMatchPageVO>> page(
            @ModelAttribute HerbImageMatchQueryRequest request) {
        return Result.success(herbImageMatchService.page(request));
    }
}
