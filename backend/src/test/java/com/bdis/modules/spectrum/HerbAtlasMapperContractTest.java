package com.bdis.modules.spectrum;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HerbAtlasMapperContractTest {

    private static final Path MAPPER =
            Path.of("src/main/resources/mapper/spectrum/HerbAtlasMapper.xml");

    @Test
    void atlasVoTimeColumnsUseExistingVoProperties() throws Exception {
        String mapper = Files.readString(MAPPER);
        int resultMapStart = mapper.indexOf("<resultMap id=\"AtlasVOResultMap\"");
        int resultMapEnd = mapper.indexOf("</resultMap>", resultMapStart);
        String atlasVoResultMap = mapper.substring(resultMapStart, resultMapEnd);

        assertThat(atlasVoResultMap).contains("column=\"created_at\" property=\"createTime\"");
        assertThat(atlasVoResultMap).contains("column=\"updated_at\" property=\"updateTime\"");
        assertThat(atlasVoResultMap)
                .doesNotContain("column=\"created_at\" property=\"createdAt\"");
        assertThat(atlasVoResultMap)
                .doesNotContain("column=\"updated_at\" property=\"updatedAt\"");
    }
}
