package com.bdis.modules.map.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.map.dto.HerbBaseRequest;
import com.bdis.modules.map.query.HerbBaseQuery;
import com.bdis.modules.map.service.HerbBaseService;
import com.bdis.modules.map.vo.HerbBaseVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb-bases")
@RequirePermission("map:base:view")
public class HerbBaseController {

    private final HerbBaseService herbBaseService;

    public HerbBaseController(HerbBaseService herbBaseService) {
        this.herbBaseService = herbBaseService;
    }

    @GetMapping
    public Result<PageResult<HerbBaseVO>> page(@Valid HerbBaseQuery query) {
        return Result.success(herbBaseService.page(query));
    }

    @GetMapping("/enabled")
    public Result<List<HerbBaseVO>> enabled() {
        return Result.success(herbBaseService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<HerbBaseVO> detail(@PathVariable Long id) {
        return Result.success(herbBaseService.detail(id));
    }

    @PostMapping
    @RequirePermission("map:base:manage")
    public Result<Long> create(@Valid @RequestBody HerbBaseRequest request) {
        return Result.success(herbBaseService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("map:base:manage")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HerbBaseRequest request) {
        herbBaseService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("map:base:manage")
    public Result<Void> delete(@PathVariable Long id) {
        herbBaseService.delete(id);
        return Result.success();
    }
}
