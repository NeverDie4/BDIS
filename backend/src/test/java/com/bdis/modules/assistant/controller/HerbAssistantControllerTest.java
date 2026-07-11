package com.bdis.modules.assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.assistant.dto.HerbAssistantChatResponse;
import com.bdis.modules.assistant.service.HerbAiChatHistoryService;
import com.bdis.modules.assistant.service.HerbAssistantService;
import com.bdis.modules.assistant.vo.HerbAssistantBatchExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainResponse;
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
class HerbAssistantControllerTest {

    private MockMvc mockMvc;

    @Mock private HerbAssistantService herbAssistantService;

    @Mock private HerbAiChatHistoryService historyService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HerbAssistantController(herbAssistantService, historyService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void chatReturnsUnifiedResult() throws Exception {
        when(herbAssistantService.chat(any()))
                .thenReturn(new HerbAssistantChatResponse("test-session-001", "模拟回答"));

        mockMvc.perform(
                        post("/herb/assistant/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"message\":\"这个系统怎么进行批次采集？\","
                                                + "\"sessionId\":\"test-session-001\","
                                                + "\"source\":\"mobile\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sessionId").value("test-session-001"))
                .andExpect(jsonPath("$.data.answer").value("模拟回答"));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(
                        post("/herb/assistant/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.message").value("消息不能为空"));
    }

    @Test
    void explainBatchReturnsUnifiedResult() throws Exception {
        when(herbAssistantService.explainBatch(any(), any()))
                .thenReturn(
                        new HerbAssistantBatchExplainResponse(
                                1L, "test-session-001", "该批次仍有 2 张图片需要复核。"));

        mockMvc.perform(
                        post("/herb/assistant/batch/1/explain")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"question\":\"这个批次为什么需要复核？\","
                                                + "\"sessionId\":\"test-session-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.batchId").value(1))
                .andExpect(jsonPath("$.data.sessionId").value("test-session-001"))
                .andExpect(jsonPath("$.data.answer").value("该批次仍有 2 张图片需要复核。"));
    }

    @Test
    void explainImageReturnsUnifiedResult() throws Exception {
        when(herbAssistantService.explainImage(any(), any()))
                .thenReturn(
                        new HerbAssistantImageExplainResponse(
                                12L, "test-session-001", "这张图片最终识别为黄连。"));

        mockMvc.perform(
                        post("/herb/assistant/image/12/explain")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"question\":\"这张图片为什么需要复核？\","
                                                + "\"sessionId\":\"test-session-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.imageId").value(12))
                .andExpect(jsonPath("$.data.sessionId").value("test-session-001"))
                .andExpect(jsonPath("$.data.answer").value("这张图片最终识别为黄连。"));
    }

    @Test
    void sessionsReturnsUnifiedPageResult() throws Exception {
        when(historyService.listSessions(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        mockMvc.perform(get("/herb/assistant/sessions").param("pageNum", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
