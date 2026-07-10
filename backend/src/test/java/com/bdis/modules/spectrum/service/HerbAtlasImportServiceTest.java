package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.storage.LocalFileStorage;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.client.HerbFeatureVectorClient;
import com.bdis.modules.spectrum.dto.HerbAtlasImportRequest;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.entity.SpectrumTagEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasTagMapper;
import com.bdis.modules.spectrum.service.impl.HerbAtlasImportServiceImpl;
import com.bdis.modules.spectrum.vo.HerbAtlasImportResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbAtlasImportServiceTest {

    @TempDir private Path workspace;

    @Mock private HerbAtlasMapper herbAtlasMapper;

    @Mock private HerbAtlasTagMapper herbAtlasTagMapper;

    @Mock private HerbSpeciesMapper herbSpeciesMapper;

    @Mock private HerbFeatureVectorClient featureVectorClient;

    private HerbAtlasImportService herbAtlasImportService;

    private Path importRoot;

    private Path uploadRoot;

    @BeforeEach
    void setUp() throws Exception {
        importRoot = workspace.resolve("import").resolve("herb_atlas");
        uploadRoot = workspace.resolve("uploads");
        herbAtlasImportService =
                new HerbAtlasImportServiceImpl(
                        herbAtlasMapper,
                        herbAtlasTagMapper,
                        herbSpeciesMapper,
                        new LocalFileStorage(uploadRoot.toString()),
                        featureVectorClient,
                        new ObjectMapper(),
                        importRoot.toString(),
                        false);
    }

    @Test
    void importAtlasCopiesImagesAndInsertsAtlasWithDefaultTags() throws Exception {
        Path imageDir =
                importRoot
                        .resolve("huanglian_coptis_chinensis")
                        .resolve("leaf_growth_fresh");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_001.jpg"), new byte[] {1, 2, 3});
        Files.writeString(imageDir.resolve("notes.txt"), "ignored");

        HerbEntity species = species(1L, "HUANGLIAN", "Huanglian", "rhizome");
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN_COPTIS_CHINENSIS")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_HUANGLIAN_COPTIS_CHINENSIS"))
                .thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_001.jpg"))
                .thenReturn(0);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_001")).thenReturn(0);
        when(featureVectorClient.extract(any())).thenReturn(List.of(0.1D, 0.2D));

        HerbAtlasImportResultVO result = herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getSkipCount()).isZero();
        assertThat(result.getFailCount()).isZero();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("success");

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        SpectrumEntity atlas = atlasCaptor.getValue();
        assertThat(atlas.getAtlasCode()).isEqualTo("ATLAS_HUANGLIAN_001");
        assertThat(atlas.getImageName()).isEqualTo("huanglian_001.jpg");
        assertThat(atlas.getImageType()).isEqualTo("standard");
        assertThat(atlas.getFeatureVector()).isEqualTo("[0.1,0.2]");
        assertThat(atlas.getFeatureDim()).isEqualTo(2);
        assertThat(atlas.getGrowthStage()).isEqualTo("unknown");
        assertThat(atlas.getMedicinalPart()).isEqualTo("rhizome");
        assertThat(atlas.getImageUrl()).isEqualTo("/herb/atlas/huanglian/huanglian_001.jpg");
        assertThat(Files.exists(uploadRoot.resolve("herb/atlas/huanglian/huanglian_001.jpg")))
                .isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SpectrumTagEntity>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(herbAtlasTagMapper).insertTags(tagsCaptor.capture());
        assertThat(tagsCaptor.getValue()).extracting(SpectrumTagEntity::getTagName)
                .containsExactly("standard", "batch_import", "huanglian");
    }

    @Test
    void importAtlasSkipsDuplicateImageName() throws Exception {
        Path imageDir = importRoot.resolve("huanglian").resolve("root_mature_dried");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_001.jpg"), new byte[] {1});

        HerbEntity species = species(1L, "HUANGLIAN", "Huanglian", null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_001.jpg"))
                .thenReturn(1);

        HerbAtlasImportResultVO result = herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

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

        HerbAtlasImportResultVO result = herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getMessage()).contains("Species not matched");
    }

    @Test
    void importAtlasCreatesKnownSpeciesWhenMissingBeforeImportingImages() throws Exception {
        Path imageDir = importRoot.resolve("dangshen_codonopsis_pilosula").resolve("root_mature_dried");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("dangshen_root_dried_01.jpeg"), new byte[] {1});
        when(herbSpeciesMapper.selectByHerbCode("DANGSHEN_CODONOPSIS_PILOSULA"))
                .thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_DANGSHEN_CODONOPSIS_PILOSULA"))
                .thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("DANGSHEN")).thenReturn(null);
        when(herbSpeciesMapper.selectByHerbCode("HERB_DANGSHEN")).thenReturn(null);
        when(herbSpeciesMapper.selectByNameOrAlias("dangshen codonopsis pilosula")).thenReturn(null);
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

        HerbAtlasImportResultVO result = herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        assertThat(result.getSuccessCount()).isEqualTo(1);
        ArgumentCaptor<HerbEntity> speciesCaptor = ArgumentCaptor.forClass(HerbEntity.class);
        verify(herbSpeciesMapper).insertSpecies(speciesCaptor.capture());
        HerbEntity species = speciesCaptor.getValue();
        assertThat(species.getHerbCode()).isEqualTo("HERB_DANGSHEN");
        assertThat(species.getHerbName()).isEqualTo("党参");
        assertThat(species.getLatinName()).isEqualTo("Codonopsis pilosula");
        assertThat(species.getStatus()).isEqualTo(1);

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        assertThat(atlasCaptor.getValue().getSpeciesId()).isEqualTo(5L);
        assertThat(atlasCaptor.getValue().getAtlasCode()).isEqualTo("ATLAS_DANGSHEN_001");
    }

    @Test
    void importAtlasIncrementsAtlasCodeWhenCodeExists() throws Exception {
        Path imageDir = importRoot.resolve("huanglian").resolve("whole_growth_fresh");
        Files.createDirectories(imageDir);
        Files.write(imageDir.resolve("huanglian_002.webp"), new byte[] {1});

        HerbEntity species = species(1L, "HUANGLIAN", "Huanglian", null);
        when(herbSpeciesMapper.selectByHerbCode("HUANGLIAN")).thenReturn(species);
        when(herbAtlasMapper.existsBySpeciesIdAndImageName(1L, "huanglian_002.webp"))
                .thenReturn(0);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_001")).thenReturn(1);
        when(herbAtlasMapper.countByAtlasCode("ATLAS_HUANGLIAN_002")).thenReturn(0);

        herbAtlasImportService.importAtlas(new HerbAtlasImportRequest());

        ArgumentCaptor<SpectrumEntity> atlasCaptor = ArgumentCaptor.forClass(SpectrumEntity.class);
        verify(herbAtlasMapper).insertAtlas(atlasCaptor.capture());
        assertThat(atlasCaptor.getValue().getAtlasCode()).isEqualTo("ATLAS_HUANGLIAN_002");
    }

    @Test
    void importAtlasSupportsHerbCodePrefixWhenMatchingDirectory() throws Exception {
        Path imageDir = importRoot.resolve("huanglian_coptis_chinensis").resolve("whole_growth_fresh");
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
        assertThat(atlas.getAtlasCode()).isEqualTo("ATLAS_HUANGLIAN_001");
        assertThat(atlas.getImageUrl()).isEqualTo("/herb/atlas/huanglian/huanglian_003.png");
    }

    private HerbEntity species(Long id, String herbCode, String herbName, String medicinalPart) {
        HerbEntity species = new HerbEntity();
        species.setId(id);
        species.setHerbCode(herbCode);
        species.setHerbName(herbName);
        species.setMedicinalPart(medicinalPart);
        return species;
    }
}
