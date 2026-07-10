package com.bdis.modules.collection.constant;

import java.util.Set;

public final class HerbQualityLevelConstants {

    public static final String EXCELLENT = "excellent";

    public static final String GOOD = "good";

    public static final String NORMAL = "normal";

    public static final String POOR = "poor";

    public static final String UNKNOWN = "unknown";

    public static final Set<String> VALID_LEVELS =
            Set.of(EXCELLENT, GOOD, NORMAL, POOR, UNKNOWN);

    private HerbQualityLevelConstants() {}
}
