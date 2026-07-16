package com.bdis.modules.collection.constant;

import java.util.Set;

public final class HerbBatchImageRoleConstants {

    public static final String LEAF = "leaf";

    public static final String ROOT = "root";

    public static final String STEM = "stem";

    public static final String FLOWER = "flower";

    public static final String FRUIT = "fruit";

    public static final String WHOLE_PLANT = "whole_plant";

    public static final String MEDICINAL_PART = "medicinal_part";

    public static final String ENVIRONMENT = "environment";

    public static final String OTHER = "other";

    public static final Set<String> VALID_ROLES =
            Set.of(
                    LEAF,
                    ROOT,
                    STEM,
                    FLOWER,
                    FRUIT,
                    WHOLE_PLANT,
                    MEDICINAL_PART,
                    ENVIRONMENT,
                    OTHER);

    private HerbBatchImageRoleConstants() {}
}
