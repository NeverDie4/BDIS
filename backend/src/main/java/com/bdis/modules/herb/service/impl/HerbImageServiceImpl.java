package com.bdis.modules.herb.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.storage.LocalFileStorage;
import com.bdis.common.storage.LocalFileStorage.StoredFile;
import com.bdis.modules.herb.dto.HerbImageQueryRequest;
import com.bdis.modules.herb.dto.HerbImageUpdateRequest;
import com.bdis.modules.herb.dto.HerbImageUploadRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.HerbImageService;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.spectrum.service.HerbFeatureService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HerbImageServiceImpl implements HerbImageService {

    private static final String DEFAULT_UPLOAD_SOURCE = "mobile";
    private static final String DEFAULT_PROCESS_STATUS = "uploaded";
    private static final DateTimeFormatter IMAGE_CODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HerbImageMapper herbImageMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final LocalFileStorage localFileStorage;
    private final HerbFeatureService herbFeatureService;

    public HerbImageServiceImpl(
            HerbImageMapper herbImageMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            LocalFileStorage localFileStorage,
            HerbFeatureService herbFeatureService) {
        this.herbImageMapper = herbImageMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.localFileStorage = localFileStorage;
        this.herbFeatureService = herbFeatureService;
    }

    @Override
    @Transactional
    public HerbImageVO upload(MultipartFile file, HerbImageUploadRequest request) {
        HerbImageUploadRequest safeRequest = request == null ? new HerbImageUploadRequest() : request;
        HerbEntity species = getActiveSpeciesIfPresent(safeRequest.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        String imageCode = generateImageCode(now);
        StoredFile storedFile = localFileStorage.saveHerbImage(file, imageCode);

        HerbImageEntity image = buildImage(file, safeRequest, storedFile, imageCode, now);
        try {
            herbImageMapper.insertImage(image);
            herbFeatureService.extractImageFeature(image.getId());
        } catch (RuntimeException exception) {
            localFileStorage.deleteByUrl(storedFile.url());
            throw exception;
        }
        return toVO(image, species);
    }

    @Override
    @Transactional
    public HerbImageVO update(Long id, HerbImageUpdateRequest request) {
        HerbImageEntity image = getActiveImage(id);
        HerbImageUpdateRequest safeRequest = request == null ? new HerbImageUpdateRequest() : request;
        HerbEntity species = getActiveSpeciesIfPresent(safeRequest.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        image.setSpeciesId(safeRequest.getSpeciesId());
        image.setCollectorId(safeRequest.getCollectorId());
        image.setBaseId(safeRequest.getBaseId());
        image.setCollectPlace(safeRequest.getCollectPlace());
        image.setCollectTime(safeRequest.getCollectTime());
        image.setImageType(safeRequest.getImageType());
        image.setGrowthStage(safeRequest.getGrowthStage());
        image.setHealthStatus(safeRequest.getHealthStatus());
        image.setProcessStatus(safeRequest.getProcessStatus());
        image.setUpdateTime(now);
        herbImageMapper.updateImage(image);
        return toVO(image, species);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbImageEntity image = getActiveImage(id);
        image.setUpdateTime(LocalDateTime.now());
        int affected = herbImageMapper.logicalDeleteById(image);
        if (affected == 0) {
            throw new BusinessException("Herb image not found or already deleted");
        }
    }

    @Override
    public HerbImageVO getById(Long id) {
        if (id == null) {
            throw new BusinessException("Herb image id is required");
        }
        HerbImageVO vo = herbImageMapper.selectDetailById(id);
        if (vo == null) {
            throw new BusinessException("Herb image not found");
        }
        return vo;
    }

    @Override
    public PageResult<HerbImageVO> page(HerbImageQueryRequest request) {
        HerbImageQueryRequest safeRequest = request == null ? new HerbImageQueryRequest() : request;
        normalizePageRequest(safeRequest);
        Long total = herbImageMapper.countPage(safeRequest);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbImageVO> records =
                herbImageMapper.selectPage(safeRequest, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    @Override
    public PageResult<HerbImageVO> my(Long collectorId, Integer pageNum, Integer pageSize) {
        if (collectorId == null) {
            throw new BusinessException("Collector id is required");
        }
        int normalizedPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long total = herbImageMapper.countByCollectorId(collectorId);
        Long offset = (long) (normalizedPageNum - 1) * normalizedPageSize;
        List<HerbImageVO> records =
                herbImageMapper.selectByCollectorId(collectorId, offset, normalizedPageSize);
        return new PageResult<>(total, normalizedPageNum, normalizedPageSize, records);
    }

    private HerbImageEntity getActiveImage(Long id) {
        if (id == null) {
            throw new BusinessException("Herb image id is required");
        }
        HerbImageEntity image = herbImageMapper.selectActiveById(id);
        if (image == null) {
            throw new BusinessException("Herb image not found");
        }
        return image;
    }

    private HerbEntity getActiveSpeciesIfPresent(Long speciesId) {
        if (speciesId == null) {
            return null;
        }
        HerbEntity species = herbSpeciesMapper.selectActiveById(speciesId);
        if (species == null) {
            throw new BusinessException("Herb species not found");
        }
        return species;
    }

    private HerbImageEntity buildImage(
            MultipartFile file,
            HerbImageUploadRequest request,
            StoredFile storedFile,
            String imageCode,
            LocalDateTime now) {
        HerbImageEntity image = new HerbImageEntity();
        image.setImageCode(imageCode);
        image.setSpeciesId(request.getSpeciesId());
        image.setImageUrl(storedFile.url());
        image.setImageName(file.getOriginalFilename());
        image.setUploadSource(resolveUploadSource(request.getUploadSource()));
        image.setCollectorId(request.getCollectorId());
        image.setBaseId(request.getBaseId());
        image.setCollectPlace(request.getCollectPlace());
        image.setCollectTime(request.getCollectTime() == null ? now : request.getCollectTime());
        image.setImageType(request.getImageType());
        image.setGrowthStage(request.getGrowthStage());
        image.setHealthStatus(request.getHealthStatus());
        image.setProcessStatus(DEFAULT_PROCESS_STATUS);
        image.setCreateTime(now);
        image.setUpdateTime(now);
        image.setDeleted(0);
        return image;
    }

    private String resolveUploadSource(String uploadSource) {
        if (!StringUtils.hasText(uploadSource)) {
            return DEFAULT_UPLOAD_SOURCE;
        }
        return uploadSource.trim();
    }

    private String generateImageCode(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "IMG_" + now.format(IMAGE_CODE_TIME_FORMAT) + "_" + random;
    }

    private void normalizePageRequest(HerbImageQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private HerbImageVO toVO(HerbImageEntity entity, HerbEntity species) {
        HerbImageVO vo = new HerbImageVO();
        vo.setId(entity.getId());
        vo.setImageCode(entity.getImageCode());
        vo.setImageUrl(entity.getImageUrl());
        vo.setImageName(entity.getImageName());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(species == null ? null : species.getHerbName());
        vo.setUploadSource(entity.getUploadSource());
        vo.setCollectorId(entity.getCollectorId());
        vo.setBaseId(entity.getBaseId());
        vo.setCollectPlace(entity.getCollectPlace());
        vo.setCollectTime(entity.getCollectTime());
        vo.setImageType(entity.getImageType());
        vo.setGrowthStage(entity.getGrowthStage());
        vo.setHealthStatus(entity.getHealthStatus());
        vo.setProcessStatus(entity.getProcessStatus());
        return vo;
    }
}
