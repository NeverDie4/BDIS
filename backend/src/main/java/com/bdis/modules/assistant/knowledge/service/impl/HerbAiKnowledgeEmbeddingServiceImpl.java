package com.bdis.modules.assistant.knowledge.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.bdis.modules.assistant.knowledge.constant.HerbAiEmbeddingStatusConstants;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeChunk;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeDoc;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeChunkMapper;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeDocMapper;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeEmbeddingService;
import com.bdis.modules.assistant.knowledge.util.HerbTextChunkUtils;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeChunkRebuildResultVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildItemVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbAiKnowledgeEmbeddingServiceImpl implements HerbAiKnowledgeEmbeddingService {

    private static final Logger log =
            LoggerFactory.getLogger(HerbAiKnowledgeEmbeddingServiceImpl.class);

    private final HerbAiKnowledgeDocMapper docMapper;
    private final HerbAiKnowledgeChunkMapper chunkMapper;
    private final HerbAssistantRagProperties properties;
    private final ObjectProvider<SimpleVectorStore> vectorStoreProvider;
    private final ObjectMapper objectMapper;

    public HerbAiKnowledgeEmbeddingServiceImpl(
            HerbAiKnowledgeDocMapper docMapper,
            HerbAiKnowledgeChunkMapper chunkMapper,
            HerbAssistantRagProperties properties,
            ObjectProvider<SimpleVectorStore> vectorStoreProvider,
            ObjectMapper objectMapper) {
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.properties = properties;
        this.vectorStoreProvider = vectorStoreProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public HerbKnowledgeChunkRebuildResultVO rebuildChunks(Long docId) {
        HerbAiKnowledgeDoc doc = requireDoc(docId);
        LocalDateTime now = LocalDateTime.now();
        removeVectors(doc.getId());
        chunkMapper.logicDeleteByDocId(doc.getId(), now);
        List<String> chunks =
                HerbTextChunkUtils.split(
                        doc.getContentText(),
                        defaultInt(properties.getChunkSize(), 800),
                        defaultInt(properties.getChunkOverlap(), 100));
        int chunkIndex = 1;
        for (String content : chunks) {
            HerbAiKnowledgeChunk chunk = new HerbAiKnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setChunkIndex(chunkIndex);
            chunk.setChunkTitle(buildChunkTitle(doc, chunkIndex));
            chunk.setChunkContent(content);
            chunk.setContentHash(HerbTextChunkUtils.sha256(content));
            chunk.setTokenCount(content.length());
            chunk.setEmbeddingStatus(HerbAiEmbeddingStatusConstants.PENDING);
            chunk.setMetadataJson(metadataJson(doc, null, chunkIndex));
            chunk.setCreateTime(now);
            chunk.setUpdateTime(now);
            chunk.setDeleted(0);
            chunkMapper.insert(chunk);
            if (chunk.getId() != null) {
                chunk.setMetadataJson(metadataJson(doc, chunk.getId(), chunkIndex));
                chunkMapper.updateEmbeddingStatus(
                        chunk.getId(), HerbAiEmbeddingStatusConstants.PENDING, null, now);
            }
            chunkIndex++;
        }
        docMapper.updateChunkCountAndEmbeddingStatus(
                doc.getId(), chunks.size(), HerbAiEmbeddingStatusConstants.PENDING, now);
        return new HerbKnowledgeChunkRebuildResultVO(doc.getId(), doc.getDocCode(), chunks.size());
    }

    @Override
    @Transactional
    public HerbKnowledgeEmbeddingBuildResultVO buildEmbedding(Long docId) {
        HerbAiKnowledgeDoc doc = requireDoc(docId);
        HerbKnowledgeEmbeddingBuildItemVO item = buildOne(doc);
        return new HerbKnowledgeEmbeddingBuildResultVO(
                1,
                item.isSuccess() ? 1 : 0,
                item.isSuccess() ? 0 : 1,
                properties.isMockEmbeddingEnabled(),
                List.of(item));
    }

    @Override
    public HerbKnowledgeEmbeddingBuildResultVO buildPending() {
        List<HerbAiKnowledgeDoc> docs = docMapper.selectPendingEnabled();
        List<HerbKnowledgeEmbeddingBuildItemVO> items = new ArrayList<>();
        int success = 0;
        int failed = 0;
        for (HerbAiKnowledgeDoc doc : docs) {
            try {
                HerbKnowledgeEmbeddingBuildItemVO item = buildOne(doc);
                items.add(item);
                if (item.isSuccess()) {
                    success++;
                } else {
                    failed++;
                }
            } catch (RuntimeException exception) {
                failed++;
                items.add(
                        new HerbKnowledgeEmbeddingBuildItemVO(
                                doc.getId(),
                                doc.getDocCode(),
                                safeChunkCount(doc),
                                false,
                                exception.getMessage()));
                log.warn(
                        "Failed to build herb knowledge embedding for doc {}",
                        doc.getId(),
                        exception);
            }
        }
        return new HerbKnowledgeEmbeddingBuildResultVO(
                docs.size(), success, failed, properties.isMockEmbeddingEnabled(), items);
    }

    @Override
    @Transactional
    public void deleteEmbedding(Long docId) {
        HerbAiKnowledgeDoc doc = requireDoc(docId);
        removeVectors(doc.getId());
        LocalDateTime now = LocalDateTime.now();
        chunkMapper.resetEmbeddingStatusByDocId(
                doc.getId(), HerbAiEmbeddingStatusConstants.PENDING, now);
        docMapper.updateEmbeddingStatus(doc.getId(), HerbAiEmbeddingStatusConstants.PENDING, now);
    }

    private void removeVectors(Long docId) {
        List<HerbAiKnowledgeChunk> chunks = chunkMapper.selectEntitiesByDocId(docId);
        SimpleVectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            List<String> vectorIds =
                    chunks.stream()
                            .map(HerbAiKnowledgeChunk::getVectorId)
                            .filter(StringUtils::hasText)
                            .toList();
            if (!vectorIds.isEmpty()) {
                vectorStore.delete(vectorIds);
                saveVectorStore(vectorStore);
            }
        } else {
            log.info("VectorStore is unavailable, reset database embedding status only.");
        }
    }

    private HerbKnowledgeEmbeddingBuildItemVO buildOne(HerbAiKnowledgeDoc doc) {
        List<HerbAiKnowledgeChunk> chunks = chunkMapper.selectEntitiesByDocId(doc.getId());
        if (chunks.isEmpty()) {
            rebuildChunks(doc.getId());
            doc = requireDoc(doc.getId());
            chunks = chunkMapper.selectEntitiesByDocId(doc.getId());
        }
        LocalDateTime now = LocalDateTime.now();
        docMapper.updateEmbeddingStatus(
                doc.getId(), HerbAiEmbeddingStatusConstants.PROCESSING, now);
        try {
            if (properties.isMockEmbeddingEnabled()) {
                for (HerbAiKnowledgeChunk chunk : chunks) {
                    chunkMapper.updateEmbeddingStatus(
                            chunk.getId(),
                            HerbAiEmbeddingStatusConstants.COMPLETED,
                            vectorId(chunk),
                            now);
                }
                docMapper.updateEmbeddingStatus(
                        doc.getId(), HerbAiEmbeddingStatusConstants.COMPLETED, now);
                log.info(
                        "Mock embedding enabled, skipped VectorStore write for doc {}",
                        doc.getId());
                return new HerbKnowledgeEmbeddingBuildItemVO(
                        doc.getId(),
                        doc.getDocCode(),
                        chunks.size(),
                        true,
                        "mock embedding enabled, skipped vector store write");
            }

            SimpleVectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) {
                throw new BusinessException("VectorStore 未配置，无法执行真实向量化");
            }
            HerbAiKnowledgeDoc currentDoc = doc;
            List<Document> documents =
                    chunks.stream().map(chunk -> toDocument(currentDoc, chunk)).toList();
            vectorStore.add(documents);
            saveVectorStore(vectorStore);
            for (HerbAiKnowledgeChunk chunk : chunks) {
                chunkMapper.updateEmbeddingStatus(
                        chunk.getId(),
                        HerbAiEmbeddingStatusConstants.COMPLETED,
                        vectorId(chunk),
                        now);
            }
            docMapper.updateEmbeddingStatus(
                    doc.getId(), HerbAiEmbeddingStatusConstants.COMPLETED, now);
            return new HerbKnowledgeEmbeddingBuildItemVO(
                    doc.getId(), doc.getDocCode(), chunks.size(), true, "completed");
        } catch (RuntimeException exception) {
            LocalDateTime failedAt = LocalDateTime.now();
            for (HerbAiKnowledgeChunk chunk : chunks) {
                chunkMapper.updateEmbeddingStatus(
                        chunk.getId(),
                        HerbAiEmbeddingStatusConstants.FAILED,
                        chunk.getVectorId(),
                        failedAt);
            }
            docMapper.updateEmbeddingStatus(
                    doc.getId(), HerbAiEmbeddingStatusConstants.FAILED, failedAt);
            log.warn("Failed to build herb knowledge embedding for doc {}", doc.getId(), exception);
            return new HerbKnowledgeEmbeddingBuildItemVO(
                    doc.getId(), doc.getDocCode(), chunks.size(), false, exception.getMessage());
        }
    }

    private Document toDocument(HerbAiKnowledgeDoc doc, HerbAiKnowledgeChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("docId", doc.getId());
        metadata.put("docCode", doc.getDocCode());
        metadata.put("docTitle", doc.getDocTitle());
        metadata.put("docType", doc.getDocType());
        metadata.put("chunkId", chunk.getId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        return Document.builder()
                .id(vectorId(chunk))
                .text(chunk.getChunkContent())
                .metadata(metadata)
                .build();
    }

    private HerbAiKnowledgeDoc requireDoc(Long docId) {
        if (docId == null || docId <= 0) {
            throw new BusinessException("知识库文档 ID 无效");
        }
        HerbAiKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException("知识库文档不存在");
        }
        return doc;
    }

    private String buildChunkTitle(HerbAiKnowledgeDoc doc, int chunkIndex) {
        return doc.getDocTitle() + " #" + chunkIndex;
    }

    private String metadataJson(HerbAiKnowledgeDoc doc, Long chunkId, int chunkIndex) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("docId", doc.getId());
        metadata.put("docCode", doc.getDocCode());
        metadata.put("docTitle", doc.getDocTitle());
        metadata.put("docType", doc.getDocType());
        if (chunkId != null) {
            metadata.put("chunkId", chunkId);
        }
        metadata.put("chunkIndex", chunkIndex);
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("知识库切片元数据生成失败");
        }
    }

    private void saveVectorStore(SimpleVectorStore vectorStore) {
        if (!StringUtils.hasText(properties.getVectorStorePath())) {
            return;
        }
        File vectorFile = new File(properties.getVectorStorePath());
        File parent = vectorFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new BusinessException("向量库目录创建失败");
        }
        vectorStore.save(vectorFile);
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int safeChunkCount(HerbAiKnowledgeDoc doc) {
        return doc.getChunkCount() == null ? 0 : doc.getChunkCount();
    }

    private String vectorId(HerbAiKnowledgeChunk chunk) {
        return "chunk-" + chunk.getId();
    }
}
