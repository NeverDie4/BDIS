package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.storage.LocalFileStorage;
import com.bdis.common.storage.LocalFileStorage.StoredFile;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.spectrum.client.HerbFeatureVectorClient;
import com.bdis.modules.spectrum.dto.HerbAtlasQueryRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUpdateRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUploadRequest;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.entity.SpectrumTagEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.mapper.HerbAtlasTagMapper;
import com.bdis.modules.spectrum.service.HerbAtlasService;
import com.bdis.modules.spectrum.vo.HerbAtlasTagVO;
import com.bdis.modules.spectrum.vo.HerbAtlasVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HerbAtlasServiceImpl implements HerbAtlasService {

    private static final DateTimeFormatter ATLAS_CODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HerbAtlasMapper herbAtlasMapper;
    private final HerbAtlasTagMapper herbAtlasTagMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final LocalFileStorage localFileStorage;
    private final HerbFeatureVectorClient featureVectorClient;
    private final ObjectMapper objectMapper;

    public HerbAtlasServiceImpl(
            HerbAtlasMapper herbAtlasMapper,
            HerbAtlasTagMapper herbAtlasTagMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            LocalFileStorage localFileStorage,
            HerbFeatureVectorClient featureVectorClient,
            ObjectMapper objectMapper) {
        this.herbAtlasMapper = herbAtlasMapper;
        this.herbAtlasTagMapper = herbAtlasTagMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.localFileStorage = localFileStorage;
        this.featureVectorClient = featureVectorClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public HerbAtlasVO upload(MultipartFile file, HerbAtlasUploadRequest request) {
        HerbEntity species = getActiveSpecies(request == null ? null : request.getSpeciesId());
        StoredFile storedFile = localFileStorage.saveHerbAtlasImage(file);
        LocalDateTime now = LocalDateTime.now();

        SpectrumEntity atlas = new SpectrumEntity();
        atlas.setSpeciesId(request.getSpeciesId());
        atlas.setAtlasCode(resolveAtlasCode(request.getAtlasCode(), now));
        atlas.setImageUrl(storedFile.url());
        atlas.setImageName(file.getOriginalFilename());
        atlas.setImageType(request.getImageType());
        atlas.setGrowthStage(request.getGrowthStage());
        atlas.setMedicinalPart(request.getMedicinalPart());
        atlas.setSource(request.getSource());
        atlas.setDescription(request.getDescription());
        fillFeatureVector(atlas);
        atlas.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        atlas.setCreateTime(now);
        atlas.setUpdateTime(now);
        atlas.setDeleted(0);
        herbAtlasMapper.insertAtlas(atlas);
        List<HerbAtlasTagVO> tagVOs = saveTags(atlas.getId(), request.getTags(), now);

        HerbAtlasVO vo = toVO(atlas);
        vo.setHerbName(species.getHerbName());
        vo.setTags(tagVOs);
        return vo;
    }

    @Override
    @Transactional
    public HerbAtlasVO update(Long id, HerbAtlasUpdateRequest request) {
        SpectrumEntity atlas = getActiveAtlas(id);
        LocalDateTime now = LocalDateTime.now();
        atlas.setImageType(request.getImageType());
        atlas.setGrowthStage(request.getGrowthStage());
        atlas.setMedicinalPart(request.getMedicinalPart());
        atlas.setSource(request.getSource());
        atlas.setDescription(request.getDescription());
        atlas.setStatus(request.getStatus());
        atlas.setUpdateTime(now);
        herbAtlasMapper.updateAtlas(atlas);

        List<HerbAtlasTagVO> tagVOs = null;
        if (request.getTags() != null) {
            logicalDeleteTags(atlas.getId(), now);
            tagVOs = saveTags(atlas.getId(), request.getTags(), now);
        }
        HerbAtlasVO vo = toVO(atlas);
        if (tagVOs != null) {
            vo.setTags(tagVOs);
        }
        return vo;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SpectrumEntity atlas = getActiveAtlas(id);
        LocalDateTime now = LocalDateTime.now();
        atlas.setUpdateTime(now);
        int affected = herbAtlasMapper.logicalDeleteById(atlas);
        if (affected == 0) {
            throw new BusinessException("Herb atlas not found or already deleted");
        }
        logicalDeleteTags(atlas.getId(), now);
    }

    @Override
    public HerbAtlasVO getById(Long id) {
        if (id == null) {
            throw new BusinessException("Herb atlas id is required");
        }
        HerbAtlasVO vo = herbAtlasMapper.selectDetailById(id);
        if (vo == null) {
            throw new BusinessException("Herb atlas not found");
        }
        vo.setTags(listTags(id));
        return vo;
    }

    @Override
    public PageResult<HerbAtlasVO> page(HerbAtlasQueryRequest request) {
        normalizePageRequest(request);
        Long total = herbAtlasMapper.countPage(request);
        Long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        List<HerbAtlasVO> records = herbAtlasMapper.selectPage(request, offset, request.getPageSize());
        return new PageResult<>(total, request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public List<HerbAtlasVO> listEnabled(Long speciesId) {
        getActiveSpecies(speciesId);
        return herbAtlasMapper.selectEnabledBySpeciesId(speciesId);
    }

    @Override
    public List<HerbAtlasTagVO> listTags(Long atlasId) {
        if (atlasId == null) {
            throw new BusinessException("Herb atlas id is required");
        }
        return herbAtlasTagMapper.selectByAtlasId(atlasId).stream().map(this::toTagVO).toList();
    }

    private HerbEntity getActiveSpecies(Long speciesId) {
        if (speciesId == null) {
            throw new BusinessException("Herb species id is required");
        }
        HerbEntity species = herbSpeciesMapper.selectActiveById(speciesId);
        if (species == null) {
            throw new BusinessException("Herb species not found");
        }
        return species;
    }

    private SpectrumEntity getActiveAtlas(Long id) {
        if (id == null) {
            throw new BusinessException("Herb atlas id is required");
        }
        SpectrumEntity atlas = herbAtlasMapper.selectActiveById(id);
        if (atlas == null) {
            throw new BusinessException("Herb atlas not found");
        }
        return atlas;
    }

    private String resolveAtlasCode(String atlasCode, LocalDateTime now) {
        if (StringUtils.hasText(atlasCode)) {
            return atlasCode;
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ATLAS_" + now.format(ATLAS_CODE_TIME_FORMAT) + "_" + suffix;
    }

    private List<HerbAtlasTagVO> saveTags(Long atlasId, String tags, LocalDateTime now) {
        List<SpectrumTagEntity> tagEntities = parseTags(tags).stream()
                .map(tagName -> toTagEntity(atlasId, tagName, now))
                .toList();
        if (!tagEntities.isEmpty()) {
            herbAtlasTagMapper.insertTags(tagEntities);
        }
        return tagEntities.stream().map(this::toTagVO).toList();
    }

    private Set<String> parseTags(String tags) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(tags)) {
            return result;
        }
        Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(result::add);
        return result;
    }

    private SpectrumTagEntity toTagEntity(Long atlasId, String tagName, LocalDateTime now) {
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

    private void logicalDeleteTags(Long atlasId, LocalDateTime now) {
        SpectrumTagEntity tag = new SpectrumTagEntity();
        tag.setAtlasId(atlasId);
        tag.setUpdateTime(now);
        herbAtlasTagMapper.logicalDeleteByAtlasId(tag);
    }

    private void normalizePageRequest(HerbAtlasQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private HerbAtlasVO toVO(SpectrumEntity entity) {
        HerbAtlasVO vo = new HerbAtlasVO();
        vo.setId(entity.getId());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setAtlasCode(entity.getAtlasCode());
        vo.setImageUrl(entity.getImageUrl());
        vo.setImageName(entity.getImageName());
        vo.setImageType(entity.getImageType());
        vo.setGrowthStage(entity.getGrowthStage());
        vo.setMedicinalPart(entity.getMedicinalPart());
        vo.setSource(entity.getSource());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private HerbAtlasTagVO toTagVO(SpectrumTagEntity entity) {
        HerbAtlasTagVO vo = new HerbAtlasTagVO();
        vo.setId(entity.getId());
        vo.setAtlasId(entity.getAtlasId());
        vo.setTagName(entity.getTagName());
        vo.setTagType(entity.getTagType());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
