package com.bdis.modules.map;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MapCoverMigrationTest {

    @Test
    void migrationPublishesOnlyConfirmedMapCoverBindings() throws IOException {
        Path migration =
                Path.of(
                        "src/main/resources/db/migration/"
                                + "V20260713_002__make_bound_map_cover_images_public.sql");
        String sql = Files.readString(migration);

        assertThat(sql).contains("sys_file_business");
        assertThat(sql).contains("binding.`biz_type` = 'map_point'");
        assertThat(sql).contains("binding.`file_usage` = 'cover'");
        assertThat(sql).contains("file_resource.`file_type` = 'image'");
        assertThat(sql).doesNotContain("cover_image_url` LIKE");
    }
}
