package com.bdis.modules.assistant.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.assistant.dto.HerbAssistantRagChatRequest;
import com.bdis.modules.assistant.service.HerbAssistantRagService;
import com.bdis.modules.assistant.vo.HerbAssistantRagChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/assistant/rag")
public class HerbAssistantRagController {

    private final HerbAssistantRagService ragService;

    public HerbAssistantRagController(HerbAssistantRagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/chat")
    public Result<HerbAssistantRagChatResponse> chat(
            @Valid @RequestBody HerbAssistantRagChatRequest request) {
        return Result.success(ragService.chat(request));
    }
}
