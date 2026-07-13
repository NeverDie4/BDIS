package com.bdis.modules.map;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MapCoverMigrationTest {

    @Test
    void migrationRepairsOnlyUniqueOwnedMapCoverCandidates() throws IOException {
        Path migration =
                Path.of(
                        "src/main/resources/db/migration/"
                                + "V20260714_001__repair_and_publish_map_cover_images.sql");
        String sql = Files.readString(migration);

        assertThat(sql).contains("sys_file_business");
        assertThat(sql).contains("binding.`biz_type` = 'map_point'");
        assertThat(sql).contains("binding.`file_usage` = 'cover'");
        assertThat(sql).contains("distribution.`created_by` = file_resource.`uploader_id`");
        assertThat(sql).contains("HAVING COUNT(*) = 1");
        assertThat(sql).contains("map_cover_migration_review");
        assertThat(sql).contains("AMBIGUOUS_CANDIDATES");
        assertThat(sql).contains("FILE_ALREADY_BOUND");
        assertThat(sql).doesNotContain("cover_image_url` LIKE");
    }
}
