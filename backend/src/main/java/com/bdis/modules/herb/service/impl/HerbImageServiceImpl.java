package com.bdis.modules.herb.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.herb.dto.HerbImageQueryRequest;
import com.bdis.modules.herb.dto.HerbImageUpdateRequest;
import com.bdis.modules.herb.dto.HerbImageUploadRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.HerbImageService;
import com.bdis.modules.herb.support.HerbImageAccessService;
import com.bdis.modules.herb.vo.HerbImageVO;
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
    private final GrowthRecordMapper growthRecordMapper;
    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;
    private final HerbImageAccessService herbImageAccessService;

    public HerbImageServiceImpl(
            HerbImageMapper herbImageMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            GrowthRecordMapper growthRecordMapper,
            FileResourceService fileResourceService,
            FileBusinessService fileBusinessService,
            HerbImageAccessService herbImageAccessService) {
        this.herbImageMapper = herbImageMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.growthRecordMapper = growthRecordMapper;
        this.fileResourceService = fileResourceService;
        this.fileBusinessService = fileBusinessService;
        this.herbImageAccessService = herbImageAccessService;
    }

    @Override
    @Transactional
    public HerbImageVO upload(MultipartFile file, HerbImageUploadRequest request) {
        HerbImageUploadRequest safeRequest =
                request == null ? new HerbImageUploadRequest() : request;
        synchronizeGrowthReference(safeRequest);
        HerbEntity species = getActiveSpeciesIfPresent(safeRequest.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        String imageCode = generateImageCode(now);
        FileResourceVO fileResource = uploadFile(file, "herb_image");
        HerbImageEntity image =
                buildImage(file, safeRequest, fileResource.getFileUrl(), imageCode, now);
        try {
            herbImageMapper.insertImage(image);
            bindFile(fileResource.getId(), "herb_image", image.getId(), "original_image");
            if (safeRequest.getGrowthRecordId() != null) {
                bindFile(
                        fileResource.getId(),
                        "herb_growth_record",
                        safeRequest.getGrowthRecordId(),
                        "field_image");
            }
        } catch (RuntimeException exception) {
            cleanupFile(fileResource.getId(), exception);
            throw exception;
        }
        return toVO(image, species);
    }

    @Override
    @Transactional
    public HerbImageVO update(Long id, HerbImageUpdateRequest request) {
        HerbImageEntity image = getActiveImage(id);
        requireOwner(image);
        Long fileId = fileResourceService.resolveFileId(image.getImageUrl());
        Long previousGrowthRecordId = image.getGrowthRecordId();
        HerbImageUpdateRequest safeRequest =
                request == null ? new HerbImageUpdateRequest() : request;
        synchronizeGrowthReference(safeRequest);
        HerbEntity species = getActiveSpeciesIfPresent(safeRequest.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        image.setSpeciesId(safeRequest.getSpeciesId());
        if (safeRequest.getCollectorId() != null) {
            image.setUploaderId(resolveUploaderId(safeRequest.getCollectorId()));
        }
        image.setDistributionId(
                safeRequest.getDistributionId() == null
                        ? safeRequest.getBaseId()
                        : safeRequest.getDistributionId());
        image.setGrowthRecordId(safeRequest.getGrowthRecordId());
        image.setCollectedLocation(safeRequest.getCollectPlace());
        image.setCollectedAt(safeRequest.getCollectTime());
        image.setImageType(safeRequest.getImageType());
        image.setGrowthStage(safeRequest.getGrowthStage());
        image.setHealthStatus(safeRequest.getHealthStatus());
        image.setProcessStatus(safeRequest.getProcessStatus());
        image.setUpdatedAt(now);
        herbImageMapper.updateImage(image);
        syncGrowthFileRelation(fileId, previousGrowthRecordId, image.getGrowthRecordId());
        return toVO(image, species);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbImageEntity image = getActiveImage(id);
        requireOwner(image);
        Long fileId = fileResourceService.resolveFileId(image.getImageUrl());
        if (fileId != null) {
            fileResourceService.delete(fileId);
        }
        image.setUpdatedAt(LocalDateTime.now());
        int affected = herbImageMapper.logicalDeleteById(image);
        if (affected == 0) {
            throw new BusinessException("Herb image not found or already deleted");
        }
    }

    @Override
    public HerbImageVO getById(Long id) {
        herbImageAccessService.requireAccess(id);
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
        CollectionAccessScope scope = herbImageAccessService.currentScope();
        Long total = herbImageMapper.countPage(safeRequest, scope);
        Long offset = (long) (safeRequest.getPageNum() - 1) * safeRequest.getPageSize();
        List<HerbImageVO> records =
                herbImageMapper.selectPage(safeRequest, scope, offset, safeRequest.getPageSize());
        return new PageResult<>(
                total, safeRequest.getPageNum(), safeRequest.getPageSize(), records);
    }

    @Override
    public PageResult<HerbImageVO> my(Long collectorId, Integer pageNum, Integer pageSize) {
        Long currentUserId = CurrentUserUtils.currentUserId();
        Long resolvedCollectorId = collectorId == null ? currentUserId : collectorId;
        if (resolvedCollectorId == null || resolvedCollectorId <= 0) {
            throw new BusinessException("Collector id is required");
        }
        if (!isAdmin() && !resolvedCollectorId.equals(currentUserId)) {
            throw new ForbiddenException("只能查询本人上传的图片");
        }
        int normalizedPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long total = herbImageMapper.countByCollectorId(resolvedCollectorId);
        Long offset = (long) (normalizedPageNum - 1) * normalizedPageSize;
        List<HerbImageVO> records =
                herbImageMapper.selectByCollectorId(
                        resolvedCollectorId, offset, normalizedPageSize);
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
            String fileUrl,
            String imageCode,
            LocalDateTime now) {
        HerbImageEntity image = new HerbImageEntity();
        image.setImageNo(imageCode);
        image.setSpeciesId(request.getSpeciesId());
        image.setImageUrl(fileUrl);
        image.setOriginalFilename(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        image.setFileFormat(StringUtils.getFilenameExtension(file.getOriginalFilename()));
        image.setUploadSource(resolveUploadSource(request.getUploadSource()));
        image.setUploaderId(resolveUploaderId(request.getCollectorId()));
        image.setDistributionId(
                request.getDistributionId() == null
                        ? request.getBaseId()
                        : request.getDistributionId());
        image.setGrowthRecordId(request.getGrowthRecordId());
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

    private FileResourceVO uploadFile(MultipartFile file, String usage) {
        FileUploadDTO dto = new FileUploadDTO();
        dto.setFile(file);
        dto.setFileType("image");
        dto.setAccessLevel("private");
        dto.setFileUsage(usage);
        return fileResourceService.upload(dto);
    }

    private void bindFile(Long fileId, String bizType, Long bizId, String usage) {
        FileBusinessBindDTO bind = new FileBusinessBindDTO();
        bind.setFileId(fileId);
        bind.setBizType(bizType);
        bind.setBizId(bizId);
        bind.setFileUsage(usage);
        fileBusinessService.bind(bind);
    }

    private void cleanupFile(Long fileId, RuntimeException original) {
        try {
            fileResourceService.delete(fileId);
        } catch (RuntimeException cleanupException) {
            original.addSuppressed(cleanupException);
        }
    }

    private void syncGrowthFileRelation(
            Long fileId, Long previousGrowthRecordId, Long growthRecordId) {
        if (fileId == null || java.util.Objects.equals(previousGrowthRecordId, growthRecordId)) {
            return;
        }
        if (previousGrowthRecordId != null) {
            fileBusinessService.deleteByBusinessAndFile(
                    "herb_growth_record", previousGrowthRecordId, fileId);
        }
        if (growthRecordId != null) {
            bindFile(fileId, "herb_growth_record", growthRecordId, "field_image");
        }
    }

    private void synchronizeGrowthReference(HerbImageUploadRequest request) {
        if (request.getGrowthRecordId() == null) {
            return;
        }
        Long distributionId =
                request.getDistributionId() == null
                        ? request.getBaseId()
                        : request.getDistributionId();
        GrowthRecordEntity growth =
                requireCompatibleGrowthRecord(
                        request.getGrowthRecordId(), request.getSpeciesId(), distributionId);
        herbImageAccessService.requireGrowthRecordAccess(growth);
        if (request.getSpeciesId() == null) {
            request.setSpeciesId(growth.getSpeciesId());
        }
        if (distributionId == null) {
            request.setDistributionId(growth.getDistributionId());
        }
    }

    private void synchronizeGrowthReference(HerbImageUpdateRequest request) {
        if (request.getGrowthRecordId() == null) {
            return;
        }
        Long distributionId =
                request.getDistributionId() == null
                        ? request.getBaseId()
                        : request.getDistributionId();
        GrowthRecordEntity growth =
                requireCompatibleGrowthRecord(
                        request.getGrowthRecordId(), request.getSpeciesId(), distributionId);
        herbImageAccessService.requireGrowthRecordAccess(growth);
        if (request.getSpeciesId() == null) {
            request.setSpeciesId(growth.getSpeciesId());
        }
        if (distributionId == null) {
            request.setDistributionId(growth.getDistributionId());
        }
    }

    private GrowthRecordEntity requireCompatibleGrowthRecord(
            Long growthRecordId, Long speciesId, Long distributionId) {
        GrowthRecordEntity growth = growthRecordMapper.selectById(growthRecordId);
        if (growth == null) {
            throw new BusinessException("Growth record not found");
        }
        if (speciesId != null && !speciesId.equals(growth.getSpeciesId())) {
            throw new BusinessException("Image species does not match growth record");
        }
        if (distributionId != null && !distributionId.equals(growth.getDistributionId())) {
            throw new BusinessException("Image distribution does not match growth record");
        }
        return growth;
    }

    private Long resolveUploaderId(Long requestedCollectorId) {
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (requestedCollectorId == null) {
            return currentUserId == null || currentUserId <= 0 ? null : currentUserId;
        }
        if (isAdmin() || requestedCollectorId.equals(currentUserId)) {
            return requestedCollectorId;
        }
        throw new ForbiddenException("不能以其他用户身份上传或修改图片");
    }

    private void requireOwner(HerbImageEntity image) {
        Long currentUserId = CurrentUserUtils.currentUserId();
        if (!isAdmin() && (currentUserId == null || !currentUserId.equals(image.getUploaderId()))) {
            throw new ForbiddenException("只能修改或删除本人上传的图片");
        }
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
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
        vo.setGrowthRecordId(entity.getGrowthRecordId());
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
