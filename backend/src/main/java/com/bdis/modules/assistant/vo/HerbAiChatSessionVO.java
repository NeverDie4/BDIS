package com.bdis.modules.assistant.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbAiChatSessionVO {

    private String sessionId;
    private String sessionTitle;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String source;
    private LocalDateTime createTime;
}
