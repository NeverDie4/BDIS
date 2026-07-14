package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FlywayMigrationVersionTest {

    private static final Pattern MIGRATION_FILE =
            Pattern.compile("V(\\d{8})_(\\d{3})__.+\\.sql");

    @Test
    void migrationVersionsAreUniqueAndStrictlyIncreasing() throws IOException {
        Path migrationDirectory = Path.of("src/main/resources/db/migration");
        List<MigrationVersion> versions;
        try (var files = Files.list(migrationDirectory)) {
            versions =
                    files.filter(Files::isRegularFile)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .filter(filename -> filename.endsWith(".sql"))
                            .map(this::parseVersion)
                            .sorted(Comparator.naturalOrder())
                            .toList();
        }

        assertThat(versions).isNotEmpty();
        assertThat(versions).doesNotHaveDuplicates();
        for (int index = 1; index < versions.size(); index++) {
            assertThat(versions.get(index))
                    .as("migration %s must be greater than %s", versions.get(index), versions.get(index - 1))
                    .isGreaterThan(versions.get(index - 1));
        }
    }

    private MigrationVersion parseVersion(String filename) {
        Matcher matcher = MIGRATION_FILE.matcher(filename);
        assertThat(matcher.matches()).as("migration filename %s", filename).isTrue();
        return new MigrationVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private record MigrationVersion(int date, int sequence) implements Comparable<MigrationVersion> {

        @Override
        public int compareTo(MigrationVersion other) {
            int dateComparison = Integer.compare(date, other.date);
            return dateComparison != 0 ? dateComparison : Integer.compare(sequence, other.sequence);
        }
    }
}
