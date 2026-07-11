package com.bdis.modules.assistant.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocCreateRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocUpdateRequest;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeDoc;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeChunkMapper;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeDocMapper;
import com.bdis.modules.assistant.knowledge.service.impl.HerbAiKnowledgeDocServiceImpl;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeDocVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbAiKnowledgeDocServiceImplTest {

    @Mock private HerbAiKnowledgeDocMapper docMapper;
    @Mock private HerbAiKnowledgeChunkMapper chunkMapper;
    @Mock private HerbAiKnowledgeEmbeddingService embeddingService;

    private HerbAiKnowledgeDocService service;

    @BeforeEach
    void setUp() {
        service = new HerbAiKnowledgeDocServiceImpl(docMapper, chunkMapper, embeddingService);
    }

    @Test
    void createUsesRequiredDefaultsAndGeneratedCode() {
        HerbAiKnowledgeDocCreateRequest request = new HerbAiKnowledgeDocCreateRequest();
        request.setDocTitle("中药材采集规范");
        request.setContentText("采集时应拍摄完整植株和药用部位。");
        when(docMapper.insert(any()))
                .thenAnswer(
                        invocation -> {
                            HerbAiKnowledgeDoc doc = invocation.getArgument(0);
                            doc.setId(1L);
                            return 1;
                        });
        when(docMapper.selectDetailById(1L)).thenReturn(docVO());

        HerbAiKnowledgeDocVO result = service.create(request);

        ArgumentCaptor<HerbAiKnowledgeDoc> captor =
                ArgumentCaptor.forClass(HerbAiKnowledgeDoc.class);
        verify(docMapper).insert(captor.capture());
        HerbAiKnowledgeDoc inserted = captor.getValue();
        assertThat(inserted.getDocCode()).startsWith("DOC_");
        assertThat(inserted.getDocType()).isEqualTo("other");
        assertThat(inserted.getSourceType()).isEqualTo("manual");
        assertThat(inserted.getStatus()).isEqualTo("enabled");
        assertThat(inserted.getEmbeddingStatus()).isEqualTo("pending");
        assertThat(inserted.getChunkCount()).isZero();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void updateResetsEmbeddingStatusWhenContentChanges() {
        HerbAiKnowledgeDoc existing = docEntity();
        existing.setEmbeddingStatus("completed");
        when(docMapper.selectById(1L)).thenReturn(existing);
        when(docMapper.updateById(any())).thenReturn(1);
        when(docMapper.selectDetailById(1L)).thenReturn(docVO());
        HerbAiKnowledgeDocUpdateRequest request = new HerbAiKnowledgeDocUpdateRequest();
        request.setContentText("修改后的内容");

        service.update(1L, request);

        ArgumentCaptor<HerbAiKnowledgeDoc> captor =
                ArgumentCaptor.forClass(HerbAiKnowledgeDoc.class);
        verify(docMapper).updateById(captor.capture());
        verify(embeddingService).deleteEmbedding(1L);
        assertThat(captor.getValue().getContentText()).isEqualTo("修改后的内容");
        assertThat(captor.getValue().getEmbeddingStatus()).isEqualTo("pending");
    }

    @Test
    void deleteLogicallyDeletesChunksAndDocument() {
        when(docMapper.selectById(1L)).thenReturn(docEntity());
        when(docMapper.logicDeleteById(eq(1L), any())).thenReturn(1);

        service.delete(1L);

        verify(embeddingService).deleteEmbedding(1L);
        verify(chunkMapper).logicDeleteByDocId(eq(1L), any());
        verify(docMapper).logicDeleteById(eq(1L), any());
    }

    @Test
    void pageValidatesFiltersAndReturnsRecords() {
        HerbAiKnowledgeDocQueryRequest query = new HerbAiKnowledgeDocQueryRequest();
        query.setDocType("rule");
        query.setStatus("enabled");
        query.setEmbeddingStatus("pending");
        when(docMapper.countPage(query)).thenReturn(1L);
        when(docMapper.selectPage(query, 0L, 10)).thenReturn(List.of(docVO()));

        var result = service.page(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    void createRejectsBlankContent() {
        HerbAiKnowledgeDocCreateRequest request = new HerbAiKnowledgeDocCreateRequest();
        request.setDocTitle("FAQ");
        request.setContentText(" ");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文档内容不能为空");
    }

    private HerbAiKnowledgeDoc docEntity() {
        HerbAiKnowledgeDoc doc = new HerbAiKnowledgeDoc();
        doc.setId(1L);
        doc.setDocCode("DOC_001");
        doc.setDocTitle("中药材采集规范");
        doc.setDocType("rule");
        doc.setSourceType("manual");
        doc.setContentText("原始内容");
        doc.setStatus("enabled");
        doc.setEmbeddingStatus("pending");
        doc.setChunkCount(0);
        doc.setDeleted(0);
        return doc;
    }

    private HerbAiKnowledgeDocVO docVO() {
        HerbAiKnowledgeDocVO vo = new HerbAiKnowledgeDocVO();
        vo.setId(1L);
        vo.setDocCode("DOC_001");
        vo.setDocTitle("中药材采集规范");
        vo.setDocType("rule");
        vo.setStatus("enabled");
        vo.setEmbeddingStatus("pending");
        vo.setChunkCount(0);
        return vo;
    }
}
