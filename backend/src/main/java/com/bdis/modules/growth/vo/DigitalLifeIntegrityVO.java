package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;

public record DigitalLifeIntegrityVO(
        boolean verified,
        int eventCount,
        String rootHash,
        String hashVersion,
        LocalDateTime generatedTime,
        Integer failedSequence,
        String failedEventType,
        String message) {}
