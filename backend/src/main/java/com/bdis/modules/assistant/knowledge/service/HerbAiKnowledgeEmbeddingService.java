package com.bdis.modules.assistant.knowledge.service;

import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeChunkRebuildResultVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildResultVO;

public interface HerbAiKnowledgeEmbeddingService {

    HerbKnowledgeChunkRebuildResultVO rebuildChunks(Long docId);

    HerbKnowledgeEmbeddingBuildResultVO buildEmbedding(Long docId);

    HerbKnowledgeEmbeddingBuildResultVO buildPending();

    void deleteEmbedding(Long docId);
}
