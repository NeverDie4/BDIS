package com.bdis.modules.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PerformanceRolePermissionMigrationTest {

    @Test
    void grantsTeachersPerformanceWorkspaceReadAccessOnly() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/V20260716_016__grant_teacher_performance_record_view.sql"));

        assertThat(sql)
                .contains("role.`role_code` = 'TEACHER'", "'performance:record:view'")
                .doesNotContain("performance:record:create")
                .doesNotContain("performance:record:update")
                .doesNotContain("performance:record:submit")
                .doesNotContain("performance:record:audit");
    }
}
