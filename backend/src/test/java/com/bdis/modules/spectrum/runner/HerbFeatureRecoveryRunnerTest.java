package com.bdis.modules.spectrum.runner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.modules.spectrum.client.FeatureExtractionClient;
import com.bdis.modules.spectrum.config.HerbFeatureProperties;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbFeatureRecoveryRunnerTest {

    @Mock private HerbFeatureService herbFeatureService;

    @Mock private FeatureExtractionClient featureExtractionClient;

    private HerbFeatureProperties properties;
    private HerbFeatureRecoveryRunner runner;

    @BeforeEach
    void setUp() {
        properties = new HerbFeatureProperties();
        properties.setEnabled(true);
        properties.setRecoveryEnabled(true);
        runner =
                new HerbFeatureRecoveryRunner(
                        herbFeatureService, properties, featureExtractionClient);
    }

    @Test
    void recoveryWaitsWhenFeatureServiceIsUnavailable() {
        when(featureExtractionClient.isAvailable()).thenReturn(false);

        runner.recoverMissingFeatures();

        verify(herbFeatureService, never()).batchExtractAtlasFeatures(any());
        verify(herbFeatureService, never()).batchExtractImageFeatures(any());
    }

    @Test
    void recoveryProcessesMissingAtlasAndImageFeaturesWhenHealthy() {
        when(featureExtractionClient.isAvailable()).thenReturn(true);
        when(herbFeatureService.batchExtractAtlasFeatures(any()))
                .thenReturn(new FeatureBatchExtractResultVO());
        when(herbFeatureService.batchExtractImageFeatures(any()))
                .thenReturn(new FeatureBatchExtractResultVO());

        runner.recoverMissingFeatures();

        verify(herbFeatureService).batchExtractAtlasFeatures(any());
        verify(herbFeatureService).batchExtractImageFeatures(any());
    }
}
