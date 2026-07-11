package com.bdis.modules.spectrum.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.spectrum.dto.HerbRecognitionQueryRequest;
import com.bdis.modules.spectrum.service.HerbRecognitionService;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb")
public class HerbRecognitionController {

    private final HerbRecognitionService herbRecognitionService;

    public HerbRecognitionController(HerbRecognitionService herbRecognitionService) {
        this.herbRecognitionService = herbRecognitionService;
    }

    @PostMapping("/image/{imageId}/recognize")
    public Result<HerbRecognitionVO> recognize(@PathVariable Long imageId) {
        return Result.success(herbRecognitionService.recognize(imageId));
    }

    @GetMapping("/image/{imageId}/recognition/latest")
    public Result<HerbRecognitionVO> latest(@PathVariable Long imageId) {
        return Result.success(herbRecognitionService.latest(imageId));
    }

    @GetMapping("/image/{imageId}/recognitions")
    public Result<List<HerbRecognitionVO>> history(@PathVariable Long imageId) {
        return Result.success(herbRecognitionService.history(imageId));
    }

    @GetMapping("/recognition/page")
    public Result<PageResult<HerbRecognitionVO>> page(
            @ModelAttribute HerbRecognitionQueryRequest request) {
        return Result.success(herbRecognitionService.page(request));
    }
}
