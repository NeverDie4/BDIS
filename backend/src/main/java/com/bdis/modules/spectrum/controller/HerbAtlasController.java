package com.bdis.modules.spectrum.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.spectrum.dto.HerbAtlasQueryRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUpdateRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUploadRequest;
import com.bdis.modules.spectrum.service.HerbAtlasService;
import com.bdis.modules.spectrum.vo.HerbAtlasTagVO;
import com.bdis.modules.spectrum.vo.HerbAtlasVO;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/herb/atlas")
@RequirePermission("herb:identification:view")
public class HerbAtlasController {

    private final HerbAtlasService herbAtlasService;

    public HerbAtlasController(HerbAtlasService herbAtlasService) {
        this.herbAtlasService = herbAtlasService;
    }

    @PostMapping("/upload")
    @RequirePermission("herb:identification:execute")
    public Result<HerbAtlasVO> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute HerbAtlasUploadRequest request) {
        return Result.success(herbAtlasService.upload(file, request));
    }

    @PutMapping("/{id}")
    @RequirePermission("herb:identification:execute")
    public Result<HerbAtlasVO> update(
            @PathVariable Long id, @RequestBody HerbAtlasUpdateRequest request) {
        return Result.success(herbAtlasService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("herb:identification:execute")
    public Result<Void> delete(@PathVariable Long id) {
        herbAtlasService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<HerbAtlasVO> getById(@PathVariable Long id) {
        return Result.success(herbAtlasService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<HerbAtlasVO>> page(@ModelAttribute HerbAtlasQueryRequest request) {
        return Result.success(herbAtlasService.page(request));
    }

    @GetMapping("/list")
    public Result<List<HerbAtlasVO>> listEnabled(Long speciesId) {
        return Result.success(herbAtlasService.listEnabled(speciesId));
    }

    @GetMapping("/tags")
    public Result<List<HerbAtlasTagVO>> listTags(Long atlasId) {
        return Result.success(herbAtlasService.listTags(atlasId));
    }
}
