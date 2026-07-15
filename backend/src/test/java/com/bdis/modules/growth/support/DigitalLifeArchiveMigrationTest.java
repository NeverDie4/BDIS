package com.bdis.modules.growth.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class DigitalLifeArchiveMigrationTest {

    private static final String DIGITAL_LIFE_MIGRATION_VERSION = "20260716_004";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d{8}_\\d{3})__.+\\.sql$");

    @Test
    void digitalLifeMigrationVersionIsUnique() throws Exception {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");
        List<String> versions;
        try (var paths = Files.list(migrationDirectory)) {
            versions =
                    paths.map(path -> path.getFileName().toString())
                            .map(VERSION_PATTERN::matcher)
                            .filter(Matcher::matches)
                            .map(matcher -> matcher.group(1))
                            .toList();
        }

        assertThat(versions).doesNotHaveDuplicates();
        assertThat(versions)
                .contains("20260715_001", "20260715_006", DIGITAL_LIFE_MIGRATION_VERSION);
    }

    @Test
    void migrationAddsTaskLevelTraceAndVisibilityFields() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src",
                                "main",
                                "resources",
                                "db",
                                "migration",
                                "V20260715_001__add_digital_life_archive_visibility.sql"));

        assertThat(sql).contains("ALTER TABLE herb_collection_task");
        assertThat(sql).contains("trace_code VARCHAR(100)");
        assertThat(sql).contains("public_visible TINYINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("UNIQUE KEY uk_herb_collection_task_trace_code");
    }

    @Test
    void appliedCategoryMigrationRemainsByteCompatibleWithoutDuplicateForwardMigration()
            throws Exception {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");
        Path historical =
                migrationDirectory.resolve("V20260714_017__seed_herb_category_dictionary.sql");
        Path forwardFix =
                migrationDirectory.resolve(
                        "V20260715_002__complete_herb_category_dictionary_seed.sql");

        assertThat(flywayChecksum(historical)).isEqualTo(-767658312);
        assertThat(forwardFix).doesNotExist();
    }

    @Test
    void narrationMigrationCreatesVersionedSnapshotCache() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src",
                                "main",
                                "resources",
                                "db",
                                "migration",
                                "V20260715_003__add_digital_life_narration_cache.sql"));

        assertThat(sql).contains("CREATE TABLE herb_digital_life_narration");
        assertThat(sql).contains("input_snapshot CHAR(64)");
        assertThat(sql).contains("narration_source VARCHAR(20)");
        assertThat(sql)
                .contains(
                        "UNIQUE KEY uk_digital_life_narration_version (growth_record_id,"
                                + " narration_type, prompt_version)");
    }

    @Test
    void integrityMigrationCreatesVersionedArchiveAndEventChain() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src",
                                "main",
                                "resources",
                                "db",
                                "migration",
                                "V20260715_004__add_digital_life_hash_chain.sql"));

        assertThat(sql).contains("CREATE TABLE herb_trace_hash_archive");
        assertThat(sql).contains("CREATE TABLE herb_trace_hash_event");
        assertThat(sql).contains("root_hash CHAR(64)");
        assertThat(sql).contains("previous_hash CHAR(64)");
        assertThat(sql).contains("event_hash CHAR(64)");
        assertThat(sql)
                .contains("UNIQUE KEY uk_trace_hash_archive_version (task_id, hash_version)");
        assertThat(sql)
                .contains(
                        "UNIQUE KEY uk_trace_hash_event_sequence (task_id, hash_version,"
                                + " sequence)");
    }

    @Test
    void eligiblePublicStagesPublishTaskLevelArchive() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src",
                                "main",
                                "resources",
                                "db",
                                "migration",
                                "V20260715_006__publish_eligible_digital_life_archives.sql"));

        assertThat(sql).contains("review_status = 'approved'");
        assertThat(sql).contains("public_visible = 1");
        assertThat(sql).contains("HAVING COUNT(*) >= 2");
        assertThat(sql).contains("CONCAT('DL-TASK-', LPAD(t.id, 8, '0'))");
    }

    private int flywayChecksum(Path migration) throws Exception {
        CRC32 checksum = new CRC32();
        for (String line : Files.readAllLines(migration, StandardCharsets.UTF_8)) {
            checksum.update(line.getBytes(StandardCharsets.UTF_8));
        }
        return (int) checksum.getValue();
    }
}
