package com.bdis.modules.research.constant;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import java.util.Map;
import java.util.Set;

public final class ResearchAchievementStatus {
    public static final String DRAFT = "draft";
    public static final String SUBMITTED = "submitted";
    public static final String CONFIRMED = "confirmed";
    public static final Set<String> ALL = Set.of(DRAFT, SUBMITTED, CONFIRMED);
    private static final Map<String, Set<String>> TRANSITIONS =
            Map.of(
                    DRAFT, Set.of(DRAFT, SUBMITTED),
                    SUBMITTED, Set.of(SUBMITTED, CONFIRMED),
                    CONFIRMED, Set.of());

    private ResearchAchievementStatus() {}

    public static void validate(String status) {
        if (!ALL.contains(status)) {
            throw new BusinessException("Invalid achievement status");
        }
    }

    public static void validateTransition(String current, String target) {
        validate(target);
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Invalid achievement status transition");
        }
    }

    public static boolean isConfirmed(String status) {
        return CONFIRMED.equals(status);
    }
}
