package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FlywayMigrationVersionTest {

    private static final Pattern MIGRATION_FILE =
            Pattern.compile("V(\\d{8})_(\\d{3})__.+\\.sql");
    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final Path DEV_BASELINE_MANIFEST =
            Path.of("src/test/resources/flyway-dev-migration-baseline.txt");

    @Test
    void migrationVersionsAreUniqueAndNewMigrationsFollowDevBaseline() throws IOException {
        List<MigrationFile> migrations = readMigrationFiles(MIGRATION_DIRECTORY);
        List<MigrationFile> baseline = readMigrationManifest();
        Set<String> baselineNames =
                baseline.stream().map(MigrationFile::filename).collect(Collectors.toSet());
        MigrationVersion baselineMaximum =
                baseline.stream()
                        .map(MigrationFile::version)
                        .max(Comparator.naturalOrder())
                        .orElseThrow();

        assertThat(migrations).isNotEmpty();
        assertThat(migrations).extracting(MigrationFile::version).doesNotHaveDuplicates();
        assertThat(migrations).extracting(MigrationFile::filename).containsAll(baselineNames);
        assertThat(migrations)
                .filteredOn(migration -> !baselineNames.contains(migration.filename()))
                .allSatisfy(
                        migration ->
                                assertThat(migration.version())
                                        .as(
                                                "new migration %s must be later than dev baseline %s",
                                                migration.filename(), baselineMaximum)
                                        .isGreaterThan(baselineMaximum));
    }

    private List<MigrationFile> readMigrationFiles(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(filename -> filename.endsWith(".sql"))
                    .map(this::parseMigrationFile)
                    .toList();
        }
    }

    private List<MigrationFile> readMigrationManifest() throws IOException {
        return Files.readAllLines(DEV_BASELINE_MANIFEST).stream()
                .filter(filename -> !filename.isBlank())
                .map(this::parseMigrationFile)
                .toList();
    }

    private MigrationFile parseMigrationFile(String filename) {
        Matcher matcher = MIGRATION_FILE.matcher(filename);
        assertThat(matcher.matches()).as("migration filename %s", filename).isTrue();
        return new MigrationFile(
                filename,
                new MigrationVersion(
                        Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
    }

    private record MigrationFile(String filename, MigrationVersion version) {}

    private record MigrationVersion(int date, int sequence) implements Comparable<MigrationVersion> {

        @Override
        public int compareTo(MigrationVersion other) {
            int dateComparison = Integer.compare(date, other.date);
            return dateComparison != 0 ? dateComparison : Integer.compare(sequence, other.sequence);
        }
    }
}
