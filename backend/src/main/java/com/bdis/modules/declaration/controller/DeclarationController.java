package com.bdis.modules.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.common.core.Result;
import com.bdis.modules.declaration.dto.DeclarationRequest;
import com.bdis.modules.declaration.dto.DeclarationReviewRequest;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationReviewRecordEntity;
import com.bdis.modules.declaration.query.DeclarationQuery;
import com.bdis.modules.declaration.service.DeclarationService;
import com.bdis.modules.declaration.vo.DeclarationDetailVO;
import com.bdis.modules.declaration.vo.DeclarationSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/declarations")
public class DeclarationController {

    private final DeclarationService declarationService;

    @GetMapping
    public Result<IPage<DeclarationEntity>> listDeclarations(DeclarationQuery query) {
        return Result.success(declarationService.listDeclarations(query));
    }

    @PostMapping
    public Result<DeclarationEntity> createDeclaration(
            @Valid @RequestBody DeclarationRequest request) {
        return Result.success(declarationService.createDeclaration(request));
    }

    @GetMapping("/{declarationId}")
    public Result<DeclarationDetailVO> getDeclarationDetail(@PathVariable Long declarationId) {
        return Result.success(declarationService.getDeclarationDetail(declarationId));
    }

    @PostMapping("/{declarationId}/submissions")
    public Result<DeclarationEntity> submitDeclaration(@PathVariable Long declarationId) {
        return Result.success(declarationService.submitDeclaration(declarationId));
    }

    @PostMapping("/{declarationId}/reviews")
    public Result<DeclarationReviewRecordEntity> reviewDeclaration(
            @PathVariable Long declarationId,
            @Valid @RequestBody DeclarationReviewRequest request) {
        return Result.success(declarationService.reviewDeclaration(declarationId, request));
    }

    @GetMapping("/{declarationId}/summary")
    public Result<DeclarationSummaryVO> getDeclarationSummary(@PathVariable Long declarationId) {
        return Result.success(declarationService.getDeclarationSummary(declarationId));
    }
}
