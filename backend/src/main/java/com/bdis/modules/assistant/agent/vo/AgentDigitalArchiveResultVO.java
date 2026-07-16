package com.bdis.modules.assistant.agent.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AgentDigitalArchiveResultVO(
    Long agentTaskId,
    Long archiveId,
    String archiveNo,
    String traceCode,
    String publicUrl,
    String qrCodeUrl,
    int stageCount,
    int imageCount,
    int completenessScore,
    boolean integrityVerified,
    String rootHashShort,
    String hashVersion,
    boolean publicVisible,
    LocalDateTime generatedTime,
    List<String> completedCapabilities,
    List<String> limitations) {}
