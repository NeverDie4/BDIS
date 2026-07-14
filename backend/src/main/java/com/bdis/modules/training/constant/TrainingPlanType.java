package com.bdis.modules.training.constant;

import java.util.Set;

public final class TrainingPlanType {
    public static final String COURSE = "course";
    public static final String SEMINAR = "seminar";
    public static final String PRACTICE = "practice";
    public static final String ONLINE = "online";
    public static final Set<String> VALUES = Set.of(COURSE, SEMINAR, PRACTICE, ONLINE);

    private TrainingPlanType() {}
}
