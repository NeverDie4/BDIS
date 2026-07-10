package com.bdis.modules.herb.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.service.FileStorageService.StoredFile;
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
    private final FileStorageService fileStorageService;
    private final HerbFeatureService herbFeatureService;

    public HerbImageServiceImpl(
            HerbImageMapper herbImageMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            FileStorageService fileStorageService,
            HerbFeatureService herbFeatureService) {
        this.herbImageMapper = herbImageMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.fileStorageService = fileStorageService;
        this.herbFeatureService = herbFeatureService;
    }

    @Override
    @Transactional
    public HerbImageVO upload(MultipartFile file, HerbImageUploadRequest request) {
        HerbImageUploadRequest safeRequest =
                request == null ? new HerbImageUploadRequest() : request;
        HerbEntity species = getActiveSpeciesIfPresent(safeRequest.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        String imageCode = generateImageCode(now);
        StoredFile storedFile = fileStorageService.save(file);

        HerbImageEntity image = buildImage(file, safeRequest, storedFile, imageCode, now);
        try {
            herbImageMapper.insertImage(image);
            herbFeatureService.extractImageFeature(image.getId());
        } catch (RuntimeException exception) {
            fileStorageService.delete(storedFile.storagePath());
            throw exception;
        }
        return toVO(image, species);
    }

    @Override
    @Transactional
    public HerbImageVO update(Long id, HerbImageUpdateRequest request) {
        HerbImageEntity image = getActiveImage(id);
        HerbImageUpdateRequest safeRequest =
                request == null ? new HerbImageUpdateRequest() : request;
        HerbEntity species = getActiveSpeciesIfPresent(safeRequest.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        image.setSpeciesId(safeRequest.getSpeciesId());
        image.setUploaderId(safeRequest.getCollectorId());
        image.setDistributionId(safeRequest.getBaseId());
        image.setCollectedLocation(safeRequest.getCollectPlace());
        image.setCollectedAt(safeRequest.getCollectTime());
        image.setImageType(safeRequest.getImageType());
        image.setGrowthStage(safeRequest.getGrowthStage());
        image.setHealthStatus(safeRequest.getHealthStatus());
        image.setProcessStatus(safeRequest.getProcessStatus());
        image.setUpdatedAt(now);
        herbImageMapper.updateImage(image);
        return toVO(image, species);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbImageEntity image = getActiveImage(id);
        image.setUpdatedAt(LocalDateTime.now());
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
        image.setImageNo(imageCode);
        image.setSpeciesId(request.getSpeciesId());
        image.setImageUrl(storedFile.fileUrl());
        image.setOriginalFilename(file.getOriginalFilename());
        image.setUploadSource(resolveUploadSource(request.getUploadSource()));
        image.setUploaderId(request.getCollectorId());
        image.setDistributionId(request.getBaseId());
        image.setCollectedLocation(request.getCollectPlace());
        image.setCollectedAt(request.getCollectTime() == null ? now : request.getCollectTime());
        image.setImageType(request.getImageType());
        image.setGrowthStage(request.getGrowthStage());
        image.setHealthStatus(request.getHealthStatus());
        image.setProcessStatus(DEFAULT_PROCESS_STATUS);
        image.setStatus(1);
        image.setCreatedAt(now);
        image.setUpdatedAt(now);
        image.setIsDeleted(0);
        image.setVersion(0);
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
        vo.setImageCode(entity.getImageNo());
        vo.setImageUrl(entity.getImageUrl());
        vo.setImageName(entity.getOriginalFilename());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(species == null ? null : species.getHerbName());
        vo.setUploadSource(entity.getUploadSource());
        vo.setCollectorId(entity.getUploaderId());
        vo.setBaseId(entity.getDistributionId());
        vo.setCollectPlace(entity.getCollectedLocation());
        vo.setCollectTime(entity.getCollectedAt());
        vo.setImageType(entity.getImageType());
        vo.setGrowthStage(entity.getGrowthStage());
        vo.setHealthStatus(entity.getHealthStatus());
        vo.setProcessStatus(entity.getProcessStatus());
        return vo;
    }
}
