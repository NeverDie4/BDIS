package com.bdis.modules.declaration.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.declaration.dto.DeclarationArchiveItemRequest;
import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationArchiveItemEntity;
import com.bdis.modules.declaration.service.DeclarationArchiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class DeclarationArchiveController {

    private final DeclarationArchiveService declarationArchiveService;

    @PostMapping("/declarations/{declarationId}/archives")
    public Result<DeclarationArchiveEntity> generateArchive(@PathVariable Long declarationId) {
        return Result.success(declarationArchiveService.generateArchive(declarationId));
    }

    @PostMapping("/declaration-archives/{archiveId}/items")
    public Result<DeclarationArchiveItemEntity> addArchiveItem(
            @PathVariable Long archiveId,
            @Valid @RequestBody DeclarationArchiveItemRequest request) {
        return Result.success(declarationArchiveService.addArchiveItem(archiveId, request));
    }
}
