package com.bdis.modules.training.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.query.TrainingMaterialQuery;
import com.bdis.modules.training.request.TrainingMaterialCreateRequest;
import com.bdis.modules.training.request.TrainingMaterialUpdateRequest;
import com.bdis.modules.training.service.TrainingMaterialService;
import com.bdis.modules.training.vo.TrainingMaterialDetailVO;
import com.bdis.modules.training.vo.TrainingMaterialListVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-materials")
public class TrainingMaterialController {
    private final TrainingMaterialService materialService;
    private final AuthorizationService authorizationService;

    public TrainingMaterialController(
            TrainingMaterialService materialService, AuthorizationService authorizationService) {
        this.materialService = materialService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<TrainingMaterialListVO>> page(
            @Valid @ModelAttribute TrainingMaterialQuery query) {
        authorizationService.requirePermission("edu:training-material:list");
        return Result.success(materialService.page(query));
    }

    @GetMapping("/{id}")
    public Result<TrainingMaterialDetailVO> detail(@PathVariable @Positive Long id) {
        requirePositive(id);
        authorizationService.requirePermission("edu:training-material:detail");
        return Result.success(materialService.getDetail(id));
    }

    @PostMapping
    public Result<TrainingMaterialDetailVO> create(
            @Valid @RequestBody TrainingMaterialCreateRequest request) {
        authorizationService.requirePermission("edu:training-material:add");
        Long id = materialService.create(request);
        return Result.success(materialService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<TrainingMaterialDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody TrainingMaterialUpdateRequest request) {
        requirePositive(id);
        authorizationService.requirePermission("edu:training-material:update");
        materialService.update(id, request);
        return Result.success(materialService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive Long id) {
        requirePositive(id);
        authorizationService.requirePermission("edu:training-material:delete");
        materialService.delete(id);
        return Result.success();
    }

    private void requirePositive(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Training material id must be positive");
        }
    }
}
