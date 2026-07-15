package com.bdis.modules.growth.support;

import java.util.Arrays;

public enum HerbDigitalLifeImageType {
    LEAF("leaf", "叶片"),
    STEM("stem", "茎部"),
    ROOT("root", "根部"),
    FLOWER("flower", "花"),
    FRUIT("fruit", "果实"),
    WHOLE_PLANT("whole_plant", "整株"),
    MEDICINAL_PART("medicinal_part", "药用部位"),
    ENVIRONMENT("environment", "生长环境"),
    OTHER("other", "其他");

    private final String code;

    private final String displayName;

    HerbDigitalLifeImageType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static String displayNameOf(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(OTHER)
                .displayName;
    }
}
