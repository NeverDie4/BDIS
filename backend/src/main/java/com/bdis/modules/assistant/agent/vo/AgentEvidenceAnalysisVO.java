package com.bdis.modules.assistant.agent.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AgentEvidenceAnalysisVO(
        Long agentTaskId,
        Long collectionTaskId,
        Integer stageCount,
        List<StageEvidenceSummaryVO> stages,
        List<MetricTrendVO> metricTrends,
        List<CrossModalFindingVO> findings,
        List<EvidenceGapVO> evidenceGaps,
        AgentEvidenceExplanation explanation,
        LocalDateTime generatedTime) {}
