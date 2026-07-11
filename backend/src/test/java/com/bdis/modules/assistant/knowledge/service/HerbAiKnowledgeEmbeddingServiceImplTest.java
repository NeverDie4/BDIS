package com.bdis.modules.assistant.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bdis.modules.assistant.knowledge.config.HerbAssistantRagProperties;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeChunk;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeDoc;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeChunkMapper;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeDocMapper;
import com.bdis.modules.assistant.knowledge.service.impl.HerbAiKnowledgeEmbeddingServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class HerbAiKnowledgeEmbeddingServiceImplTest {

    @Mock private HerbAiKnowledgeDocMapper docMapper;
    @Mock private HerbAiKnowledgeChunkMapper chunkMapper;
    @Mock private ObjectProvider<SimpleVectorStore> vectorStoreProvider;

    private HerbAssistantRagProperties properties;
    private HerbAiKnowledgeEmbeddingService service;

    @BeforeEach
    void setUp() {
        properties = new HerbAssistantRagProperties();
        properties.setChunkSize(20);
        properties.setChunkOverlap(5);
        properties.setMockEmbeddingEnabled(true);
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
        assertThat(captor.getAllValues()).extracting(HerbAiKnowledgeChunk::getChunkIndex).contains(1);
        assertThat(captor.getAllValues()).allSatisfy(chunk -> assertThat(chunk.getContentHash()).hasSize(64));
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
        when(chunkMapper.selectEntitiesByDocId(1L)).thenReturn(List.of(chunk(11L)));
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);

        service.deleteEmbedding(1L);

        verify(chunkMapper).resetEmbeddingStatusByDocId(eq(1L), eq("pending"), any());
        verify(docMapper).updateEmbeddingStatus(eq(1L), eq("pending"), any());
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
}
