package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.storage.LocalFileStorage;
import com.bdis.common.storage.LocalFileStorage.StoredFile;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.client.HerbFeatureVectorClient;
import com.bdis.modules.spectrum.dto.HerbAtlasImportRequest;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.entity.SpectrumTagEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasTagMapper;
import com.bdis.modules.spectrum.service.HerbAtlasImportService;
import com.bdis.modules.spectrum.vo.HerbAtlasImportResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class HerbAtlasImportServiceImpl implements HerbAtlasImportService {

    private static final String DEFAULT_IMAGE_TYPE = "standard";
    private static final String DEFAULT_GROWTH_STAGE = "unknown";
    private static final String DEFAULT_SOURCE = "batch_import";
    private static final String DEFAULT_DESCRIPTION = "Batch imported standard atlas image";
    private static final String DEFAULT_CATEGORY = "中药材";
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
    private final LocalFileStorage localFileStorage;
    private final HerbFeatureVectorClient featureVectorClient;
    private final ObjectMapper objectMapper;
    private final String defaultImportPath;

    public HerbAtlasImportServiceImpl(
            HerbAtlasMapper herbAtlasMapper,
            HerbAtlasTagMapper herbAtlasTagMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            LocalFileStorage localFileStorage,
            HerbFeatureVectorClient featureVectorClient,
            ObjectMapper objectMapper,
            @Value("${file.atlas-import-path:import/herb_atlas}") String defaultImportPath,
            @Value("${herb.atlas.auto-import:false}") boolean autoImport) {
        this.herbAtlasMapper = herbAtlasMapper;
        this.herbAtlasTagMapper = herbAtlasTagMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.localFileStorage = localFileStorage;
        this.featureVectorClient = featureVectorClient;
        this.objectMapper = objectMapper;
        this.defaultImportPath = defaultImportPath;
    }

    @Override
    public HerbAtlasImportResultVO importAtlas(HerbAtlasImportRequest request) {
        Path importRoot = resolveImportRoot(request);
        HerbAtlasImportResultVO result = new HerbAtlasImportResultVO();
        if (!Files.isDirectory(importRoot)) {
            throw new BusinessException("Atlas import path does not exist");
        }

        try (Stream<Path> speciesDirectories = Files.list(importRoot)) {
            speciesDirectories
                    .filter(Files::isDirectory)
                    .sorted()
                    .forEach(speciesDirectory -> importSpeciesDirectory(speciesDirectory, result));
        } catch (IOException exception) {
            throw new BusinessException("Failed to scan atlas import path");
        }
        return result;
    }

    private Path resolveImportRoot(HerbAtlasImportRequest request) {
        if (request != null && StringUtils.hasText(request.getImportPath())) {
            return Path.of(request.getImportPath()).normalize();
        }
        return Path.of(defaultImportPath).normalize();
    }

    private void importSpeciesDirectory(Path speciesDirectory, HerbAtlasImportResultVO result) {
        String directoryName = speciesDirectory.getFileName().toString();
        List<Path> images = listImages(speciesDirectory);
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
        species.setHerbCode(knownSpecies.herbCode());
        species.setHerbName(knownSpecies.herbName());
        species.setLatinName(knownSpecies.latinName());
        species.setAliasName(directoryName.replace('_', ' '));
        species.setCategory(DEFAULT_CATEGORY);
        species.setMedicinalPart(knownSpecies.medicinalPart());
        species.setStatus(1);
        species.setCreateTime(now);
        species.setUpdateTime(now);
        species.setDeleted(0);
        herbSpeciesMapper.insertSpecies(species);
        return species;
    }

    private List<Path> listImages(Path speciesDirectory) {
        List<Path> images = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(speciesDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(localFileStorage::isSupportedImage)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(images::add);
        } catch (IOException exception) {
            return images;
        }
        return images;
    }

    @Transactional
    protected void importImage(Path image, HerbEntity species, HerbAtlasImportResultVO result) {
        String imageName = image.getFileName().toString();
        String speciesCode = importCode(species.getHerbCode());
        if (herbAtlasMapper.existsBySpeciesIdAndImageName(species.getId(), imageName) > 0) {
            result.addSkip(imageName, speciesCode, "Image already exists");
            return;
        }

        try {
            String herbCodeLower = speciesCode.toLowerCase(Locale.ROOT);
            StoredFile storedFile = localFileStorage.copyHerbAtlasImportImage(image, herbCodeLower);
            LocalDateTime now = LocalDateTime.now();
            SpectrumEntity atlas = buildAtlas(species, speciesCode, imageName, storedFile.url(), now);
            herbAtlasMapper.insertAtlas(atlas);
            herbAtlasTagMapper.insertTags(defaultTags(atlas.getId(), herbCodeLower, now));
            result.addSuccess(imageName, speciesCode, "Imported successfully");
        } catch (RuntimeException exception) {
            result.addFail(imageName, speciesCode, exception.getMessage());
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
        atlas.setAtlasCode(nextAtlasCode(importSpeciesCode));
        atlas.setImageUrl(imageUrl);
        atlas.setImageName(imageName);
        atlas.setImageType(DEFAULT_IMAGE_TYPE);
        atlas.setGrowthStage(DEFAULT_GROWTH_STAGE);
        atlas.setMedicinalPart(species.getMedicinalPart());
        atlas.setSource(DEFAULT_SOURCE);
        atlas.setDescription(DEFAULT_DESCRIPTION);
        fillFeatureVector(atlas);
        atlas.setStatus(1);
        atlas.setCreateTime(now);
        atlas.setUpdateTime(now);
        atlas.setDeleted(0);
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

    private List<SpectrumTagEntity> defaultTags(Long atlasId, String herbCodeLower, LocalDateTime now) {
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
        tag.setCreateTime(now);
        tag.setUpdateTime(now);
        tag.setDeleted(0);
        return tag;
    }

    private void fillFeatureVector(SpectrumEntity atlas) {
        try {
            HerbImageEntity image = new HerbImageEntity();
            image.setImageUrl(atlas.getImageUrl());
            List<Double> vector = featureVectorClient.extract(image);
            if (CollectionUtils.isEmpty(vector)) {
                return;
            }
            atlas.setFeatureVector(objectMapper.writeValueAsString(vector));
            atlas.setFeatureDim(vector.size());
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Failed to serialize atlas feature vector");
        } catch (RuntimeException exception) {
            atlas.setFeatureVector(null);
            atlas.setFeatureDim(null);
        }
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
