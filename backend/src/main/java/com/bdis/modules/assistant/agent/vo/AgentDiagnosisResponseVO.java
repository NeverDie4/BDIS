package com.bdis.modules.assistant.agent.vo;

public record AgentDiagnosisResponseVO(
        Boolean available, String status, String message, AgentCompletenessReportVO report) {}
