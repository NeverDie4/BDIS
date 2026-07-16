package com.bdis.modules.assistant.agent.vo;

import java.util.List;

public record CrossModalFindingVO(
        String findingType,
        String confidenceLevel,
        List<Long> stageIds,
        List<String> confirmedFacts,
        List<String> relatedEvidence,
        String association,
        String uncertainty) {}
