package com.bdis.modules.training.constant;

import java.util.Set;

public final class TrainingMaterialSourceType {
    public static final String UPLOAD = "upload";
    public static final String COURSE_RESOURCE = "course_resource";
    public static final String GENERATED = "generated";
    public static final Set<String> VALUES = Set.of(UPLOAD, COURSE_RESOURCE, GENERATED);
    private TrainingMaterialSourceType() {}
}
