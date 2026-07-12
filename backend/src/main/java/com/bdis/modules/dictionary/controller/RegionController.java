package com.bdis.modules.dictionary.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.dictionary.dto.RegionRequest;
import com.bdis.modules.dictionary.service.DictionaryService;
import com.bdis.modules.dictionary.vo.RegionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/regions")
@RequirePermission("dictionary:view")
public class RegionController {

    private final DictionaryService dictionaryService;

    public RegionController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/tree")
    public Result<List<RegionVO>> tree(@RequestParam(required = false) Integer status) {
        return Result.success(dictionaryService.regionTree(status));
    }

    @PostMapping
    @RequirePermission("dictionary:manage")
    public Result<Long> create(@Valid @RequestBody RegionRequest request) {
        return Result.success(dictionaryService.createRegion(request));
    }

    @PutMapping("/{regionId}")
    @RequirePermission("dictionary:manage")
    public Result<Void> update(
            @PathVariable Long regionId, @Valid @RequestBody RegionRequest request) {
        dictionaryService.updateRegion(regionId, request);
        return Result.success();
    }

    @DeleteMapping("/{regionId}")
    @RequirePermission("dictionary:manage")
    public Result<Void> delete(@PathVariable Long regionId) {
        dictionaryService.deleteRegion(regionId);
        return Result.success();
    }
}
