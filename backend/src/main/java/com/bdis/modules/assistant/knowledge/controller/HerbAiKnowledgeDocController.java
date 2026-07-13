package com.bdis.modules.assistant.knowledge.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeChunkQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocCreateRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocUpdateRequest;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeDocService;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeEmbeddingService;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeChunkVO;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeDocVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeChunkRebuildResultVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildResultVO;
import com.bdis.modules.permission.service.AuthorizationService;
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
@RequestMapping("/herb/assistant/knowledge/docs")
public class HerbAiKnowledgeDocController {

    private final HerbAiKnowledgeDocService knowledgeDocService;
    private final HerbAiKnowledgeEmbeddingService embeddingService;
    private final AuthorizationService authorizationService;

    public HerbAiKnowledgeDocController(
            HerbAiKnowledgeDocService knowledgeDocService,
            HerbAiKnowledgeEmbeddingService embeddingService,
            AuthorizationService authorizationService) {
        this.knowledgeDocService = knowledgeDocService;
        this.embeddingService = embeddingService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public Result<HerbAiKnowledgeDocVO> create(
            @Valid @RequestBody HerbAiKnowledgeDocCreateRequest request) {
        requireManagePermission();
        return Result.success(knowledgeDocService.create(request));
    }

    @PutMapping("/{id}")
    public Result<HerbAiKnowledgeDocVO> update(
            @PathVariable Long id, @Valid @RequestBody HerbAiKnowledgeDocUpdateRequest request) {
        requireManagePermission();
        return Result.success(knowledgeDocService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireManagePermission();
        knowledgeDocService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<HerbAiKnowledgeDocVO> getById(@PathVariable Long id) {
        requireViewPermission();
        return Result.success(knowledgeDocService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<HerbAiKnowledgeDocVO>> page(
            @Valid @ModelAttribute HerbAiKnowledgeDocQueryRequest request) {
        requireViewPermission();
        return Result.success(knowledgeDocService.page(request));
    }

    @PutMapping("/{id}/enable")
    public Result<HerbAiKnowledgeDocVO> enable(@PathVariable Long id) {
        requireManagePermission();
        return Result.success(knowledgeDocService.enable(id));
    }

    @PutMapping("/{id}/disable")
    public Result<HerbAiKnowledgeDocVO> disable(@PathVariable Long id) {
        requireManagePermission();
        return Result.success(knowledgeDocService.disable(id));
    }

    @GetMapping("/{id}/chunks")
    public Result<List<HerbAiKnowledgeChunkVO>> listChunks(
            @PathVariable Long id, @ModelAttribute HerbAiKnowledgeChunkQueryRequest request) {
        requireViewPermission();
        return Result.success(knowledgeDocService.listChunks(id, request));
    }

    @PostMapping("/{id}/chunks/rebuild")
    public Result<HerbKnowledgeChunkRebuildResultVO> rebuildChunks(@PathVariable Long id) {
        requireManagePermission();
        return Result.success(embeddingService.rebuildChunks(id));
    }

    @PostMapping("/{id}/embedding/build")
    public Result<HerbKnowledgeEmbeddingBuildResultVO> buildEmbedding(@PathVariable Long id) {
        requireManagePermission();
        return Result.success(embeddingService.buildEmbedding(id));
    }

    @DeleteMapping("/{id}/embedding")
    public Result<Void> deleteEmbedding(@PathVariable Long id) {
        requireManagePermission();
        embeddingService.deleteEmbedding(id);
        return Result.success();
    }

    private void requireViewPermission() {
        authorizationService.requirePermission("herb:assistant:knowledge:view");
    }

    private void requireManagePermission() {
        authorizationService.requirePermission("herb:assistant:knowledge:manage");
    }
}
