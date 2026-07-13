package com.bdis.modules.spectrum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.dto.HerbAtlasImportRequest;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.entity.SpectrumTagEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasTagMapper;
import com.bdis.modules.spectrum.service.HerbAtlasImportService;
import com.bdis.modules.spectrum.vo.HerbAtlasImportResultVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HerbAtlasImportServiceImpl implements HerbAtlasImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HerbAtlasImportServiceImpl.class);

    private static final String DEFAULT_IMAGE_TYPE = "standard";
    private static final String DEFAULT_GROWTH_STAGE = "unknown";
    private static final String DEFAULT_SOURCE = "batch_import";
    private static final String DEFAULT_DESCRIPTION = "Batch imported standard atlas image";
    private static final String DEFAULT_CATEGORY = "HERB";
    private static final int MAX_IMPORT_ROOT_ENTRIES = 200;
    private static final int MAX_IMPORT_DEPTH = 4;
    private static final int MAX_SCANNED_ENTRIES_PER_SPECIES = 5000;
    private static final int MAX_IMAGES_PER_SPECIES = 1000;
    private static final Map<String, KnownSpecies> KNOWN_SPECIES =
            Map.of(
                    "HUANGLIAN_COPTIS_CHINENSIS",
                    new KnownSpecies("HERB_HUANGLIAN", "黄连", "Coptis chinensis", "根茎"),
                    "HUANGQI_ASTRAGALUS_MONGHOLICUS",
                    new KnownSpecies("HERB_HUANGQI", "黄芪", "Astragalus mongholicus", "根"),
                    "DANGSHEN_CODONOPSIS_PILOSULA",
                    new KnownSpecies("HERB_DANGSHEN", "党参", "Codonopsis pilosula", "根"),
                    "GOUQI_LYCIUM_BARBARUM",
                    new KnownSpecies("HERB_GOUQI", "枸杞", "Lycium barbarum", "果实"),
                    "JINYINHUA_LONICERA_JAPONICA",
                    new KnownSpecies("HERB_JINYINHUA", "金银花", "Lonicera japonica", "花"));

    private final HerbAtlasMapper herbAtlasMapper;
    private final HerbAtlasTagMapper herbAtlasTagMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;
    private final Path defaultImportRoot;

    public HerbAtlasImportServiceImpl(
            HerbAtlasMapper herbAtlasMapper,
            HerbAtlasTagMapper herbAtlasTagMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            FileResourceService fileResourceService,
            FileBusinessService fileBusinessService,
            @Value("${file.atlas-import-path:import/herb_atlas}") String defaultImportPath) {
        this.herbAtlasMapper = herbAtlasMapper;
        this.herbAtlasTagMapper = herbAtlasTagMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.fileResourceService = fileResourceService;
        this.fileBusinessService = fileBusinessService;
        this.defaultImportRoot = Path.of(defaultImportPath).toAbsolutePath().normalize();
    }

    @Override
    public HerbAtlasImportResultVO importAtlas(HerbAtlasImportRequest request) {
        Path importRoot = resolveImportRoot(request);
        HerbAtlasImportResultVO result = new HerbAtlasImportResultVO();
        if (!Files.isDirectory(importRoot)) {
            throw new BusinessException("Atlas import path does not exist");
        }

        try (Stream<Path> speciesDirectories = Files.list(importRoot)) {
            List<Path> rootEntries =
                    speciesDirectories.limit(MAX_IMPORT_ROOT_ENTRIES + 1L).toList();
            if (rootEntries.size() > MAX_IMPORT_ROOT_ENTRIES) {
                throw new BusinessException("Atlas import contains too many root entries");
            }
            List<Path> directories =
                    rootEntries.stream()
                            .filter(Files::isDirectory)
                            .map(path -> requireContainedDirectory(importRoot, path))
                            .sorted()
                            .toList();
            directories.forEach(
                    speciesDirectory ->
                            importSpeciesDirectory(importRoot, speciesDirectory, result));
        } catch (IOException exception) {
            throw new BusinessException("Failed to scan atlas import path");
        }
        return result;
    }

    private Path resolveImportRoot(HerbAtlasImportRequest request) {
        Path allowedRoot = requireDirectory(defaultImportRoot, "Atlas import root does not exist");
        if (request == null || !StringUtils.hasText(request.getImportPath())) {
            return allowedRoot;
        }
        Path relativePath;
        try {
            relativePath = Path.of(request.getImportPath());
        } catch (InvalidPathException exception) {
            throw new BusinessException("Atlas import path is invalid");
        }
        if (relativePath.isAbsolute()) {
            throw new BusinessException(
                    "Atlas import path must be relative to the configured root");
        }
        Path candidate = allowedRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(allowedRoot)) {
            throw new BusinessException("Atlas import path exceeds the configured root");
        }
        Path realCandidate = requireDirectory(candidate, "Atlas import path does not exist");
        if (!realCandidate.startsWith(allowedRoot)) {
            throw new BusinessException("Atlas import path exceeds the configured root");
        }
        return realCandidate;
    }

    private Path requireDirectory(Path path, String message) {
        try {
            Path realPath = path.toRealPath();
            if (!Files.isDirectory(realPath)) {
                throw new BusinessException(message);
            }
            return realPath;
        } catch (IOException exception) {
            throw new BusinessException(message);
        }
    }

    private Path requireContainedDirectory(Path importRoot, Path directory) {
        Path realDirectory =
                requireDirectory(directory, "Atlas species directory is not accessible");
        if (!realDirectory.startsWith(importRoot)) {
            throw new BusinessException("Atlas species directory exceeds the configured root");
        }
        return realDirectory;
    }

    private Path requireContainedFile(Path importRoot, Path file) {
        try {
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(importRoot) || !Files.isRegularFile(realFile)) {
                throw new BusinessException("Atlas image exceeds the configured root");
            }
            return realFile;
        } catch (IOException exception) {
            throw new BusinessException("Atlas image is not accessible");
        }
    }

    private void importSpeciesDirectory(
            Path importRoot, Path speciesDirectory, HerbAtlasImportResultVO result) {
        String directoryName = speciesDirectory.getFileName().toString();
        List<Path> images = listImages(importRoot, speciesDirectory);
        if (images.isEmpty()) {
            result.addSkip(directoryName, normalizeCode(directoryName), "Empty directory");
            return;
        }
        HerbEntity species = matchOrCreateSpecies(directoryName);
        if (species == null) {
            for (Path image : images) {
                result.increaseTotal();
                result.addFail(
                        image.getFileName().toString(),
                        normalizeCode(directoryName),
                        "Species not matched");
            }
            return;
        }
        for (Path image : images) {
            result.increaseTotal();
            importImage(image, species, result);
        }
    }

    private HerbEntity matchOrCreateSpecies(String directoryName) {
        HerbEntity species = matchSpecies(directoryName);
        if (species != null) {
            return species;
        }
        return createKnownSpecies(directoryName);
    }

    private HerbEntity matchSpecies(String directoryName) {
        String normalizedCode = normalizeCode(directoryName);
        HerbEntity species = herbSpeciesMapper.selectByHerbCode(normalizedCode);
        if (species != null) {
            return species;
        }
        species = herbSpeciesMapper.selectByHerbCode("HERB_" + normalizedCode);
        if (species != null) {
            return species;
        }
        String firstCodePart = normalizedCode.split("_")[0];
        species = herbSpeciesMapper.selectByHerbCode(firstCodePart);
        if (species != null) {
            return species;
        }
        species = herbSpeciesMapper.selectByHerbCode("HERB_" + firstCodePart);
        if (species != null) {
            return species;
        }
        return herbSpeciesMapper.selectByNameOrAlias(directoryName.replace('_', ' '));
    }

    private HerbEntity createKnownSpecies(String directoryName) {
        KnownSpecies knownSpecies = KNOWN_SPECIES.get(normalizeCode(directoryName));
        if (knownSpecies == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        HerbEntity species = new HerbEntity();
        species.setHerbNo(knownSpecies.herbCode());
        species.setHerbName(knownSpecies.herbName());
        species.setLatinName(knownSpecies.latinName());
        species.setAliasName(directoryName.replace('_', ' '));
        species.setCategoryCode(DEFAULT_CATEGORY);
        species.setMedicinalPart(knownSpecies.medicinalPart());
        species.setStatus(1);
        species.setCreatedAt(now);
        species.setUpdatedAt(now);
        species.setIsDeleted(0);
        species.setVersion(0);
        herbSpeciesMapper.insertSpecies(species);
        return species;
    }

    private List<Path> listImages(Path importRoot, Path speciesDirectory) {
        try (Stream<Path> paths = Files.walk(speciesDirectory, MAX_IMPORT_DEPTH)) {
            List<Path> entries = paths.limit(MAX_SCANNED_ENTRIES_PER_SPECIES + 1L).toList();
            if (entries.size() > MAX_SCANNED_ENTRIES_PER_SPECIES) {
                throw new BusinessException("Atlas species directory contains too many entries");
            }
            List<Path> images =
                    entries.stream()
                            .filter(Files::isRegularFile)
                            .filter(this::isSupportedImage)
                            .map(path -> requireContainedFile(importRoot, path))
                            .sorted(Comparator.comparing(Path::toString))
                            .toList();
            if (images.size() > MAX_IMAGES_PER_SPECIES) {
                throw new BusinessException("Atlas species directory contains too many images");
            }
            return images;
        } catch (IOException exception) {
            return List.of();
        }
    }

    protected void importImage(Path image, HerbEntity species, HerbAtlasImportResultVO result) {
        String imageName = image.getFileName().toString();
        String speciesCode = importCode(species.getHerbNo());
        if (herbAtlasMapper.existsBySpeciesIdAndImageName(species.getId(), imageName) > 0) {
            result.addSkip(imageName, speciesCode, "Image already exists");
            return;
        }

        FileResourceVO file = null;
        SpectrumEntity atlas = null;
        try {
            String herbCodeLower = speciesCode.toLowerCase(Locale.ROOT);
            file = fileResourceService.importPrivate(image, imageName, "图谱批量导入：" + speciesCode);
            LocalDateTime now = LocalDateTime.now();
            atlas = buildAtlas(species, speciesCode, imageName, file.getFileUrl(), now);
            herbAtlasMapper.insertAtlas(atlas);
            bindAtlasFile(file.getId(), atlas.getId());
            fileResourceService.publishForBusiness(file.getId(), "herb_atlas", atlas.getId());
            atlas.setImageUrl("/api/public-files/" + file.getId() + "/content");
            herbAtlasMapper.updateImageUrl(atlas.getId(), atlas.getImageUrl());
            herbAtlasTagMapper.insertTags(defaultTags(atlas.getId(), herbCodeLower, now));
            result.addSuccess(imageName, speciesCode, "Imported successfully");
        } catch (RuntimeException exception) {
            cleanupFailedImport(atlas, file);
            result.addFail(imageName, speciesCode, exception.getMessage());
        }
    }

    private void bindAtlasFile(Long fileId, Long atlasId) {
        FileBusinessBindDTO bind = new FileBusinessBindDTO();
        bind.setFileId(fileId);
        bind.setBizType("herb_atlas");
        bind.setBizId(atlasId);
        bind.setFileUsage("atlas_image");
        bind.setRemark("图谱标准图片");
        fileBusinessService.bind(bind);
    }

    private void cleanupFailedImport(SpectrumEntity atlas, FileResourceVO file) {
        if (atlas != null && atlas.getId() != null) {
            runCleanup(
                    () ->
                            herbAtlasTagMapper.delete(
                                    new LambdaQueryWrapper<SpectrumTagEntity>()
                                            .eq(SpectrumTagEntity::getAtlasId, atlas.getId())),
                    "atlas tags",
                    atlas.getId());
            runCleanup(
                    () -> {
                        if (file != null && file.getId() != null) {
                            fileBusinessService.deleteByBusinessAndFile(
                                    "herb_atlas", atlas.getId(), file.getId());
                        }
                    },
                    "atlas file relation",
                    atlas.getId());
            runCleanup(
                    () -> herbAtlasMapper.deleteById(atlas.getId()), "atlas record", atlas.getId());
        }
        if (file != null && file.getId() != null) {
            runCleanup(
                    () -> fileResourceService.delete(file.getId()), "file resource", file.getId());
        }
    }

    private void runCleanup(Runnable cleanup, String resourceType, Long resourceId) {
        try {
            cleanup.run();
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Failed to clean up {} after atlas import failure: id={}",
                    resourceType,
                    resourceId,
                    exception);
        }
    }

    private SpectrumEntity buildAtlas(
            HerbEntity species,
            String importSpeciesCode,
            String imageName,
            String imageUrl,
            LocalDateTime now) {
        SpectrumEntity atlas = new SpectrumEntity();
        atlas.setSpeciesId(species.getId());
        atlas.setHerbName(species.getHerbName());
        atlas.setAtlasNo(nextAtlasCode(importSpeciesCode));
        atlas.setImageUrl(imageUrl);
        atlas.setAtlasTitle(imageName);
        atlas.setImageType(DEFAULT_IMAGE_TYPE);
        atlas.setGrowthStage(DEFAULT_GROWTH_STAGE);
        atlas.setMedicinalPart(species.getMedicinalPart());
        atlas.setSourceType(DEFAULT_SOURCE);
        atlas.setIdentificationPoints(DEFAULT_DESCRIPTION);
        atlas.setStatus(1);
        atlas.setCreatedBy(CurrentUserUtils.currentUserId());
        atlas.setCreatedAt(now);
        atlas.setUpdatedAt(now);
        atlas.setIsDeleted(0);
        atlas.setVersion(0);
        return atlas;
    }

    private String nextAtlasCode(String herbCode) {
        int index = 1;
        while (true) {
            String atlasCode = "ATLAS_" + herbCode + "_" + String.format("%03d", index);
            if (herbAtlasMapper.countByAtlasCode(atlasCode) == 0) {
                return atlasCode;
            }
            index++;
        }
    }

    private boolean isSupportedImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")
                || fileName.endsWith(".webp");
    }

    private List<SpectrumTagEntity> defaultTags(
            Long atlasId, String herbCodeLower, LocalDateTime now) {
        return List.of(
                tag(atlasId, "standard", now),
                tag(atlasId, "batch_import", now),
                tag(atlasId, herbCodeLower, now));
    }

    private SpectrumTagEntity tag(Long atlasId, String tagName, LocalDateTime now) {
        SpectrumTagEntity tag = new SpectrumTagEntity();
        tag.setAtlasId(atlasId);
        tag.setTagName(tagName);
        tag.setTagType("feature");
        tag.setStatus(1);
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        tag.setIsDeleted(0);
        tag.setVersion(0);
        return tag;
    }

    private String normalizeCode(String value) {
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String importCode(String herbCode) {
        String normalized = normalizeCode(herbCode);
        if (normalized.startsWith("HERB_")) {
            return normalized.substring("HERB_".length());
        }
        return normalized;
    }

    private record KnownSpecies(
            String herbCode, String herbName, String latinName, String medicinalPart) {}
}
