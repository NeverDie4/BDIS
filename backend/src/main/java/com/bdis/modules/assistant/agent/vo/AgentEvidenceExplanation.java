package com.bdis.modules.assistant.agent.vo;

import java.util.List;

public record AgentEvidenceExplanation(
        String conclusion,
        List<String> confirmedFacts,
        List<String> possibleAssociations,
        List<String> uncertainties,
        List<String> evidenceGaps,
        List<String> followUpSuggestions,
        String confidenceLevel) {}
