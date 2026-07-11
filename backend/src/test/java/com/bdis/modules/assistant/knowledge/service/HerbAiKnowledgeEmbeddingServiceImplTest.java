package com.bdis.modules.assistant.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeChunk;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeDoc;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeChunkMapper;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeDocMapper;
import com.bdis.modules.assistant.knowledge.service.impl.HerbAiKnowledgeEmbeddingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class HerbAiKnowledgeEmbeddingServiceImplTest {

    @Mock private HerbAiKnowledgeDocMapper docMapper;
    @Mock private HerbAiKnowledgeChunkMapper chunkMapper;
    @Mock private ObjectProvider<SimpleVectorStore> vectorStoreProvider;
    @Mock private SimpleVectorStore vectorStore;

    private HerbAssistantRagProperties properties;
    private HerbAiKnowledgeEmbeddingService service;

    @BeforeEach
    void setUp() {
        properties = new HerbAssistantRagProperties();
        properties.setChunkSize(20);
        properties.setChunkOverlap(5);
        properties.setMockEmbeddingEnabled(true);
        properties.setVectorStorePath(null);
        service =
                new HerbAiKnowledgeEmbeddingServiceImpl(
                        docMapper,
                        chunkMapper,
                        properties,
                        vectorStoreProvider,
                        new ObjectMapper());
    }

    @Test
    void rebuildChunksDeletesOldChunksAndInsertsNewChunks() {
        when(docMapper.selectById(1L)).thenReturn(doc());
        when(chunkMapper.insert(any()))
                .thenAnswer(
                        invocation -> {
                            HerbAiKnowledgeChunk chunk = invocation.getArgument(0);
                            chunk.setId((long) chunk.getChunkIndex());
                            return 1;
                        });

        var result = service.rebuildChunks(1L);

        assertThat(result.getDocId()).isEqualTo(1L);
        assertThat(result.getChunkCount()).isGreaterThan(0);
        verify(chunkMapper).logicDeleteByDocId(eq(1L), any());
        verify(docMapper)
                .updateChunkCountAndEmbeddingStatus(
                        eq(1L), eq(result.getChunkCount()), eq("pending"), any());
        ArgumentCaptor<HerbAiKnowledgeChunk> captor =
                ArgumentCaptor.forClass(HerbAiKnowledgeChunk.class);
        verify(chunkMapper, atLeastOnce()).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(HerbAiKnowledgeChunk::getChunkIndex)
                .contains(1);
        assertThat(captor.getAllValues())
                .allSatisfy(chunk -> assertThat(chunk.getContentHash()).hasSize(64));
        assertThat(captor.getAllValues().get(0).getMetadataJson()).contains("\"docId\":1");
    }

    @Test
    void mockBuildEmbeddingMarksChunksAndDocCompletedWithoutVectorStore() {
        when(docMapper.selectById(1L)).thenReturn(doc());
        when(chunkMapper.selectEntitiesByDocId(1L)).thenReturn(List.of(chunk(11L)));

        var result = service.buildEmbedding(1L);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.isMockEmbedding()).isTrue();
        verify(vectorStoreProvider, never()).getIfAvailable();
        verify(chunkMapper).updateEmbeddingStatus(eq(11L), eq("completed"), eq("chunk-11"), any());
        verify(docMapper).updateEmbeddingStatus(eq(1L), eq("completed"), any());
    }

    @Test
    void buildPendingContinuesWhenOneDocumentFails() {
        HerbAiKnowledgeDoc ok = doc();
        HerbAiKnowledgeDoc missing = doc();
        missing.setId(2L);
        missing.setDocCode("DOC_002");
        when(docMapper.selectPendingEnabled()).thenReturn(List.of(ok, missing));
        when(chunkMapper.selectEntitiesByDocId(1L)).thenReturn(List.of(chunk(11L)));
        when(chunkMapper.selectEntitiesByDocId(2L)).thenThrow(new RuntimeException("boom"));

        var result = service.buildPending();

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void deleteEmbeddingResetsDatabaseStatusWhenVectorStoreUnavailable() {
        when(docMapper.selectById(1L)).thenReturn(doc());
        when(chunkMapper.selectAllEntitiesByDocId(1L)).thenReturn(List.of(chunk(11L)));
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);

        service.deleteEmbedding(1L);

        verify(chunkMapper).resetEmbeddingStatusByDocId(eq(1L), eq("pending"), any());
        verify(docMapper).updateEmbeddingStatus(eq(1L), eq("pending"), any());
    }

    @Test
    void deleteEmbeddingDeletesStoredVectorsIncludingDeletedChunks() {
        HerbAiKnowledgeChunk active = chunk(11L);
        active.setVectorId("chunk-11");
        HerbAiKnowledgeChunk deleted = chunk(12L);
        deleted.setVectorId("chunk-12");
        deleted.setDeleted(1);
        when(docMapper.selectById(1L)).thenReturn(doc());
        when(chunkMapper.selectAllEntitiesByDocId(1L)).thenReturn(List.of(active, deleted));
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);

        service.deleteEmbedding(1L);

        verify(vectorStore).delete(List.of("chunk-11", "chunk-12"));
        verify(chunkMapper).resetEmbeddingStatusByDocId(eq(1L), eq("pending"), any());
        verify(docMapper).updateEmbeddingStatus(eq(1L), eq("pending"), any());
    }

    @Test
    void realVectorStoreBuildWritesSearchableDocuments() {
        properties.setMockEmbeddingEnabled(false);
        SimpleVectorStore realVectorStore =
                SimpleVectorStore.builder(new FixedEmbeddingModel()).build();
        when(docMapper.selectById(1L)).thenReturn(doc());
        when(chunkMapper.selectEntitiesByDocId(1L)).thenReturn(List.of(chunk(11L)));
        when(vectorStoreProvider.getIfAvailable()).thenReturn(realVectorStore);

        var result = service.buildEmbedding(1L);

        assertThat(result.getSuccess()).isEqualTo(1);
        List<org.springframework.ai.document.Document> documents =
                realVectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("向量化")
                                .topK(1)
                                .similarityThreshold(0.0)
                                .build());
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getMetadata()).containsEntry("chunkId", 11L);
    }

    private HerbAiKnowledgeDoc doc() {
        HerbAiKnowledgeDoc doc = new HerbAiKnowledgeDoc();
        doc.setId(1L);
        doc.setDocCode("DOC_001");
        doc.setDocTitle("系统使用说明");
        doc.setDocType("system_guide");
        doc.setContentText("第一段内容用于切片。\n\n第二段内容继续用于切片，确保能够生成内容。");
        doc.setStatus("enabled");
        doc.setEmbeddingStatus("pending");
        doc.setChunkCount(1);
        doc.setDeleted(0);
        return doc;
    }

    private HerbAiKnowledgeChunk chunk(Long id) {
        HerbAiKnowledgeChunk chunk = new HerbAiKnowledgeChunk();
        chunk.setId(id);
        chunk.setDocId(1L);
        chunk.setChunkIndex(1);
        chunk.setChunkContent("第一段内容用于向量化。");
        chunk.setEmbeddingStatus("pending");
        return chunk;
    }

    private static class FixedEmbeddingModel
            implements org.springframework.ai.embedding.EmbeddingModel {

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(
                org.springframework.ai.embedding.EmbeddingRequest request) {
            List<org.springframework.ai.embedding.Embedding> embeddings =
                    request.getInstructions().stream()
                            .map(
                                    instruction ->
                                            new org.springframework.ai.embedding.Embedding(
                                                    vector(instruction), 0))
                            .toList();
            return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return vector(document == null ? "" : document.getText());
        }

        private static float[] vector(String text) {
            return new float[] {1.0f, text == null ? 0.0f : Math.min(text.length(), 10) / 10.0f};
        }
    }
}
