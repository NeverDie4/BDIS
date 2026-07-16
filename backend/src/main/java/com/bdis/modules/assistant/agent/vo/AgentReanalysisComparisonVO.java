package com.bdis.modules.assistant.agent.vo;

import com.bdis.modules.assistant.agent.dto.AgentReanalysisSnapshot.MetricChange;
import java.util.List;
import java.util.Map;

public record AgentReanalysisComparisonVO(
    Long analysisRoundId,
    Integer roundNo,
    List<AgentFindingVO> resolvedFindings,
    List<AgentFindingVO> remainingFindings,
    List<AgentFindingVO> newFindings,
    List<MetricChange> metricChanges,
    Map<String, Integer> imageEvidenceChanges,
    Map<String, Integer> recognitionChanges,
    Integer completenessScoreBefore,
    Integer completenessScoreAfter,
    String archiveReadinessBefore,
    String archiveReadinessAfter,
    String conclusion,
    String outcome) {}
