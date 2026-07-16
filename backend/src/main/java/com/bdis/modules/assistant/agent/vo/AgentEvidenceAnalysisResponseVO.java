package com.bdis.modules.assistant.agent.vo;

public record AgentEvidenceAnalysisResponseVO(
        Boolean available, String status, String message, AgentEvidenceAnalysisVO analysis) {}
