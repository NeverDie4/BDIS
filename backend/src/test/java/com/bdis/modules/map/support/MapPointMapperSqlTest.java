package com.bdis.modules.map.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MapPointMapperSqlTest {

    @Test
    void mapPointListUsesSpeciesCoverWhenPointCoverIsEmpty() throws Exception {
        String mapper =
                Files.readString(
                        Path.of(
                                "src/main/java/com/bdis/modules/map/mapper/MapPointMapper.java"),
                        StandardCharsets.UTF_8);

        assertThat(mapper)
                .contains("COALESCE(d.cover_image_url, h.cover_image_url) AS coverImageUrl");
    }
}
