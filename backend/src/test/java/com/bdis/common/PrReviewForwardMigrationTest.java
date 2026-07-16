package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PrReviewForwardMigrationTest {
    @Test
    void splitsSubmitReviewPermissionWithoutChangingReviewPermission() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/V20260716_005__split_project_submit_review_permission.sql"));
        assertThat(sql).contains("research:project:submit-review", "RESEARCHER", "ADMIN");
    }

    @Test
    void repeatAttendanceMigrationDropsLegacyPlanUserUniqueness() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/V20260716_006__allow_repeat_training_attendance.sql"));
        assertThat(sql)
                .contains("uk_edu_training_record_plan_user")
                .containsIgnoringCase("DROP INDEX")
                .containsIgnoringCase("information_schema.statistics");
    }

    @Test
    void trainingRecordFilePolicyIsRegistered() {
        Throwable error =
                org.assertj.core.api.Assertions.catchThrowable(
                        () ->
                                Class.forName(
                                        "com.bdis.modules.training.file.TrainingRecordFileBusinessAccessPolicy"));
        assertThat(error).isNull();
    }
}
