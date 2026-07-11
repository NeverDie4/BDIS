package com.bdis.modules.assistant.vo;

import lombok.Data;

@Data
public class HerbAssistantRagReferenceVO {

    private Long docId;

    private String docTitle;

    private Long chunkId;

    private Integer chunkIndex;

    private Double score;

    private String contentPreview;
}
