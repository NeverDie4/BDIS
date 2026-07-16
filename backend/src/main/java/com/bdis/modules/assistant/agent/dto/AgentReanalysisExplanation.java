package com.bdis.modules.assistant.agent.dto;

import java.util.List;

public record AgentReanalysisExplanation(
    String changeSummary,
    List<String> confirmedFacts,
    List<String> uncertainties,
    String nextSuggestion) {}
