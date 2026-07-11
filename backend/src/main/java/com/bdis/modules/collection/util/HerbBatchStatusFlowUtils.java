package com.bdis.modules.collection.util;

import com.bdis.modules.collection.constant.HerbBatchActionConstants;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HerbBatchStatusFlowUtils {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS =
            Map.of(
                    HerbBatchStatusConstants.DRAFT,
                    Set.of(HerbBatchStatusConstants.COLLECTING, HerbBatchStatusConstants.CANCELLED),
                    HerbBatchStatusConstants.COLLECTING,
                    Set.of(HerbBatchStatusConstants.SUBMITTED, HerbBatchStatusConstants.CANCELLED),
                    HerbBatchStatusConstants.SUBMITTED,
                    Set.of(
                            HerbBatchStatusConstants.IDENTIFYING,
                            HerbBatchStatusConstants.REVIEWING,
                            HerbBatchStatusConstants.CONFIRMED,
                            HerbBatchStatusConstants.CANCELLED),
                    HerbBatchStatusConstants.IDENTIFYING,
                    Set.of(
                            HerbBatchStatusConstants.REVIEWING,
                            HerbBatchStatusConstants.CONFIRMED,
                            HerbBatchStatusConstants.CANCELLED),
                    HerbBatchStatusConstants.REVIEWING,
                    Set.of(
                            HerbBatchStatusConstants.IDENTIFYING,
                            HerbBatchStatusConstants.CONFIRMED,
                            HerbBatchStatusConstants.CANCELLED),
                    HerbBatchStatusConstants.CONFIRMED,
                    Set.of(
                            HerbBatchStatusConstants.ARCHIVED,
                            HerbBatchStatusConstants.REVIEWING,
                            HerbBatchStatusConstants.CANCELLED),
                    HerbBatchStatusConstants.ARCHIVED,
                    Set.of(),
                    HerbBatchStatusConstants.CANCELLED,
                    Set.of());

    private HerbBatchStatusFlowUtils() {}

    public static boolean canTransition(String currentStatus, String targetStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public static List<String> getAllowedActions(HerbBatchEntity batch) {
        if (batch == null) {
            return List.of();
        }
        return switch (batch.getBatchStatus()) {
            case HerbBatchStatusConstants.DRAFT ->
                    List.of(
                            HerbBatchActionConstants.START_COLLECTION,
                            HerbBatchActionConstants.CANCEL);
            case HerbBatchStatusConstants.COLLECTING ->
                    List.of(HerbBatchActionConstants.SUBMIT, HerbBatchActionConstants.CANCEL);
            case HerbBatchStatusConstants.SUBMITTED ->
                    List.of(
                            HerbBatchActionConstants.START_IDENTIFICATION,
                            HerbBatchActionConstants.MARK_REVIEWING,
                            HerbBatchActionConstants.CONFIRM_STATUS,
                            HerbBatchActionConstants.CANCEL);
            case HerbBatchStatusConstants.IDENTIFYING ->
                    List.of(
                            HerbBatchActionConstants.MARK_REVIEWING,
                            HerbBatchActionConstants.CONFIRM_STATUS,
                            HerbBatchActionConstants.CANCEL);
            case HerbBatchStatusConstants.REVIEWING ->
                    List.of(
                            HerbBatchActionConstants.START_IDENTIFICATION,
                            HerbBatchActionConstants.CONFIRM_STATUS,
                            HerbBatchActionConstants.CANCEL);
            case HerbBatchStatusConstants.CONFIRMED ->
                    List.of(
                            HerbBatchActionConstants.ARCHIVE,
                            HerbBatchActionConstants.REOPEN_REVIEW,
                            HerbBatchActionConstants.CANCEL);
            default -> List.of();
        };
    }
}
