package com.bdis.modules.assistant.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HerbKnowledgeChunkRebuildResultVO {

    private Long docId;
    private String docCode;
    private Integer chunkCount;
}
