package com.bdis.modules.assistant.knowledge.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeEmbeddingService;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildResultVO;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/assistant/knowledge/embedding")
public class HerbAiKnowledgeEmbeddingController {

    private final HerbAiKnowledgeEmbeddingService embeddingService;
    private final AuthorizationService authorizationService;

    public HerbAiKnowledgeEmbeddingController(
            HerbAiKnowledgeEmbeddingService embeddingService,
            AuthorizationService authorizationService) {
        this.embeddingService = embeddingService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/build-pending")
    public Result<HerbKnowledgeEmbeddingBuildResultVO> buildPendingEmbedding() {
        authorizationService.requirePermission("herb:assistant:knowledge:manage");
        return Result.success(embeddingService.buildPending());
    }
}
