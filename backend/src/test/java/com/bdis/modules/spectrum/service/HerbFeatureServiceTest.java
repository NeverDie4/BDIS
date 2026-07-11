package com.bdis.modules.spectrum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.FileStorageException;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.spectrum.client.FeatureExtractionClient;
import com.bdis.modules.spectrum.client.FeatureExtractionClientResponse;
import com.bdis.modules.spectrum.config.HerbFeatureProperties;
import com.bdis.modules.spectrum.dto.AtlasFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.dto.ImageFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.entity.HerbAtlasFeatureEntity;
import com.bdis.modules.spectrum.entity.HerbImageFeatureEntity;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.service.impl.HerbFeatureServiceImpl;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractResultVO;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbFeatureServiceTest {

    @TempDir private Path uploadRoot;

    @Mock private HerbAtlasMapper herbAtlasMapper;

    @Mock private HerbImageMapper herbImageMapper;

    @Mock private HerbAtlasFeatureMapper herbAtlasFeatureMapper;

    @Mock private HerbImageFeatureMapper herbImageFeatureMapper;

    @Mock private FeatureExtractionClient featureExtractionClient;

    @Mock private FileResourceService fileResourceService;

    private HerbFeatureService herbFeatureService;

    @BeforeEach
    void setUp() {
        lenient()
                .when(fileResourceService.resolveLocalPath(anyString()))
                .thenAnswer(
                        invocation -> {
                            String fileUrl = invocation.getArgument(0);
                            Path resolved = uploadRoot.resolve(fileUrl.replaceFirst("^/", ""));
                            if (!Files.isRegularFile(resolved)) {
                                throw new FileStorageException("文件不存在或已不可访问");
                            }
                            return resolved;
                        });
        herbFeatureService =
                new HerbFeatureServiceImpl(
                        herbAtlasMapper,
                        herbImageMapper,
                        herbAtlasFeatureMapper,
                        herbImageFeatureMapper,
                        featureExtractionClient,
                        fileResourceService,
                        new ObjectMapper(),
                        featureProperties());
    }

    @Test
    void extractAtlasFeatureSavesSuccessfulVector() throws Exception {
        SpectrumEntity atlas = atlas(1L, "ATLAS_1", "/herb/atlas/a.jpg");
        copyFixture("wuzhimaotao_field.jpg", uploadRoot.resolve("herb/atlas/a.jpg"));
        when(herbAtlasMapper.selectActiveById(1L)).thenReturn(atlas);
        when(featureExtractionClient.extract(any())).thenReturn(successResponse());

        FeatureExtractResultVO result = herbFeatureService.extractAtlasFeature(1L);

        assertThat(result.getTargetType()).isEqualTo("atlas");
        assertThat(result.getTargetId()).isEqualTo(1L);
        assertThat(result.getFeatureDimension()).isEqualTo(3);
        assertThat(result.getExtractStatus()).isEqualTo("success");
        assertThat(result.getFeatureVector()).isNull();
        ArgumentCaptor<HerbAtlasFeatureEntity> captor =
                ArgumentCaptor.forClass(HerbAtlasFeatureEntity.class);
        verify(herbAtlasFeatureMapper)
                .deleteActiveByAtlasIdAndModel(any(), anyString(), anyString());
        verify(herbAtlasFeatureMapper).insertFeature(captor.capture());
        assertThat(captor.getValue().getFeatureVector()).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    void extractImageFeatureRejectsMissingLocalFile() {
        HerbImageEntity image = image(2L, "/herb/image/missing.jpg");
        when(herbImageMapper.selectActiveById(2L)).thenReturn(image);

        assertThatThrownBy(() -> herbFeatureService.extractImageFeature(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件不存在");

        verify(featureExtractionClient, never()).extract(any());
        ArgumentCaptor<HerbImageFeatureEntity> captor =
                ArgumentCaptor.forClass(HerbImageFeatureEntity.class);
        verify(herbImageFeatureMapper).insertFeature(captor.capture());
        assertThat(captor.getValue().getExtractStatus()).isEqualTo("failed");
        assertThat(captor.getValue().getErrorMessage()).contains("文件不存在");
    }

    @Test
    void batchExtractSkipsExistingSuccessfulFeatureWhenForceRefreshIsFalse() {
        AtlasFeatureBatchExtractRequest request = new AtlasFeatureBatchExtractRequest();
        request.setForceRefresh(false);
        SpectrumEntity atlas = atlas(1L, "ATLAS_1", "/herb/atlas/a.jpg");
        when(herbAtlasMapper.selectEnabledForFeatureExtraction(null)).thenReturn(List.of(atlas));
        when(herbAtlasFeatureMapper.existsSuccessByAtlasIdAndModel(1L, "resnet50", "v1.0"))
                .thenReturn(1);

        FeatureBatchExtractResultVO result = herbFeatureService.batchExtractAtlasFeatures(request);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSkipCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("skip");
        verify(featureExtractionClient, never()).extract(any());
    }

    @Test
    void batchExtractContinuesWhenOneAtlasFails() throws Exception {
        AtlasFeatureBatchExtractRequest request = new AtlasFeatureBatchExtractRequest();
        request.setForceRefresh(true);
        SpectrumEntity missing = atlas(1L, "ATLAS_1", "/herb/atlas/missing.jpg");
        SpectrumEntity ok = atlas(2L, "ATLAS_2", "/herb/atlas/ok.jpg");
        copyFixture("wuzhimaotao_field.jpg", uploadRoot.resolve("herb/atlas/ok.jpg"));
        when(herbAtlasMapper.selectEnabledForFeatureExtraction(null))
                .thenReturn(List.of(missing, ok));
        when(featureExtractionClient.extract(any())).thenReturn(successResponse());

        FeatureBatchExtractResultVO result = herbFeatureService.batchExtractAtlasFeatures(request);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getItems()).extracting("status").containsExactly("failed", "success");
    }

    @Test
    void batchExtractImageFeaturesSavesMissingFeature() throws Exception {
        ImageFeatureBatchExtractRequest request = new ImageFeatureBatchExtractRequest();
        request.setForceRefresh(false);
        HerbImageEntity image = image(2L, "/herb/image/ok.png");
        image.setImageNo("IMG_2");
        image.setOriginalFilename("ok.png");
        copyFixture("gouqi_fruit.png", uploadRoot.resolve("herb/image/ok.png"));
        when(herbImageMapper.selectActiveForFeatureExtraction(null)).thenReturn(List.of(image));
        when(herbImageFeatureMapper.existsSuccessByImageIdAndModel(2L, "resnet50", "v1.0"))
                .thenReturn(0);
        when(herbImageMapper.selectActiveById(2L)).thenReturn(image);
        when(featureExtractionClient.extract(any())).thenReturn(successResponse());

        FeatureBatchExtractResultVO result = herbFeatureService.batchExtractImageFeatures(request);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getImageId()).isEqualTo(2L);
        ArgumentCaptor<HerbImageFeatureEntity> captor =
                ArgumentCaptor.forClass(HerbImageFeatureEntity.class);
        verify(herbImageFeatureMapper).insertFeature(captor.capture());
        assertThat(captor.getValue().getImageId()).isEqualTo(2L);
        assertThat(captor.getValue().getFeatureVector()).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    void getAtlasFeatureOmitsVectorByDefaultAndIncludesWhenRequested() {
        FeatureExtractResultVO saved = new FeatureExtractResultVO();
        saved.setTargetType("atlas");
        saved.setTargetId(1L);
        saved.setFeatureVector("[0.1,0.2]");
        when(herbAtlasFeatureMapper.selectLatestByAtlasId(1L)).thenReturn(saved);

        assertThat(herbFeatureService.getAtlasFeature(1L, false).getFeatureVector()).isNull();
        assertThat(herbFeatureService.getAtlasFeature(1L, true).getFeatureVector())
                .isEqualTo("[0.1,0.2]");
    }

    private FeatureExtractionClientResponse successResponse() {
        FeatureExtractionClientResponse response = new FeatureExtractionClientResponse();
        response.setSuccess(true);
        response.setFeatureVector(List.of(0.1D, 0.2D, 0.3D));
        response.setDimension(3);
        response.setModelName("resnet50");
        response.setModelVersion("v1.0");
        response.setMessage("Feature extracted successfully");
        return response;
    }

    private HerbFeatureProperties featureProperties() {
        HerbFeatureProperties properties = new HerbFeatureProperties();
        properties.setModelName("resnet50");
        properties.setModelVersion("v1.0");
        return properties;
    }

    private SpectrumEntity atlas(Long id, String atlasCode, String imageUrl) {
        SpectrumEntity atlas = new SpectrumEntity();
        atlas.setId(id);
        atlas.setAtlasNo(atlasCode);
        atlas.setSpeciesId(9L);
        atlas.setAtlasTitle(atlasCode + ".jpg");
        atlas.setImageUrl(imageUrl);
        return atlas;
    }

    private HerbImageEntity image(Long id, String imageUrl) {
        HerbImageEntity image = new HerbImageEntity();
        image.setId(id);
        image.setSpeciesId(9L);
        image.setImageUrl(imageUrl);
        return image;
    }

    private void copyFixture(String filename, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        try (InputStream input =
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/herb/" + filename))) {
            Files.copy(input, target);
        }
    }
}
