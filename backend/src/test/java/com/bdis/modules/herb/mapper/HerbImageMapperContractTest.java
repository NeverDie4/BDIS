package com.bdis.modules.herb.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HerbImageMapperContractTest {

    @Test
    void imageQueriesResolveUploaderNameFromSysUser() throws IOException {
        try (InputStream input =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("mapper/herb/HerbImageMapper.xml")) {
            assertNotNull(input, "HerbImageMapper.xml must exist on the test classpath");
            String mapper = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertFalse(
                    mapper.contains("i.uploader_name"), "herb_image has no uploader_name column");
            assertTrue(
                    mapper.contains("COALESCE(u.real_name, u.username) AS uploader_name"),
                    "uploader name must be resolved from sys_user");
            assertTrue(
                    mapper.contains(
                            "LEFT JOIN sys_user u ON u.id = i.uploader_id AND u.is_deleted = 0"),
                    "image VO queries must join sys_user by uploader_id");
        }
    }
}
