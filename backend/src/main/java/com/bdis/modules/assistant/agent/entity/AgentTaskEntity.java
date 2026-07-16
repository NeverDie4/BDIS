package com.bdis.modules.assistant.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("assistant_agent_task")
public class AgentTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private String sessionId;
    private Long userId;
    private String goalType;
    private String goalText;
    private String targetType;
    private Long targetId;
    private Long collectionTaskId;
    private Long speciesId;
    private String status;
    private String currentPhase;
    private Integer progressPercent;
    private String contextJson;
    private String resultSummary;
    private String errorCode;
    private String errorMessage;

    @Version private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;

    @TableField("is_deleted")
    private Integer deleted;
}
