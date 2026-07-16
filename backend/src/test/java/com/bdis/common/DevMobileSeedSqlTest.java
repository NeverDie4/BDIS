package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DevMobileSeedSqlTest {

    @Test
    void collectorDevPasswordHashMatchesDocumentedPassword() throws Exception {
        String sql = Files.readString(Path.of("..", "scripts", "dev-mobile-seed.sql"));
        Matcher matcher = Pattern.compile("'collector_dev',\\s*'([^']+)'").matcher(sql);

        assertThat(matcher.find()).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("password", matcher.group(1))).isTrue();
    }
}
