package com.bdis.modules.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_ai_chat_message")
public class HerbAiChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private Long userId;
    private String role;
    private String content;
    private String modelName;
    private Integer tokenCount;
    private String source;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    private Integer deleted;
}
