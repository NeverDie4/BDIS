package com.bdis.modules.spectrum.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.spectrum.dto.AtlasFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.dto.ImageFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractResultVO;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb")
public class HerbFeatureController {

    private final HerbFeatureService herbFeatureService;

    public HerbFeatureController(HerbFeatureService herbFeatureService) {
        this.herbFeatureService = herbFeatureService;
    }

    @PostMapping("/atlas/{atlasId}/feature/extract")
    public Result<FeatureExtractResultVO> extractAtlasFeature(@PathVariable Long atlasId) {
        return Result.success(herbFeatureService.extractAtlasFeature(atlasId));
    }

    @PostMapping("/atlas/feature/batch-extract")
    public Result<FeatureBatchExtractResultVO> batchExtractAtlasFeatures(
            @RequestBody(required = false) AtlasFeatureBatchExtractRequest request) {
        return Result.success(herbFeatureService.batchExtractAtlasFeatures(request));
    }

    @PostMapping("/image/feature/batch-extract")
    public Result<FeatureBatchExtractResultVO> batchExtractImageFeatures(
            @RequestBody(required = false) ImageFeatureBatchExtractRequest request) {
        return Result.success(herbFeatureService.batchExtractImageFeatures(request));
    }

    @PostMapping("/image/{imageId}/feature/extract")
    public Result<FeatureExtractResultVO> extractImageFeature(@PathVariable Long imageId) {
        return Result.success(herbFeatureService.extractImageFeature(imageId));
    }

    @GetMapping("/atlas/{atlasId}/feature")
    public Result<FeatureExtractResultVO> getAtlasFeature(
            @PathVariable Long atlasId,
            @RequestParam(defaultValue = "false") boolean includeVector) {
        return Result.success(herbFeatureService.getAtlasFeature(atlasId, includeVector));
    }

    @GetMapping("/image/{imageId}/feature")
    public Result<FeatureExtractResultVO> getImageFeature(
            @PathVariable Long imageId,
            @RequestParam(defaultValue = "false") boolean includeVector) {
        return Result.success(herbFeatureService.getImageFeature(imageId, includeVector));
    }
}
