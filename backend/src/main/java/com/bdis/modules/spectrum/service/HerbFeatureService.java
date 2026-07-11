package com.bdis.modules.spectrum.service;

import com.bdis.modules.spectrum.dto.AtlasFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.dto.ImageFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractResultVO;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;

public interface HerbFeatureService {

    FeatureExtractResultVO extractAtlasFeature(Long atlasId);

    FeatureBatchExtractResultVO batchExtractAtlasFeatures(AtlasFeatureBatchExtractRequest request);

    FeatureBatchExtractResultVO batchExtractImageFeatures(ImageFeatureBatchExtractRequest request);

    FeatureExtractResultVO extractImageFeature(Long imageId);

    FeatureExtractResultVO getAtlasFeature(Long atlasId, boolean includeVector);

    FeatureExtractResultVO getImageFeature(Long imageId, boolean includeVector);
}
