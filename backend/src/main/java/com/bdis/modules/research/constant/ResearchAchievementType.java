package com.bdis.modules.research.constant;

import java.util.Set;

public final class ResearchAchievementType {
    public static final String PAPER = "paper";
    public static final String PATENT = "patent";
    public static final String REPORT = "report";
    public static final String EXPERIMENT_RESULT = "experiment_result";
    public static final Set<String> ALL = Set.of(PAPER, PATENT, REPORT, EXPERIMENT_RESULT);

    private ResearchAchievementType() {}
}
