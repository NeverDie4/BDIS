package com.bdis.modules.assistant.agent.vo;

public record EvidenceGapVO(
        String findingType,
        String severity,
        Long targetId,
        String targetType,
        String description,
        String suggestion) {}
