package com.bdis.modules.herb.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.herb.dto.HerbSpeciesCreateRequest;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.dto.HerbSpeciesUpdateRequest;
import com.bdis.modules.herb.service.HerbSpeciesService;
import com.bdis.modules.herb.vo.HerbSpeciesVO;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/species")
public class HerbSpeciesController {

    private final HerbSpeciesService herbSpeciesService;

    public HerbSpeciesController(HerbSpeciesService herbSpeciesService) {
        this.herbSpeciesService = herbSpeciesService;
    }

    @PostMapping
    public Result<HerbSpeciesVO> create(@Valid @RequestBody HerbSpeciesCreateRequest request) {
        return Result.success(herbSpeciesService.create(request));
    }

    @PutMapping("/{id}")
    public Result<HerbSpeciesVO> update(
            @PathVariable Long id, @Valid @RequestBody HerbSpeciesUpdateRequest request) {
        return Result.success(herbSpeciesService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        herbSpeciesService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<HerbSpeciesVO> getById(@PathVariable Long id) {
        return Result.success(herbSpeciesService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<HerbSpeciesVO>> page(@ModelAttribute HerbSpeciesQueryRequest request) {
        return Result.success(herbSpeciesService.page(request));
    }

    @GetMapping("/list")
    public Result<List<HerbSpeciesVO>> listEnabled() {
        return Result.success(herbSpeciesService.listEnabled());
    }
}
