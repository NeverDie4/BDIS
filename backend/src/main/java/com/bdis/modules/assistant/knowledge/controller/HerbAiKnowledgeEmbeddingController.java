package com.bdis.modules.assistant.knowledge.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeEmbeddingService;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/assistant/knowledge/embedding")
public class HerbAiKnowledgeEmbeddingController {

    private final HerbAiKnowledgeEmbeddingService embeddingService;

    public HerbAiKnowledgeEmbeddingController(HerbAiKnowledgeEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/build-pending")
    public Result<HerbKnowledgeEmbeddingBuildResultVO> buildPendingEmbedding() {
        return Result.success(embeddingService.buildPending());
    }
}
