package com.bdis.modules.spectrum.runner;

import com.bdis.modules.spectrum.client.FeatureExtractionClient;
import com.bdis.modules.spectrum.config.HerbFeatureProperties;
import com.bdis.modules.spectrum.dto.AtlasFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.dto.ImageFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HerbFeatureRecoveryRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(HerbFeatureRecoveryRunner.class);

    private final HerbFeatureService herbFeatureService;
    private final HerbFeatureProperties properties;
    private final FeatureExtractionClient featureExtractionClient;

    public HerbFeatureRecoveryRunner(
            HerbFeatureService herbFeatureService,
            HerbFeatureProperties properties,
            FeatureExtractionClient featureExtractionClient) {
        this.herbFeatureService = herbFeatureService;
        this.properties = properties;
        this.featureExtractionClient = featureExtractionClient;
    }

    @Scheduled(
            initialDelayString = "${herb.feature.recovery-initial-delay-ms:5000}",
            fixedDelayString = "${herb.feature.recovery-interval-ms:60000}")
    public void recoverMissingFeatures() {
        if (!properties.isEnabled() || !properties.isRecoveryEnabled()) {
            return;
        }
        if (!featureExtractionClient.isAvailable()) {
            LOGGER.warn("Feature service is unavailable; recovery will retry later");
            return;
        }
        AtlasFeatureBatchExtractRequest atlasRequest = new AtlasFeatureBatchExtractRequest();
        atlasRequest.setForceRefresh(false);
        ImageFeatureBatchExtractRequest imageRequest = new ImageFeatureBatchExtractRequest();
        imageRequest.setForceRefresh(false);
        try {
            FeatureBatchExtractResultVO atlasResult =
                    herbFeatureService.batchExtractAtlasFeatures(atlasRequest);
            FeatureBatchExtractResultVO imageResult =
                    herbFeatureService.batchExtractImageFeatures(imageRequest);
            LOGGER.info(
                    "Feature recovery completed: atlas success={}, skipped={}, failed={}; image"
                            + " success={}, skipped={}, failed={}",
                    atlasResult.getSuccessCount(),
                    atlasResult.getSkipCount(),
                    atlasResult.getFailCount(),
                    imageResult.getSuccessCount(),
                    imageResult.getSkipCount(),
                    imageResult.getFailCount());
        } catch (RuntimeException exception) {
            LOGGER.warn("Feature recovery task failed and will retry later", exception);
        }
    }
}
