package com.bdis.modules.assistant.knowledge.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeChunkQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocCreateRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocUpdateRequest;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeChunkVO;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeDocVO;
import java.util.List;

public interface HerbAiKnowledgeDocService {

    HerbAiKnowledgeDocVO create(HerbAiKnowledgeDocCreateRequest request);

    HerbAiKnowledgeDocVO update(Long id, HerbAiKnowledgeDocUpdateRequest request);

    void delete(Long id);

    HerbAiKnowledgeDocVO getById(Long id);

    PageResult<HerbAiKnowledgeDocVO> page(HerbAiKnowledgeDocQueryRequest request);

    HerbAiKnowledgeDocVO enable(Long id);

    HerbAiKnowledgeDocVO disable(Long id);

    List<HerbAiKnowledgeChunkVO> listChunks(
            Long docId, HerbAiKnowledgeChunkQueryRequest request);
}
