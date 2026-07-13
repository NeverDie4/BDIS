package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
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
import com.bdis.modules.spectrum.service.impl.HerbAtlasImportServiceImpl;
import com.bdis.modules.spectrum.vo.HerbAtlasImportResultVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbAtlasImportServiceTest {

    @TempDir private Path workspace;

    @Mock private HerbAtlasMapper herbAtlasMapper;

    @Mock private HerbAtlasTagMapper herbAtlasTagMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private FileResourceService fileResourceService;

    @Mock private FileBusinessService fileBusinessService;

    private HerbAtlasImportService herbAtlasImportService;

    private Path importRoot;

    @BeforeEach
    void setUp() throws Exception {
        importRoot = Files.createTempDirectory(Path.of("target"), "atlas-import-").resolve("herb_atlas");
        Files.createDirectories(importRoot);
        FileResourceVO file = new FileResourceVO();
        file.setId(11L);
        file.setFileUrl("/api/public-files/11/content");
        lenient()
                .when(fileResourceService.importPublic(any(Path.class), any(), any()))
                .thenReturn(file);
        lenient()
                .doAnswer(
                        invocation -> {
                            SpectrumEntity atlas = invocation.getArgument(0);
                            atlas.setId(101L);
                            return 1;
                        })
                .when(herbAtlasMapper)
                .insertAtlas(any(SpectrumEntity.class));
        herbAtlasImportService =
                new HerbAtlasImportServiceImpl(
                        herbAtlasMapper,
                        herbAtlasTagMapper,
                        herbSpeciesMapper,
                        fileResourceService,
                        fileBusinessService,
                        importRoot.toRealPath().toString(),
                        false);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws IOException {
        if (importRoot != null && Files.exists(importRoot.getParent())) {
            try (var paths = Files.walk(importRoot.getParent())) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    @Test
    void importAtlasRejectsAbsolutePath() throws Exception {
        Files.createDirectories(importRoot);
        Path outside = Path.of(System.getProperty("java.io.tmpdir"), "outside").toAbsolutePath();
        HerbAtlasImportRequest request = new HerbAtlasImportRequest();
        request.setImportPath(outside.toString());

        assertThatThrownBy(() -> herbAtlasImportService.importAtlas(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Atlas import path must be relative to the configured root");
    }

    @Test
    void importAtlasRejectsDirectoryTraversal() throws Exception {
        Files.createDirectories(importRoot);
        HerbAtlasImportRequest request = new HerbAtlasImportRequest();
        request.setImportPath("../outside");

        assertThatThrownBy(() -> herbAtlasImportService.importAtlas(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Atlas import path exceeds the configured root");
    }

    @Test
    void importAtlasAllowsRelativeDirectoryInsideConfiguredRoot() throws Exception {
        Files.createDirectories(importRoot.resolve("batch-20260712"));
        HerbAtlasImportRequest request = new HerbAtlasImportRequest();
        request.setImportPath("batch-20260712");

        HerbAtlasImportResultVO result = herbAtlasImportService.importAtlas(request);

        assertThat(result.getTotalCount()).isZero();
    }

    @Test
    void importAtlasRejectsSymbolicLinkOutsideConfiguredRoot() throws Exception {
        Files.createDirectories(importRoot);
        Path outside = importRoot.getParent().resolve("outside");
        Files.createDirectories(outside);
        Path link = importRoot.resolve("outside-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            return;
        }
        HerbAtlasImportRequest request = new HerbAtlasImportRequest();
        request.setImportPath("outside-link");

        assertThatThrownBy(() -> herbAtlasImportService.importAtlas(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Atlas import path exceeds the configured root");
    }

    @Test
    void importAtlasCopiesImagesAndInsertsAtlasWithDefaultTags() throws Exception {
        Path imageDir =
                importRoot.resolve("huanglian_coptis_chinensis").resolve("leaf_growth_fresh");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_001.jpg"), new byte[] {1, 2, 3});
        Files.writeString(imageDir.resolve("notes.txt"), "ignored");

        HerbEntity species = species(1L, "HUANGLIAN", "Huanglian", "rhizome");
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN_COPTIS_CHINENSIS")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN_COPTIS_CHINENSIS"))
                .thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_001.jpg")).thenReturn(0);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_001")).thenReturn(0);

        HerbAtlasImportResultVO result =
                herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getSkipCount()).isZero();
        assertThat(result.getFailCount()).isZero();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("success");

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        SpectrumEntity atlas = atlasCaptor.getValue();
        assertThat(atlas.getAtlasNo()).isEqualTo("ATLAS_HUANGLIAN_001");
        assertThat(atlas.getAtlasTitle()).isEqualTo("huanglian_001.jpg");
        assertThat(atlas.getImageType()).isEqualTo("standard");
        assertThat(atlas.getFeatureVector()).isNull();
        assertThat(atlas.getFeatureDim()).isNull();
        assertThat(atlas.getGrowthStage()).isEqualTo("unknown");
        assertThat(atlas.getMedicinalPart()).isEqualTo("rhizome");
        assertThat(atlas.getImageUrl()).isEqualTo("/api/public-files/11/content");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SpectrumTagEntity>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(herbAtlasTagMapper).insertTags(tagsCaptor.capture());
        verify(fileBusinessService).bindSystem(any());
        assertThat(tagsCaptor.getValue())
                .extracting(SpectrumTagEntity::getTagName)
                .containsExactly("standard", "batch_import", "huanglian");
    }

    @Test
    void reconcileLegacyAtlasRegistersAndBindsExistingFile() {
        SpectrumEntity atlas = new SpectrumEntity();
        atlas.setId(101L);
        atlas.setAtlasTitle("legacy.jpg");
        atlas.setImageUrl("/api/files/uploads/2026-07-11/legacy.jpg");
        FileResourceVO file = new FileResourceVO();
        file.setId(22L);
        file.setFileUrl("/api/public-files/22/content");
        when(herbAtlasMapper.selectLegacyFileCandidates()).thenReturn(List.of(atlas));
        when(fileResourceService.registerPublic(
                        "/api/files/uploads/2026-07-11/legacy.jpg", "legacy.jpg", "历史图谱文件资源迁移"))
                .thenReturn(file);
        when(herbAtlasMapper.updateImageUrl(101L, "/api/public-files/22/content")).thenReturn(1);

        int migrated = herbAtlasImportService.reconcileFileResources();

        assertThat(migrated).isEqualTo(1);
        verify(fileBusinessService).bindSystem(any());
        verify(herbAtlasMapper).updateImageUrl(101L, "/api/public-files/22/content");
    }

    @Test
    void importAtlasSkipsDuplicateImageName() throws Exception {
        Path imageDir = importRoot.resolve("huanglian").resolve("root_mature_dried");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_001.jpg"), new byte[] {1});

        HerbEntity species = species(1L, "HUANGLIAN", "Huanglian", null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_001.jpg")).thenReturn(1);

        HerbAtlasImportResultVO result =
                herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getSkipCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("skip");
    }

    @Test
    void importAtlasRecordsFailureWhenSpeciesCannotBeMatched() throws Exception {
        Path imageDir = importRoot.resolve("unknown_species").resolve("whole_growth_fresh");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("unknown_001.png"), new byte[] {1});
        when(herbSpeciesMapper.selectByHerbCode("UNKNOWN_SPECIES")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_UNKNOWN_SPECIES")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("UNKNOWN")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_UNKNOWN")).thenReturn(null);
        when(herbSpeciesMapper.selectByNameOrAlias("unknown species")).thenReturn(null);

        HerbAtlasImportResultVO result =
                herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getMessage()).contains("Species not matched");
    }

    @Test
    void importAtlasCreatesKnownSpeciesWhenMissingBeforeImportingImages() throws Exception {
        Path imageDir =
                importRoot.resolve("dangshen_codonopsis_pilosula").resolve("root_mature_dried");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("dangshen_root_dried_01.jpeg"), new byte[] {1});
        when(herbSpeciesMapper.selectByHerbCode("DANGSHEN_CODONOPSIS_PILOSULA")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_DANGSHEN_CODONOPSIS_PILOSULA"))
                .thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("DANGSHEN")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_DANGSHEN")).thenReturn(null);
        when(herbSpeciesMapper.selectByNameOrAlias("dangshen codonopsis pilosula"))
                .thenReturn(null);
        doAnswer(
                        invocation -> {
                            HerbEntity species = invocation.getArgument(0);
                            species.setId(5L);
                            return 1;
                        })
                .when(herbSpeciesMapper)
                .insertSpecies(any(HerbEntity.class));
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(5L, "dangshen_root_dried_01.jpeg"))
                .thenReturn(0);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_DANGSHEN_001")).thenReturn(0);

        HerbAtlasImportResultVO result =
                herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getSuccessCount()).isEqualTo(1);
        ArgumentCaptor<HerbEntity> speciesCaptor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).insertSpecies(speciesCaptor.capture());
        HerbEntity species = speciesCaptor.getValue();
        assertThat(species.getHerbNo()).isEqualTo("HERB_DANGSHEN");
        assertThat(species.getHerbName()).isEqualTo("党参");
        assertThat(species.getLatinName()).isEqualTo("Codonopsis pilosula");
        assertThat(species.getStatus()).isEqualTo(1);

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        assertThat(atlasCaptor.getValue().getSpeciesId()).isEqualTo(5L);
        assertThat(atlasCaptor.getValue().getAtlasNo()).isEqualTo("ATLAS_DANGSHEN_001");
    }

    @Test
    void importAtlasIncrementsAtlasCodeWhenCodeExists() throws Exception {
        Path imageDir = importRoot.resolve("huanglian").resolve("whole_growth_fresh");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_002.webp"), new byte[] {1});

        HerbEntity species = species(1L, "HUANGLIAN", "Huanglian", null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_002.webp")).thenReturn(0);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_001")).thenReturn(1);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_002")).thenReturn(0);

        herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        assertThat(atlasCaptor.getValue().getAtlasNo()).isEqualTo("ATLAS_HUANGLIAN_002");
    }

    @Test
    void importAtlasSupportsHerbCodePrefixWhenMatchingDirectory() throws Exception {
        Path imageDir =
                importRoot.resolve("huanglian_coptis_chinensis").resolve("whole_growth_fresh");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_003.png"), new byte[] {1});

        HerbEntity species = species(1L, "HERB_HUANGLIAN", "Huanglian", null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN_COPTIS_CHINENSIS")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN_COPTIS_CHINENSIS"))
                .thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_003.png")).thenReturn(0);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_001")).thenReturn(0);

        herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        SpectrumEntity atlas = atlasCaptor.getValue();
        assertThat(atlas.getAtlasNo()).isEqualTo("ATLAS_HUANGLIAN_001");
        assertThat(atlas.getImageUrl()).isEqualTo("/api/public-files/11/content");
    }

    private HerbEntity species(Long id, String herbCode, String herbName, String medicinalPart) {
        HerbEntity species = new HerbEntity();
        species.setId(id);
        species.setHerbNo(herbCode);
        species.setHerbName(herbName);
        species.setMedicinalPart(medicinalPart);
        return species;
    }
}
