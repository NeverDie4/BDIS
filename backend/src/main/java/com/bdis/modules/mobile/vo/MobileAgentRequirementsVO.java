package com.bdis.modules.mobile.vo;

import java.util.List;

public record MobileAgentRequirementsVO(
        Boolean agentGenerated,
        String objective,
        List<RequirementItem> requiredMetrics,
        List<RequirementItem> requiredImages,
        List<String> completionCriteria) {

    public record RequirementItem(
            String code,
            String name,
            Boolean required,
            Integer minCount,
            String unit,
            String guidance,
            String reason) {}
}
