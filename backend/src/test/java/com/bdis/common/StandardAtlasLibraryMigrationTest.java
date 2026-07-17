package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StandardAtlasLibraryMigrationTest {

    private static final Path ASSET_ROOT = Path.of("import/herb_atlas");
    private static final Path MIGRATION =
            Path.of(
                    "src/main/java/db/migration/"
                            + "V20260717_004__import_standard_atlas_library.java");

    @Test
    void atlasLibraryContainsFiveCompleteSpeciesSets() throws IOException {
        Map<String, Long> imagesBySpecies;
        try (var paths = Files.walk(ASSET_ROOT)) {
            imagesBySpecies =
                    paths.filter(Files::isRegularFile)
                            .filter(StandardAtlasLibraryMigrationTest::isImage)
                            .collect(
                                    Collectors.groupingBy(
                                            path -> ASSET_ROOT.relativize(path).getName(0).toString(),
                                            Collectors.counting()));
        }

        assertThat(imagesBySpecies)
                .containsOnlyKeys(
                        "dangshen_codonopsis_pilosula",
                        "gouqi_lycium_barbarum",
                        "huanglian_coptis_chinensis",
                        "huangqi_astragalus_mongholicus",
                        "jinyinhua_lonicera_japonica");
        assertThat(imagesBySpecies.values()).allMatch(count -> count == 9L);
        assertThat(imagesBySpecies.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(45L);
    }

    @Test
    void migrationUsesDeterministicControlledPublicFileRecords() throws IOException {
        String source = Files.readString(MIGRATION);

        assertThat(source).contains("ATLAS_LIBRARY_MIGRATION_20260717_004");
        assertThat(source).contains("EXPECTED_IMAGE_COUNT = 45");
        assertThat(source).contains("/api/public-files/");
        assertThat(source).contains("sys_file_resource");
        assertThat(source).contains("sys_file_business");
        assertThat(source).contains("herb_atlas");
        assertThat(source).contains("herb_atlas_tag");
        assertThat(source).contains("ON DUPLICATE KEY UPDATE");
        assertThat(source).doesNotContain("D:\\BDIS", "C:\\");
    }

    @Test
    void migrationMapsEveryAtlasGroupDirectory() throws IOException {
        Set<String> groups;
        try (var paths = Files.walk(ASSET_ROOT)) {
            groups =
                    paths.filter(Files::isRegularFile)
                            .filter(StandardAtlasLibraryMigrationTest::isImage)
                            .map(path -> ASSET_ROOT.relativize(path).getName(1).toString())
                            .collect(Collectors.toSet());
        }
        String source = Files.readString(MIGRATION);

        assertThat(groups)
                .allSatisfy(group -> assertThat(source).contains("case \"" + group + "\""));
    }

    private static boolean isImage(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp");
    }
}
