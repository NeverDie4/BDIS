package com.bdis.modules.collection.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class HerbCollectionTaskMapperContractTest {

    @Test
    void baseTaskQueryIncludesEnabledStatusUsedByPublicArchiveValidation() throws Exception {
        Path mapper =
                Path.of(
                        "src/main/resources/mapper/collection/HerbCollectionTaskMapper.xml");
        String xml = Files.readString(mapper, StandardCharsets.UTF_8);

        assertThat(xml).contains("<result column=\"status\" property=\"status\"/>");
        assertThat(xml).containsPattern("(?s)<sql id=\"BaseColumns\">.*public_visible,\\s*status,");
    }
}
