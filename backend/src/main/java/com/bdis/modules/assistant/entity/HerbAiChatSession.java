package com.bdis.modules.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_ai_chat_session")
public class HerbAiChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private String sessionTitle;
    private Long userId;
    private String source;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
