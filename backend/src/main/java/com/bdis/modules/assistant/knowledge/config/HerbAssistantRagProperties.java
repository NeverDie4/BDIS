package com.bdis.modules.assistant.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "herb.assistant.rag")
public class HerbAssistantRagProperties {

    private boolean enabled = true;
    private boolean mockEmbeddingEnabled = true;
    private boolean embeddingEnabled = false;
    private String embeddingModel;
    private String embeddingBaseUrl;
    private String embeddingApiKey;
    private String vectorStorePath = "data/vector-store/herb-knowledge.json";
    private Integer chunkSize = 800;
    private Integer chunkOverlap = 100;
    private Integer topK = 5;
    private Double similarityThreshold = 0.6;
}
