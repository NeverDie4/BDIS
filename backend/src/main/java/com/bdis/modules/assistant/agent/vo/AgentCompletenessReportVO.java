package com.bdis.modules.assistant.agent.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentCompletenessReportVO(
        Long agentTaskId,
        Long collectionTaskId,
        String collectionTaskName,
        Integer stageCount,
        Integer validStageCount,
        Integer completenessScore,
        String archiveReadiness,
        Map<String, Integer> scoreBreakdown,
        Map<String, List<String>> scoreDeductions,
        List<AgentFindingVO> blockingFindings,
        List<AgentFindingVO> warningFindings,
        List<String> nextSuggestions,
        String summary,
        LocalDateTime generatedTime) {}
