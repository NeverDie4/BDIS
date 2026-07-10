package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileStorageService;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.spectrum.client.FeatureExtractionClient;
import com.bdis.modules.spectrum.client.FeatureExtractionClientResponse;
import com.bdis.modules.spectrum.config.HerbFeatureProperties;
import com.bdis.modules.spectrum.constant.HerbProcessStatusConstants;
import com.bdis.modules.spectrum.dto.AtlasFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.dto.ImageFeatureBatchExtractRequest;
import com.bdis.modules.spectrum.entity.HerbAtlasFeatureEntity;
import com.bdis.modules.spectrum.entity.HerbImageFeatureEntity;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasFeatureMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbImageFeatureMapper;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractItemVO;
import com.bdis.modules.spectrum.vo.FeatureBatchExtractResultVO;
import com.bdis.modules.spectrum.vo.FeatureExtractResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class HerbFeatureServiceImpl implements HerbFeatureService {

    private static final String STATUS_SUCCESS = "success";

    private final HerbAtlasMapper herbAtlasMapper;
    private final HerbImageMapper herbImageMapper;
    private final HerbAtlasFeatureMapper herbAtlasFeatureMapper;
    private final HerbImageFeatureMapper herbImageFeatureMapper;
    private final FeatureExtractionClient featureExtractionClient;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final HerbFeatureProperties properties;

    public HerbFeatureServiceImpl(
            HerbAtlasMapper herbAtlasMapper,
            HerbImageMapper herbImageMapper,
            HerbAtlasFeatureMapper herbAtlasFeatureMapper,
            HerbImageFeatureMapper herbImageFeatureMapper,
            FeatureExtractionClient featureExtractionClient,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper,
            HerbFeatureProperties properties) {
        this.herbAtlasMapper = herbAtlasMapper;
        this.herbImageMapper = herbImageMapper;
        this.herbAtlasFeatureMapper = herbAtlasFeatureMapper;
        this.herbImageFeatureMapper = herbImageFeatureMapper;
        this.featureExtractionClient = featureExtractionClient;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    @Transactional
    public FeatureExtractResultVO extractAtlasFeature(Long atlasId) {
        SpectrumEntity atlas = getActiveAtlas(atlasId);
        return extractAtlasFeature(atlas);
    }

    private FeatureExtractResultVO extractAtlasFeature(SpectrumEntity atlas) {
        Path imagePath = fileStorageService.resolve(atlas.getImageUrl());
        FeatureExtractionClientResponse response = extractValidFeature(imagePath);
        HerbAtlasFeatureEntity feature = buildAtlasFeature(atlas, response);
        herbAtlasFeatureMapper.deleteActiveByAtlasIdAndModel(
                atlas.getId(), feature.getFeatureModel(), feature.getFeatureVersion());
        herbAtlasFeatureMapper.insertFeature(feature);
        return toAtlasVO(feature, "Feature extracted successfully", false);
    }

    @Override
    public FeatureBatchExtractResultVO batchExtractAtlasFeatures(
            AtlasFeatureBatchExtractRequest request) {
        AtlasFeatureBatchExtractRequest safeRequest =
                request == null ? new AtlasFeatureBatchExtractRequest() : request;
        boolean forceRefresh = Boolean.TRUE.equals(safeRequest.getForceRefresh());
        FeatureBatchExtractResultVO result = new FeatureBatchExtractResultVO();
        for (SpectrumEntity atlas :
                herbAtlasMapper.selectEnabledForFeatureExtraction(safeRequest.getSpeciesId())) {
            FeatureBatchExtractItemVO item = batchItem(atlas);
            try {
                if (!forceRefresh && hasSuccessfulAtlasFeature(atlas.getId())) {
                    item.setMessage("Feature already exists");
                    result.addSkip(item);
                    continue;
                }
                extractAtlasFeature(atlas);
                item.setMessage("Feature extracted successfully");
                result.addSuccess(item);
            } catch (RuntimeException exception) {
                item.setMessage(exception.getMessage());
                result.addFail(item);
            }
        }
        return result;
    }

    @Override
    public FeatureBatchExtractResultVO batchExtractImageFeatures(
            ImageFeatureBatchExtractRequest request) {
        ImageFeatureBatchExtractRequest safeRequest =
                request == null ? new ImageFeatureBatchExtractRequest() : request;
        boolean forceRefresh = Boolean.TRUE.equals(safeRequest.getForceRefresh());
        FeatureBatchExtractResultVO result = new FeatureBatchExtractResultVO();
        for (HerbImageEntity image :
                herbImageMapper.selectActiveForFeatureExtraction(safeRequest.getSpeciesId())) {
            FeatureBatchExtractItemVO item = imageBatchItem(image);
            try {
                if (!forceRefresh && hasSuccessfulImageFeature(image.getId())) {
                    item.setMessage("Feature already exists");
                    result.addSkip(item);
                    continue;
                }
                extractImageFeature(image.getId());
                item.setMessage("Feature extracted successfully");
                result.addSuccess(item);
            } catch (RuntimeException exception) {
                item.setMessage(exception.getMessage());
                result.addFail(item);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public FeatureExtractResultVO extractImageFeature(Long imageId) {
        HerbImageEntity image = getActiveImage(imageId);
        Path imagePath = fileStorageService.resolve(image.getImageUrl());
        FeatureExtractionClientResponse response = extractValidFeature(imagePath);
        HerbImageFeatureEntity feature = buildImageFeature(image, response);
        herbImageFeatureMapper.deleteActiveByImageIdAndModel(
                image.getId(), feature.getFeatureModel(), feature.getFeatureVersion());
        herbImageFeatureMapper.insertFeature(feature);
        updateImageProcessStatus(image.getId(), HerbProcessStatusConstants.FEATURE_EXTRACTED);
        return toImageVO(feature, "Feature extracted successfully", false);
    }

    @Override
    public FeatureExtractResultVO getAtlasFeature(Long atlasId, boolean includeVector) {
        FeatureExtractResultVO vo = herbAtlasFeatureMapper.selectLatestSuccessByAtlasId(atlasId);
        if (vo == null) {
            throw new BusinessException("Atlas feature not found");
        }
        return stripVector(vo, includeVector);
    }

    @Override
    public FeatureExtractResultVO getImageFeature(Long imageId, boolean includeVector) {
        FeatureExtractResultVO vo = herbImageFeatureMapper.selectLatestSuccessByImageId(imageId);
        if (vo == null) {
            throw new BusinessException("Image feature not found");
        }
        return stripVector(vo, includeVector);
    }

    private boolean hasSuccessfulAtlasFeature(Long atlasId) {
        return herbAtlasFeatureMapper.existsSuccessByAtlasIdAndModel(
                        atlasId, properties.getModelName(), properties.getModelVersion())
                > 0;
    }

    private boolean hasSuccessfulImageFeature(Long imageId) {
        return herbImageFeatureMapper.existsSuccessByImageIdAndModel(
                        imageId, properties.getModelName(), properties.getModelVersion())
                > 0;
    }

    private FeatureExtractionClientResponse extractValidFeature(Path imagePath) {
        FeatureExtractionClientResponse response = featureExtractionClient.extract(imagePath);
        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            throw new BusinessException(
                    response == null ? "Feature extraction failed" : response.getMessage());
        }
        if (CollectionUtils.isEmpty(response.getFeatureVector())
                || response.getDimension() == null
                || response.getDimension() <= 0) {
            throw new BusinessException("Feature vector is empty");
        }
        return response;
    }

    private SpectrumEntity getActiveAtlas(Long atlasId) {
        if (atlasId == null) {
            throw new BusinessException("Atlas id is required");
        }
        SpectrumEntity atlas = herbAtlasMapper.selectActiveById(atlasId);
        if (atlas == null) {
            throw new BusinessException("Herb atlas not found");
        }
        return atlas;
    }

    private HerbImageEntity getActiveImage(Long imageId) {
        if (imageId == null) {
            throw new BusinessException("Image id is required");
        }
        HerbImageEntity image = herbImageMapper.selectActiveById(imageId);
        if (image == null) {
            throw new BusinessException("Herb image not found");
        }
        return image;
    }

    private HerbAtlasFeatureEntity buildAtlasFeature(
            SpectrumEntity atlas, FeatureExtractionClientResponse response) {
        LocalDateTime now = LocalDateTime.now();
        HerbAtlasFeatureEntity feature = new HerbAtlasFeatureEntity();
        feature.setAtlasId(atlas.getId());
        feature.setSpeciesId(atlas.getSpeciesId());
        fillCommonFeature(feature, response, now);
        return feature;
    }

    private HerbImageFeatureEntity buildImageFeature(
            HerbImageEntity image, FeatureExtractionClientResponse response) {
        LocalDateTime now = LocalDateTime.now();
        HerbImageFeatureEntity feature = new HerbImageFeatureEntity();
        feature.setImageId(image.getId());
        feature.setSpeciesId(image.getSpeciesId());
        fillCommonFeature(feature, response, now);
        return feature;
    }

    private void fillCommonFeature(
            HerbAtlasFeatureEntity feature,
            FeatureExtractionClientResponse response,
            LocalDateTime now) {
        feature.setFeatureCode(featureCode(response));
        feature.setFeatureVector(toJson(response));
        feature.setFeatureDimension(response.getDimension());
        feature.setFeatureModel(response.getModelName());
        feature.setFeatureVersion(response.getModelVersion());
        feature.setExtractStatus(STATUS_SUCCESS);
        feature.setExtractTime(now);
        feature.setCreatedAt(now);
        feature.setUpdatedAt(now);
        feature.setIsDeleted(0);
        feature.setStatus(1);
        feature.setVersion(0);
    }

    private void fillCommonFeature(
            HerbImageFeatureEntity feature,
            FeatureExtractionClientResponse response,
            LocalDateTime now) {
        feature.setFeatureCode(featureCode(response));
        feature.setFeatureVector(toJson(response));
        feature.setFeatureDimension(response.getDimension());
        feature.setFeatureModel(response.getModelName());
        feature.setFeatureVersion(response.getModelVersion());
        feature.setExtractStatus(STATUS_SUCCESS);
        feature.setExtractTime(now);
        feature.setCreatedAt(now);
        feature.setUpdatedAt(now);
        feature.setIsDeleted(0);
        feature.setStatus(1);
        feature.setVersion(0);
    }

    private String featureCode(FeatureExtractionClientResponse response) {
        return response.getModelName() + "_" + response.getModelVersion();
    }

    private String toJson(FeatureExtractionClientResponse response) {
        try {
            return objectMapper.writeValueAsString(response.getFeatureVector());
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Failed to serialize feature vector");
        }
    }

    private FeatureExtractResultVO toAtlasVO(
            HerbAtlasFeatureEntity feature, String message, boolean includeVector) {
        FeatureExtractResultVO vo = new FeatureExtractResultVO();
        vo.setTargetType("atlas");
        vo.setTargetId(feature.getAtlasId());
        vo.setAtlasId(feature.getAtlasId());
        vo.setSpeciesId(feature.getSpeciesId());
        fillVO(
                vo,
                feature.getFeatureCode(),
                feature.getFeatureVector(),
                feature.getFeatureDimension(),
                feature.getFeatureModel(),
                feature.getFeatureVersion(),
                feature.getExtractStatus(),
                feature.getExtractTime(),
                message,
                includeVector);
        return vo;
    }

    private FeatureExtractResultVO toImageVO(
            HerbImageFeatureEntity feature, String message, boolean includeVector) {
        FeatureExtractResultVO vo = new FeatureExtractResultVO();
        vo.setTargetType("image");
        vo.setTargetId(feature.getImageId());
        vo.setImageId(feature.getImageId());
        vo.setSpeciesId(feature.getSpeciesId());
        fillVO(
                vo,
                feature.getFeatureCode(),
                feature.getFeatureVector(),
                feature.getFeatureDimension(),
                feature.getFeatureModel(),
                feature.getFeatureVersion(),
                feature.getExtractStatus(),
                feature.getExtractTime(),
                message,
                includeVector);
        return vo;
    }

    private void fillVO(
            FeatureExtractResultVO vo,
            String featureCode,
            String featureVector,
            Integer featureDimension,
            String featureModel,
            String featureVersion,
            String extractStatus,
            LocalDateTime extractTime,
            String message,
            boolean includeVector) {
        vo.setFeatureCode(featureCode);
        vo.setFeatureVector(includeVector ? featureVector : null);
        vo.setFeatureDimension(featureDimension);
        vo.setFeatureModel(featureModel);
        vo.setFeatureVersion(featureVersion);
        vo.setExtractStatus(extractStatus);
        vo.setExtractTime(extractTime);
        vo.setMessage(message);
    }

    private FeatureExtractResultVO stripVector(FeatureExtractResultVO vo, boolean includeVector) {
        FeatureExtractResultVO result = new FeatureExtractResultVO();
        result.setTargetType(vo.getTargetType());
        result.setTargetId(vo.getTargetId());
        result.setAtlasId(vo.getAtlasId());
        result.setImageId(vo.getImageId());
        result.setSpeciesId(vo.getSpeciesId());
        result.setFeatureCode(vo.getFeatureCode());
        result.setFeatureDimension(vo.getFeatureDimension());
        result.setFeatureModel(vo.getFeatureModel());
        result.setFeatureVersion(vo.getFeatureVersion());
        result.setExtractStatus(vo.getExtractStatus());
        result.setExtractTime(vo.getExtractTime());
        result.setMessage(vo.getMessage());
        result.setErrorMessage(vo.getErrorMessage());
        if (!includeVector) {
            result.setFeatureVector(null);
        } else {
            result.setFeatureVector(vo.getFeatureVector());
        }
        return result;
    }

    private FeatureBatchExtractItemVO batchItem(SpectrumEntity atlas) {
        FeatureBatchExtractItemVO item = new FeatureBatchExtractItemVO();
        item.setAtlasId(atlas.getId());
        item.setAtlasCode(atlas.getAtlasNo());
        item.setImageName(atlas.getAtlasTitle());
        item.setSpeciesId(atlas.getSpeciesId());
        return item;
    }

    private FeatureBatchExtractItemVO imageBatchItem(HerbImageEntity image) {
        FeatureBatchExtractItemVO item = new FeatureBatchExtractItemVO();
        item.setImageId(image.getId());
        item.setImageCode(image.getImageNo());
        item.setImageName(image.getOriginalFilename());
        item.setSpeciesId(image.getSpeciesId());
        return item;
    }

    private void updateImageProcessStatus(Long imageId, String processStatus) {
        HerbImageEntity update = new HerbImageEntity();
        update.setId(imageId);
        update.setProcessStatus(processStatus);
        update.setUpdatedAt(LocalDateTime.now());
        herbImageMapper.updateProcessStatus(update);
    }
}
