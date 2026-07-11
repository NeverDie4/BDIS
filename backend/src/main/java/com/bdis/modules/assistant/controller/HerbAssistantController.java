package com.bdis.modules.assistant.controller;

import com.bdis.common.core.Result;
import com.bdis.common.core.PageResult;
import com.bdis.modules.assistant.dto.HerbAiChatHistoryQueryRequest;
import com.bdis.modules.assistant.dto.HerbAssistantBatchExplainRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatResponse;
import com.bdis.modules.assistant.dto.HerbAssistantImageExplainRequest;
import com.bdis.modules.assistant.service.HerbAiChatHistoryService;
import com.bdis.modules.assistant.service.HerbAssistantService;
import com.bdis.modules.assistant.vo.HerbAssistantBatchExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainResponse;
import com.bdis.modules.assistant.vo.HerbAiChatMessageVO;
import com.bdis.modules.assistant.vo.HerbAiChatSessionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/assistant")
public class HerbAssistantController {

    private final HerbAssistantService herbAssistantService;
    private final HerbAiChatHistoryService historyService;

    public HerbAssistantController(
            HerbAssistantService herbAssistantService, HerbAiChatHistoryService historyService) {
        this.herbAssistantService = herbAssistantService;
        this.historyService = historyService;
    }

    @PostMapping("/chat")
    public Result<HerbAssistantChatResponse> chat(
            @Valid @RequestBody HerbAssistantChatRequest request) {
        return Result.success(herbAssistantService.chat(request));
    }

    @PostMapping("/batch/{batchId}/explain")
    public Result<HerbAssistantBatchExplainResponse> explainBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody HerbAssistantBatchExplainRequest request) {
        return Result.success(herbAssistantService.explainBatch(batchId, request));
    }

    @PostMapping("/image/{imageId}/explain")
    public Result<HerbAssistantImageExplainResponse> explainImage(
            @PathVariable Long imageId,
            @Valid @RequestBody HerbAssistantImageExplainRequest request) {
        return Result.success(herbAssistantService.explainImage(imageId, request));
    }

    @GetMapping("/sessions")
    public Result<PageResult<HerbAiChatSessionVO>> sessions(
            @Valid @ModelAttribute HerbAiChatHistoryQueryRequest request) {
        return Result.success(historyService.listSessions(request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<HerbAiChatMessageVO>> messages(@PathVariable String sessionId) {
        return Result.success(historyService.listMessages(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        historyService.deleteSession(sessionId);
        return Result.success();
    }
}
