package com.bdis.modules.assistant.knowledge.config;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class HerbVectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(HerbVectorStoreConfig.class);

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public SimpleVectorStore herbKnowledgeVectorStore(
            EmbeddingModel embeddingModel, HerbAssistantRagProperties properties) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        if (StringUtils.hasText(properties.getVectorStorePath())) {
            File vectorFile = new File(properties.getVectorStorePath());
            if (vectorFile.exists() && vectorFile.isFile()) {
                try {
                    vectorStore.load(vectorFile);
                } catch (RuntimeException exception) {
                    log.warn("Failed to load herb knowledge vector store: {}", vectorFile, exception);
                }
            }
        }
        return vectorStore;
    }
}
