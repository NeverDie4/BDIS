package com.bdis.modules.assistant.knowledge.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HerbKnowledgeEmbeddingBuildResultVO {

    private Integer total;
    private Integer success;
    private Integer failed;
    private boolean mockEmbedding;
    private List<HerbKnowledgeEmbeddingBuildItemVO> items;
}
