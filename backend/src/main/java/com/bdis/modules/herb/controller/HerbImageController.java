package com.bdis.modules.herb.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.herb.dto.HerbImageQueryRequest;
import com.bdis.modules.herb.dto.HerbImageUpdateRequest;
import com.bdis.modules.herb.dto.HerbImageUploadRequest;
import com.bdis.modules.herb.service.HerbImageService;
import com.bdis.modules.herb.vo.HerbImageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/herb/image")
@RequirePermission("herb:identification:view")
public class HerbImageController {

    private final HerbImageService herbImageService;

    public HerbImageController(HerbImageService herbImageService) {
        this.herbImageService = herbImageService;
    }

    @PostMapping("/upload")
    @RequirePermission("herb:identification:execute")
    public Result<HerbImageVO> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute HerbImageUploadRequest request) {
        return Result.success(herbImageService.upload(file, request));
    }

    @PutMapping("/{id}")
    @RequirePermission("herb:identification:execute")
    public Result<HerbImageVO> update(
            @PathVariable Long id, @RequestBody HerbImageUpdateRequest request) {
        return Result.success(herbImageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("herb:identification:execute")
    public Result<Void> delete(@PathVariable Long id) {
        herbImageService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<HerbImageVO> getById(@PathVariable Long id) {
        return Result.success(herbImageService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<HerbImageVO>> page(@ModelAttribute HerbImageQueryRequest request) {
        return Result.success(herbImageService.page(request));
    }

    @GetMapping("/my")
    public Result<PageResult<HerbImageVO>> my(Long collectorId, Integer pageNum, Integer pageSize) {
        return Result.success(herbImageService.my(collectorId, pageNum, pageSize));
    }
}
