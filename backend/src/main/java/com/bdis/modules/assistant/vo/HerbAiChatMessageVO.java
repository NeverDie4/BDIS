package com.bdis.modules.assistant.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbAiChatMessageVO {

    private String role;
    private String content;
    private String modelName;
    private LocalDateTime createTime;
}
