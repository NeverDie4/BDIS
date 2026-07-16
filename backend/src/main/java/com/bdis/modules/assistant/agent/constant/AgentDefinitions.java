package com.bdis.modules.assistant.agent.constant;

import java.util.Set;

public final class AgentDefinitions {

    public static final String DEFAULT_GOAL_TYPE = "DIGITAL_TWIN_RESEARCH";
    public static final String DEFAULT_TARGET_TYPE = "COLLECTION_TASK";
    public static final String INITIAL_STEP_TYPE = "LOAD_CONTEXT";
    public static final String INITIAL_STEP_NAME = "装载任务上下文";

    public static final Set<String> GOAL_TYPES =
            Set.of(
                    DEFAULT_GOAL_TYPE,
                    "TASK_COMPLETENESS_CHECK",
                    "EVIDENCE_GAP_ANALYSIS",
                    "FOLLOW_UP_COLLECTION_PLAN",
                    "DIGITAL_ARCHIVE_PREPARATION");

    public static final Set<String> STEP_TYPES =
            Set.of(
                    INITIAL_STEP_TYPE,
                    "INSPECT_TASK",
                    "ANALYZE_TIMELINE",
                    "ANALYZE_EVIDENCE",
                    "GENERATE_COLLECTION_PLAN",
                    "WAIT_FOR_CONFIRMATION",
                    "CREATE_COLLECTION_TASK",
                    "WAIT_FOR_FIELD_DATA",
                    "REANALYZE",
                    "PREPARE_DIGITAL_ARCHIVE",
                    "GENERATE_STAGE_NARRATIONS",
                    "GENERATE_ARCHIVE_SUMMARY",
                    "GENERATE_ARCHIVE_HASH",
                    "VERIFY_ARCHIVE",
                    "GENERATE_TRACE_QR",
                    "WAIT_PUBLIC_CONFIRMATION",
                    "COMPLETE_REPORT");

    public static final Set<String> FINDING_TYPES =
            Set.of(
                    "MISSING_GROWTH_RECORD",
                    "MISSING_IMAGE",
                    "MISSING_IMAGE_TYPE",
                    "UNRECOGNIZED_IMAGE",
                    "LOW_CONFIDENCE_RECOGNITION",
                    "MISSING_METRIC",
                    "ABNORMAL_TIMELINE",
                    "METRIC_TREND_CHANGE",
                    "LOCATION_MISSING",
                    "REVIEW_PENDING",
                    "REVIEW_REJECTED",
                    "NARRATION_OUTDATED",
                    "ARCHIVE_NOT_READY",
                    "HASH_VERIFY_FAILED",
                    "SUSPECTED_TEST_DATA",
                    "FILE_RECORD_MISMATCH",
                    "INSUFFICIENT_STAGE_COUNT",
                    "MISSING_FOLLOW_UP_STAGE",
                    "MISSING_ROOT_IMAGE",
                    "MISSING_LEAF_IMAGE",
                    "MISSING_WHOLE_PLANT_IMAGE",
                    "MISSING_ENVIRONMENT_IMAGE",
                    "IMAGE_QUALITY_LOW",
                    "RECOGNITION_AMBIGUOUS",
                    "METRIC_OUTLIER",
                    "STAGE_SEQUENCE_SUSPECTED",
                    "LOCATION_CHANGE_SUSPECTED",
                    "INSUFFICIENT_EVIDENCE");

    public static final Set<String> ACTION_TYPES =
            Set.of(
                    "REFRESH_BATCH_SUMMARY",
                    "GENERATE_STAGE_NARRATION",
                    "VERIFY_ARCHIVE_HASH",
                    "GENERATE_ANALYSIS_REPORT",
                    "CREATE_FOLLOW_UP_COLLECTION_TASK",
                    "SUBMIT_GROWTH_RECORD",
                    "APPROVE_GROWTH_RECORD",
                    "REJECT_GROWTH_RECORD",
                    "GENERATE_TRACE_QR_CODE",
                    "ENABLE_PUBLIC_TRACE",
                    "REGENERATE_ARCHIVE_HASH",
                    "UPDATE_BUSINESS_DATA",
                    "DELETE_BUSINESS_DATA");

    public static final Set<String> STEP_STATUSES =
            Set.of("PENDING", "RUNNING", "WAITING", "SUCCEEDED", "FAILED", "SKIPPED", "CANCELLED");
    public static final Set<String> FINDING_STATUSES =
            Set.of("OPEN", "ACKNOWLEDGED", "RESOLVED", "IGNORED");
    public static final Set<String> ACTION_STATUSES =
            Set.of(
                    "PROPOSED",
                    "WAITING_CONFIRMATION",
                    "CONFIRMED",
                    "REJECTED",
                    "EXECUTING",
                    "SUCCEEDED",
                    "FAILED",
                    "CANCELLED");

    private AgentDefinitions() {}
}
