package com.bdis.modules.herb.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HerbSpeciesCoverMigrationTest {

    @Test
    void migrationRepairsOnlyVerifiedHerbSpeciesCoverCandidates() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src",
                                "main",
                                "resources",
                                "db",
                                "migration",
                                "V20260715_008__repair_and_publish_herb_species_cover_images.sql"));

        assertThat(sql).contains("herb_species_cover_migration_review");
        assertThat(sql).contains("species.`cover_image_url` = CONCAT('/api/files/', file_resource.`id`, '/content')");
        assertThat(sql).contains("COALESCE(species.`created_by`, file_resource.`uploader_id`)");
        assertThat(sql).contains("binding.`biz_type` = 'herb_species'");
        assertThat(sql).contains("binding.`file_usage` = 'cover'");
        assertThat(sql).contains("file_resource.`access_level` = 'public'");
        assertThat(sql)
                .contains("species.`cover_image_url` = CONCAT(")
                .contains("'/api/public-files/', file_resource.`id`, '/content'");
        assertThat(sql).doesNotContain("cover_image_url` LIKE");
    }
}
