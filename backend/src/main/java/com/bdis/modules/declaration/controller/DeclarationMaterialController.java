package com.bdis.modules.declaration.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.declaration.dto.DeclarationMaterialRequest;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;
import com.bdis.modules.declaration.service.DeclarationMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/declarations/{declarationId}/materials")
public class DeclarationMaterialController {

    private final DeclarationMaterialService declarationMaterialService;

    @PostMapping
    public Result<DeclarationMaterialEntity> addMaterial(
            @PathVariable Long declarationId,
            @Valid @RequestBody DeclarationMaterialRequest request) {
        return Result.success(declarationMaterialService.addMaterial(declarationId, request));
    }
}
