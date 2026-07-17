package db.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20260717_004__import_standard_atlas_library extends BaseJavaMigration {

    private static final String SOURCE_MARKER = "ATLAS_LIBRARY_MIGRATION_20260717_004";
    private static final int EXPECTED_IMAGE_COUNT = 45;

    private static final Map<String, SpeciesDefinition> SPECIES = speciesDefinitions();

    @Override
    public void migrate(Context context) throws Exception {
        Path assetRoot = resolveAssetRoot();
        Path storageRoot = resolveStorageRoot(assetRoot);
        List<Path> images = listImages(assetRoot);
        if (images.size() != EXPECTED_IMAGE_COUNT) {
            throw new FlywayException(
                    "Expected "
                            + EXPECTED_IMAGE_COUNT
                            + " standard atlas images but found "
                            + images.size()
                            + " in "
                            + assetRoot);
        }

        Files.createDirectories(storageRoot);
        Connection connection = context.getConnection();
        for (Path image : images) {
            importImage(connection, assetRoot, storageRoot, image);
        }
    }

    private void importImage(
            Connection connection, Path assetRoot, Path storageRoot, Path source) throws Exception {
        Path relativeAsset = assetRoot.relativize(source);
        if (relativeAsset.getNameCount() != 3) {
            throw new FlywayException("Unexpected atlas asset path: " + relativeAsset);
        }

        String speciesDirectory = relativeAsset.getName(0).toString();
        String groupDirectory = relativeAsset.getName(1).toString();
        String filename = relativeAsset.getFileName().toString();
        SpeciesDefinition definition = SPECIES.get(speciesDirectory);
        if (definition == null) {
            throw new FlywayException("Unsupported atlas species directory: " + speciesDirectory);
        }

        String logicalKey = relativeAsset.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String digest = sha256(logicalKey);
        String fileNo = "ATLAS_FILE_" + digest.substring(0, 20).toUpperCase(Locale.ROOT);
        String atlasNo = "ATLAS_LIB_" + digest.substring(0, 16).toUpperCase(Locale.ROOT);
        Path relativeStorage = Path.of("atlas-library").resolve(relativeAsset).normalize();
        Path target = storageRoot.resolve(relativeStorage).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new FlywayException("Atlas storage path escapes configured root: " + relativeStorage);
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        long speciesId = ensureSpecies(connection, definition);
        long fileId = ensureFile(connection, source, relativeStorage, fileNo);
        String publicUrl = "/api/public-files/" + fileId + "/content";
        updatePublicFileUrl(connection, fileId, publicUrl);
        long atlasId =
                ensureAtlas(
                        connection,
                        definition,
                        speciesId,
                        atlasNo,
                        filename,
                        groupDirectory,
                        publicUrl);
        ensureFileBusiness(connection, fileId, atlasId);
        ensureTags(connection, atlasId, definition, groupDirectory);
    }

    private long ensureSpecies(Connection connection, SpeciesDefinition definition) throws Exception {
        Long existing = selectSpeciesId(connection, "herb_no", definition.herbNo());
        if (existing == null) {
            existing = selectSpeciesId(connection, "herb_name", definition.herbName());
        }
        if (existing != null) {
            return existing;
        }

        Long categoryId = selectCategoryId(connection, definition.categoryCode());
        String sql =
                """
                INSERT INTO herb_species (
                    herb_no, herb_name, alias_name, latin_name, category_id, category_code,
                    medicinal_part, description, status, is_deleted, created_at, updated_at,
                    remark, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, 0)
                """;
        try (PreparedStatement statement =
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, definition.herbNo());
            statement.setString(2, definition.herbName());
            statement.setString(3, definition.aliasName());
            statement.setString(4, definition.latinName());
            if (categoryId == null) {
                statement.setNull(5, Types.BIGINT);
            } else {
                statement.setLong(5, categoryId);
            }
            statement.setString(6, definition.categoryCode());
            statement.setString(7, definition.medicinalPart());
            statement.setString(8, definition.description());
            statement.setString(9, SOURCE_MARKER);
            statement.executeUpdate();
            return generatedId(statement, "herb_species");
        }
    }

    private Long selectSpeciesId(Connection connection, String column, String value)
            throws Exception {
        String sql = "SELECT id FROM herb_species WHERE " + column + " = ? ORDER BY is_deleted, id LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    private Long selectCategoryId(Connection connection, String categoryCode) throws Exception {
        String sql =
                "SELECT id FROM dict_item WHERE item_code = ? AND is_deleted = 0 ORDER BY id LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, categoryCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    private long ensureFile(
            Connection connection, Path source, Path relativeStorage, String fileNo)
            throws Exception {
        Long existing = selectId(connection, "sys_file_resource", "file_no", fileNo);
        if (existing != null) {
            String sql =
                    """
                    UPDATE sys_file_resource
                    SET storage_path = ?, file_size = ?, file_format = ?, content_type = ?,
                        access_level = 'public', status = 1, is_deleted = 0,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND remark = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, unixPath(relativeStorage));
                statement.setLong(2, Files.size(source));
                statement.setString(3, extension(source));
                statement.setString(4, contentType(source));
                statement.setLong(5, existing);
                statement.setString(6, SOURCE_MARKER);
                statement.executeUpdate();
            }
            return existing;
        }

        String filename = source.getFileName().toString();
        String sql =
                """
                INSERT INTO sys_file_resource (
                    file_no, file_name, original_filename, file_type, file_format, file_size,
                    file_url, storage_path, thumbnail_url, storage_type, access_level, content_type,
                    uploader_name, uploaded_at, status, is_deleted, created_at, updated_at,
                    remark, version
                ) VALUES (?, ?, ?, 'image', ?, ?, ?, ?, NULL, 'local', 'public', ?,
                          'Flyway migration', CURRENT_TIMESTAMP, 1, 0, CURRENT_TIMESTAMP,
                          CURRENT_TIMESTAMP, ?, 0)
                """;
        try (PreparedStatement statement =
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fileNo);
            statement.setString(2, filename);
            statement.setString(3, filename);
            statement.setString(4, extension(source));
            statement.setLong(5, Files.size(source));
            statement.setString(6, "/api/public-files/pending/content");
            statement.setString(7, unixPath(relativeStorage));
            statement.setString(8, contentType(source));
            statement.setString(9, SOURCE_MARKER);
            statement.executeUpdate();
            return generatedId(statement, "sys_file_resource");
        }
    }

    private void updatePublicFileUrl(Connection connection, long fileId, String publicUrl)
            throws Exception {
        String sql =
                """
                UPDATE sys_file_resource
                SET file_url = ?, thumbnail_url = ?, access_level = 'public',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND remark = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, publicUrl);
            statement.setString(2, publicUrl);
            statement.setLong(3, fileId);
            statement.setString(4, SOURCE_MARKER);
            statement.executeUpdate();
        }
    }

    private long ensureAtlas(
            Connection connection,
            SpeciesDefinition definition,
            long speciesId,
            String atlasNo,
            String filename,
            String groupDirectory,
            String publicUrl)
            throws Exception {
        Long existing = selectId(connection, "herb_atlas", "atlas_no", atlasNo);
        if (existing != null) {
            String sql =
                    """
                    UPDATE herb_atlas
                    SET image_url = ?, thumbnail_url = ?, status = 1, is_deleted = 0,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND remark = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, publicUrl);
                statement.setString(2, publicUrl);
                statement.setLong(3, existing);
                statement.setString(4, SOURCE_MARKER);
                statement.executeUpdate();
            }
            return existing;
        }

        AtlasMetadata metadata = atlasMetadata(groupDirectory);
        String sql =
                """
                INSERT INTO herb_atlas (
                    atlas_no, species_id, herb_name, atlas_title, image_url, thumbnail_url,
                    image_type, growth_stage, medicinal_part, health_status, form_type,
                    identification_points, source_type, license_desc, has_watermark,
                    image_quality, usable_for_feature, quality_status, status, is_deleted,
                    created_at, updated_at, remark, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'normal', ?, ?, 'migration_import', ?, 0,
                          'normal', 1, 'available', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, 0)
                """;
        try (PreparedStatement statement =
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, atlasNo);
            statement.setLong(2, speciesId);
            statement.setString(3, definition.herbName());
            statement.setString(4, atlasTitle(definition, metadata, filename));
            statement.setString(5, publicUrl);
            statement.setString(6, publicUrl);
            statement.setString(7, metadata.imageType());
            statement.setString(8, metadata.growthStage());
            statement.setString(9, definition.medicinalPart());
            statement.setString(10, metadata.formType());
            statement.setString(11, "项目内置标准图谱素材，可用于本地图谱特征提取与识别比对。");
            statement.setString(12, "项目内置标准图谱素材");
            statement.setString(13, SOURCE_MARKER);
            statement.executeUpdate();
            return generatedId(statement, "herb_atlas");
        }
    }

    private void ensureFileBusiness(Connection connection, long fileId, long atlasId)
            throws Exception {
        String sql =
                """
                INSERT INTO sys_file_business (
                    file_id, biz_type, biz_id, file_usage, is_public, sort_order,
                    created_at, remark
                ) VALUES (?, 'herb_atlas', ?, 'atlas_image', 1, 0, CURRENT_TIMESTAMP, ?)
                ON DUPLICATE KEY UPDATE is_public = 1, file_usage = 'atlas_image', remark = VALUES(remark)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, fileId);
            statement.setLong(2, atlasId);
            statement.setString(3, SOURCE_MARKER);
            statement.executeUpdate();
        }
    }

    private void ensureTags(
            Connection connection,
            long atlasId,
            SpeciesDefinition definition,
            String groupDirectory)
            throws Exception {
        List<String> tags =
                List.of(
                        "standard",
                        "atlas_library",
                        definition.herbNo().toLowerCase(Locale.ROOT),
                        groupDirectory);
        String sql =
                """
                INSERT INTO herb_atlas_tag (
                    atlas_id, tag_name, tag_type, tag_source, sort_order, status, is_deleted,
                    created_at, updated_at, remark, version
                )
                SELECT ?, ?, 'feature', 'migration', ?, 1, 0, CURRENT_TIMESTAMP,
                       CURRENT_TIMESTAMP, ?, 0
                WHERE NOT EXISTS (
                    SELECT 1 FROM herb_atlas_tag
                    WHERE atlas_id = ? AND tag_name = ? AND is_deleted = 0
                )
                """;
        for (int index = 0; index < tags.size(); index++) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, atlasId);
                statement.setString(2, tags.get(index));
                statement.setInt(3, index);
                statement.setString(4, SOURCE_MARKER);
                statement.setLong(5, atlasId);
                statement.setString(6, tags.get(index));
                statement.executeUpdate();
            }
        }
    }

    private Long selectId(Connection connection, String table, String column, String value)
            throws Exception {
        String sql = "SELECT id FROM " + table + " WHERE " + column + " = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    private long generatedId(PreparedStatement statement, String table) throws Exception {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new FlywayException("Failed to obtain generated id for " + table);
    }

    private List<Path> listImages(Path assetRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(assetRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(V20260717_004__import_standard_atlas_library::isImage)
                    .sorted(Comparator.comparing(path -> unixPath(assetRoot.relativize(path))))
                    .toList();
        }
    }

    private static boolean isImage(Path path) {
        String extension = extension(path);
        return extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("png")
                || extension.equals("webp");
    }

    private static Path resolveAssetRoot() {
        String configured = System.getenv("FILE_ATLAS_IMPORT_PATH");
        List<Path> candidates =
                configured == null || configured.isBlank()
                        ? List.of(
                                Path.of("backend", "import", "herb_atlas"),
                                Path.of("import", "herb_atlas"))
                        : List.of(
                                Path.of(configured),
                                Path.of("backend").resolve(configured),
                                Path.of("backend", "import", "herb_atlas"),
                                Path.of("import", "herb_atlas"));
        return candidates.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(
                        () ->
                                new FlywayException(
                                        "Standard atlas asset directory does not exist; checked "
                                                + candidates));
    }

    private static Path resolveStorageRoot(Path assetRoot) {
        String configured = System.getenv("BDIS_FILE_STORAGE_PATH");
        Path configuredPath =
                configured == null || configured.isBlank()
                        ? Path.of("storage")
                        : Path.of(configured);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        Path importDirectory = assetRoot.getParent();
        Path backendRoot = importDirectory == null ? null : importDirectory.getParent();
        if (backendRoot != null && !configuredPath.startsWith("backend")) {
            return backendRoot.resolve(configuredPath).normalize();
        }
        return configuredPath.toAbsolutePath().normalize();
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static String extension(Path path) {
        String filename = path.getFileName().toString();
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private static String contentType(Path path) {
        return switch (extension(path)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private static String unixPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static AtlasMetadata atlasMetadata(String group) {
        return switch (group) {
            case "root_mature_dried" -> new AtlasMetadata("root", "mature", "dried", "根部成熟干燥图谱");
            case "rhizome_mature_dried" ->
                    new AtlasMetadata("root", "mature", "dried", "根茎成熟干燥图谱");
            case "dried_fruit_processed" ->
                    new AtlasMetadata("fruit", "mature", "processed", "果实加工干燥图谱");
            case "dried_flower_processed" ->
                    new AtlasMetadata("flower", "mature", "processed", "花蕾加工干燥图谱");
            case "fruit_mature_fresh" ->
                    new AtlasMetadata("fruit", "mature", "fresh", "果实成熟鲜品图谱");
            case "flower_flowering_fresh" ->
                    new AtlasMetadata("flower", "flowering", "fresh", "花期鲜品图谱");
            case "flower_leaf_flowering_fresh" ->
                    new AtlasMetadata("flower", "flowering", "fresh", "花叶花期鲜品图谱");
            case "leaf_growth_fresh" ->
                    new AtlasMetadata("leaf", "growth", "fresh", "叶片生长期鲜品图谱");
            case "whole_growth_fresh" ->
                    new AtlasMetadata("whole_plant", "growth", "fresh", "整株生长期鲜品图谱");
            default -> throw new FlywayException("Unsupported atlas group directory: " + group);
        };
    }

    private static String atlasTitle(
            SpeciesDefinition definition, AtlasMetadata metadata, String filename) {
        String sequence = filename.replaceFirst("^.*_(\\d+)\\.[^.]+$", "$1");
        return definition.herbName() + " · " + metadata.title() + " · " + sequence;
    }

    private static Map<String, SpeciesDefinition> speciesDefinitions() {
        Map<String, SpeciesDefinition> definitions = new LinkedHashMap<>();
        definitions.put(
                "dangshen_codonopsis_pilosula",
                new SpeciesDefinition(
                        "HERB_DANGSHEN",
                        "党参",
                        "党参、Codonopsis pilosula",
                        "Codonopsis pilosula",
                        "HERB_CAT_BUYI",
                        "根",
                        "桔梗科党参属植物党参的干燥根。"));
        definitions.put(
                "gouqi_lycium_barbarum",
                new SpeciesDefinition(
                        "HERB_GOUQI",
                        "枸杞",
                        "枸杞子、Lycium barbarum",
                        "Lycium barbarum",
                        "HERB_CAT_BUYI",
                        "果实",
                        "茄科枸杞属植物宁夏枸杞的干燥成熟果实。"));
        definitions.put(
                "huanglian_coptis_chinensis",
                new SpeciesDefinition(
                        "HERB_HUANGLIAN",
                        "黄连",
                        "川连、Coptis chinensis",
                        "Coptis chinensis",
                        "HERB_CAT_QINGRE",
                        "根茎",
                        "毛茛科黄连属植物黄连的干燥根茎。"));
        definitions.put(
                "huangqi_astragalus_mongholicus",
                new SpeciesDefinition(
                        "HERB_HUANGQI",
                        "黄芪",
                        "黄耆、Astragalus mongholicus",
                        "Astragalus mongholicus",
                        "HERB_CAT_BUYI",
                        "根",
                        "豆科黄芪属植物蒙古黄芪的干燥根。"));
        definitions.put(
                "jinyinhua_lonicera_japonica",
                new SpeciesDefinition(
                        "HERB_JINYINHUA",
                        "金银花",
                        "忍冬花、Lonicera japonica",
                        "Lonicera japonica",
                        "HERB_CAT_QINGRE",
                        "花蕾",
                        "忍冬科忍冬属植物忍冬的干燥花蕾或初开的花。"));
        return Map.copyOf(definitions);
    }

    private record SpeciesDefinition(
            String herbNo,
            String herbName,
            String aliasName,
            String latinName,
            String categoryCode,
            String medicinalPart,
            String description) {}

    private record AtlasMetadata(
            String imageType, String growthStage, String formType, String title) {}
}
