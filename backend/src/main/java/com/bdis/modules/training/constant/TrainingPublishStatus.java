package com.bdis.modules.training.constant;

import java.util.Set;

public final class TrainingPublishStatus {
    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";
    public static final String CLOSED = "closed";
    public static final Set<String> VALUES = Set.of(DRAFT, PUBLISHED, CLOSED);

    private TrainingPublishStatus() {}
}
