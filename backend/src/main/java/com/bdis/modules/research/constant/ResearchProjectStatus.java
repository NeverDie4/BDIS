package com.bdis.modules.research.constant;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

public final class ResearchProjectStatus {
    public static final String PLANNING = "planning";
    public static final String ONGOING = "ongoing";
    public static final String SUSPENDED = "suspended";
    public static final String COMPLETED = "completed";
    private static final Set<String> STATUSES = Set.of(PLANNING, ONGOING, SUSPENDED, COMPLETED);
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PLANNING, Set.of(ONGOING), ONGOING, Set.of(SUSPENDED, COMPLETED),
            SUSPENDED, Set.of(ONGOING), COMPLETED, Set.of());

    private ResearchProjectStatus() {}

    public static void assertMutable(ResearchProjectEntity project) {
        if (COMPLETED.equals(project.getProjectStatus())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "completed project cannot be modified");
        }
    }

    public static void validateTransition(String currentStatus, String targetStatus) {
        if (!STATUSES.contains(targetStatus)) {
            throw new BusinessException("Invalid target project status");
        }
        if (Objects.equals(currentStatus, targetStatus)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project status is already " + targetStatus);
        }
        if (!TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Invalid project status transition");
        }
    }
}
