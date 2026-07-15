package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TeachingResearchPermissionMigrationTest {

    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/V20260715_032__grant_teaching_research_scope_permissions.sql");

    @Test
    void grantsCurrentUserTeachingAndResearchReadPermissions() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertThat(sql)
                .contains("edu:course:enrollment:list")
                .contains("research:project:list")
                .contains("research:project:detail")
                .contains("r.role_code = 'STUDENT'")
                .contains("r.role_code = 'TEACHER'");
    }
}
