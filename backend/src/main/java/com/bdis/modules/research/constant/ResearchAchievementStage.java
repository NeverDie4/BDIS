package com.bdis.modules.research.constant;

import java.util.Set;

public final class ResearchAchievementStage {
    public static final String INITIAL = "initial";
    public static final String MIDDLE = "middle";
    public static final String FINAL = "final";
    public static final Set<String> ALL = Set.of(INITIAL, MIDDLE, FINAL);

    private ResearchAchievementStage() {}
}
