package com.bdis.modules.training.constant;

import java.util.Set;

public final class TrainingStatus {
    public static final String NOT_STARTED = "not_started";
    public static final String LEARNING = "learning";
    public static final String COMPLETED = "completed";
    public static final String FAILED = "failed";
    public static final String MAKEUP = "makeup";
    public static final Set<String> VALUES =
            Set.of(NOT_STARTED, LEARNING, COMPLETED, FAILED, MAKEUP);

    private TrainingStatus() {}
}
