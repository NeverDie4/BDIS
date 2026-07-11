package com.bdis.modules.assistant.knowledge.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeDocService;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeEmbeddingService;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeDocVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeChunkRebuildResultVO;
import com.bdis.modules.assistant.knowledge.vo.HerbKnowledgeEmbeddingBuildResultVO;
import com.bdis.modules.permission.service.AuthorizationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HerbAiKnowledgeDocControllerTest {

    @Mock private HerbAiKnowledgeDocService service;
    @Mock private HerbAiKnowledgeEmbeddingService embeddingService;
    @Mock private AuthorizationService authorizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbAiKnowledgeDocController(
                                        service, embeddingService, authorizationService),
                                new HerbAiKnowledgeEmbeddingController(
                                        embeddingService, authorizationService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void createReturnsDocumentDetail() throws Exception {
        when(service.create(any())).thenReturn(docVO());

        mockMvc.perform(
                        post("/herb/assistant/knowledge/docs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"docTitle\":\"中药材采集规范\","
                                                + "\"docType\":\"rule\","
                                                + "\"contentText\":\"采集时应拍摄药用部位。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.docCode").value("DOC_001"));
        verify(authorizationService).requirePermission("herb:assistant:knowledge:manage");
    }

    @Test
    void createRejectsBlankContent() throws Exception {
        mockMvc.perform(
                        post("/herb/assistant/knowledge/docs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"docTitle\":\"FAQ\",\"contentText\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateReturnsDocumentDetail() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(docVO());

        mockMvc.perform(
                        put("/herb/assistant/knowledge/docs/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"summary\":\"更新后的摘要\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void pageReturnsUnifiedPageResult() throws Exception {
        when(service.page(any())).thenReturn(new PageResult<>(List.of(docVO()), 1, 10, 1));

        mockMvc.perform(get("/herb/assistant/knowledge/docs/page").param("docType", "rule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].docType").value("rule"));
        verify(authorizationService).requirePermission("herb:assistant:knowledge:view");
    }

    @Test
    void enableDisableDeleteAndChunksRoutesAreAvailable() throws Exception {
        when(service.enable(1L)).thenReturn(docVO());
        when(service.disable(1L)).thenReturn(docVO());
        when(service.listChunks(eq(1L), any())).thenReturn(List.of());
        when(embeddingService.rebuildChunks(1L))
                .thenReturn(new HerbKnowledgeChunkRebuildResultVO(1L, "DOC_001", 2));
        when(embeddingService.buildEmbedding(1L))
                .thenReturn(new HerbKnowledgeEmbeddingBuildResultVO(1, 1, 0, true, List.of()));
        when(embeddingService.buildPending())
                .thenReturn(new HerbKnowledgeEmbeddingBuildResultVO(1, 1, 0, true, List.of()));

        mockMvc.perform(put("/herb/assistant/knowledge/docs/1/enable")).andExpect(status().isOk());
        mockMvc.perform(put("/herb/assistant/knowledge/docs/1/disable")).andExpect(status().isOk());
        mockMvc.perform(get("/herb/assistant/knowledge/docs/1/chunks")).andExpect(status().isOk());
        mockMvc.perform(post("/herb/assistant/knowledge/docs/1/chunks/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkCount").value(2));
        mockMvc.perform(post("/herb/assistant/knowledge/docs/1/embedding/build"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(1));
        mockMvc.perform(post("/herb/assistant/knowledge/embedding/build-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(delete("/herb/assistant/knowledge/docs/1/embedding"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/herb/assistant/knowledge/docs/1")).andExpect(status().isOk());
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
