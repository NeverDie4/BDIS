package com.bdis.modules.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.course.entity.CourseResourceEntity;
import com.bdis.modules.course.mapper.CourseResourceMapper;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.modules.training.constant.TrainingMaterialSourceType;
import com.bdis.modules.training.constant.TrainingMaterialType;
import com.bdis.modules.training.entity.TrainingMaterialEntity;
import com.bdis.modules.training.mapper.TrainingMaterialMapper;
import com.bdis.modules.training.mapper.TrainingPlanMaterialMapper;
import com.bdis.modules.training.query.TrainingMaterialQuery;
import com.bdis.modules.training.request.TrainingMaterialCreateRequest;
import com.bdis.modules.training.request.TrainingMaterialUpdateRequest;
import com.bdis.modules.training.service.TrainingMaterialService;
import com.bdis.modules.training.vo.TrainingMaterialDetailVO;
import com.bdis.modules.training.vo.TrainingMaterialListVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainingMaterialServiceImpl implements TrainingMaterialService {
    private static final String AUDIT_MODULE = "M15_TRAINING_MATERIAL";
    private static final String BIZ_TYPE = "edu_training_material";
    private static final Set<String> SORT_FIELDS =
            Set.of("uploadedAt", "materialName", "materialNo", "createdAt", "updatedAt", "reuseCount");

    private final TrainingMaterialMapper materialMapper;
    private final TrainingPlanMaterialMapper relationMapper;
    private final FileResourceMapper fileMapper;
    private final FileAccessGuard fileAccessGuard;
    private final CourseResourceMapper courseResourceMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public TrainingMaterialServiceImpl(
            TrainingMaterialMapper materialMapper,
            TrainingPlanMaterialMapper relationMapper,
            FileResourceMapper fileMapper,
            CourseResourceMapper courseResourceMapper,
            UserMapper userMapper,
            FileAccessGuard fileAccessGuard,
            AuditLogService auditLogService) {
        this.materialMapper = materialMapper;
        this.relationMapper = relationMapper;
        this.fileMapper = fileMapper;
        this.fileAccessGuard = fileAccessGuard;
        this.courseResourceMapper = courseResourceMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TrainingMaterialListVO> page(TrainingMaterialQuery query) {
        TrainingMaterialQuery safe = query == null ? new TrainingMaterialQuery() : query;
        validateQuery(safe);
        Page<TrainingMaterialEntity> page =
                materialMapper.selectPage(
                        Page.of(safe.getPageNo(), safe.getPageSize()), buildWrapper(safe));
        Lookup lookup = loadLookup(page.getRecords());
        List<TrainingMaterialListVO> records =
                page.getRecords().stream().map(entity -> toListVO(entity, lookup)).toList();
        return PageResult.of(records, page);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingMaterialDetailVO getDetail(Long id) {
        TrainingMaterialEntity entity = requireActive(id);
        FileResourceEntity file = requireActiveFile(entity.getFileId());
        UserEntity uploader =
                entity.getUploaderId() == null ? null : userMapper.selectById(entity.getUploaderId());
        TrainingMaterialDetailVO vo = new TrainingMaterialDetailVO();
        copyListFields(entity, vo, file, uploader);
        vo.setOriginalFilename(file.getOriginalFilename());
        vo.setFileFormat(file.getFileFormat());
        vo.setFileSize(file.getFileSize());
        vo.setFileUrl(file.getFileUrl());
        vo.setThumbnailUrl(file.getThumbnailUrl());
        vo.setStorageType(file.getStorageType());
        vo.setRemark(entity.getRemark());
        vo.setVersion(entity.getVersion());
        vo.setPlanCount(defaultZero(relationMapper.countActivePlanBindings(id)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TrainingMaterialCreateRequest request) {
        validateCreate(request);
        Long operatorId = requireCurrentOperator();
        ensureMaterialNoAvailable(request.getMaterialNo());
        FileResourceEntity file =
                validateSource(request.getFileId(), request.getSourceType(), request.getSourceResourceId());

        LocalDateTime now = LocalDateTime.now();
        TrainingMaterialEntity entity = new TrainingMaterialEntity();
        entity.setMaterialNo(request.getMaterialNo().trim());
        entity.setMaterialName(request.getMaterialName().trim());
        entity.setMaterialType(request.getMaterialType().trim());
        entity.setDescription(request.getDescription());
        entity.setFileId(file.getId());
        entity.setSourceType(request.getSourceType().trim());
        entity.setSourceResourceId(request.getSourceResourceId());
        entity.setUploaderId(operatorId);
        entity.setUploadedAt(now);
        entity.setReuseCount(0);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setRemark(request.getRemark());
        entity.setVersion(0);
        try {
            if (materialMapper.insert(entity) == 0) throw conflict("Training material creation failed");
        } catch (DuplicateKeyException exception) {
            throw conflict("Training material number already exists");
        }
        recordAudit("CREATE", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TrainingMaterialUpdateRequest request) {
        TrainingMaterialEntity entity = requireActive(id);
        validateUpdate(request);
        if (request.getMaterialNo() != null) {
            throw new BusinessException("Training material number cannot be changed");
        }
        if (!Objects.equals(request.getVersion(), entity.getVersion())) {
            throw conflict("Training material version conflict");
        }
        Long operatorId = requireCurrentOperator();
        FileResourceEntity file =
                validateSource(request.getFileId(), request.getSourceType(), request.getSourceResourceId());
        String operationType =
                Objects.equals(entity.getStatus(), request.getStatus())
                        ? "UPDATE"
                        : (Objects.equals(request.getStatus(), 1) ? "ENABLE" : "DISABLE");
        entity.setMaterialName(request.getMaterialName().trim());
        entity.setMaterialType(request.getMaterialType().trim());
        entity.setDescription(request.getDescription());
        entity.setFileId(file.getId());
        entity.setSourceType(request.getSourceType().trim());
        entity.setSourceResourceId(request.getSourceResourceId());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(operatorId);
        if (materialMapper.updateById(entity) == 0) {
            throw conflict("Training material version conflict");
        }
        recordAudit(operationType, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TrainingMaterialEntity entity = requireActive(id);
        if (defaultZero(relationMapper.countActivePlanBindings(id)) > 0) {
            throw conflict("Training material is still bound to an active training plan");
        }
        Long operatorId = requireCurrentOperator();
        if (materialMapper.logicalDelete(id, entity.getVersion(), LocalDateTime.now(), operatorId) == 0) {
            throw conflict("Training material delete state or version conflict");
        }
        recordAudit("DELETE", id);
    }

    private void validateCreate(TrainingMaterialCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getMaterialNo())
                || !StringUtils.hasText(request.getMaterialName())
                || request.getFileId() == null) {
            throw new BusinessException("Material number, name and file are required");
        }
        validateCommon(request.getMaterialType(), request.getSourceType(), request.getSourceResourceId());
    }

    private void validateUpdate(TrainingMaterialUpdateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getMaterialName())
                || request.getFileId() == null
                || request.getStatus() == null
                || request.getVersion() == null) {
            throw new BusinessException("Material name, file, status and version are required");
        }
        if (request.getStatus() < 0 || request.getStatus() > 1) {
            throw new BusinessException("Training material status must be 0 or 1");
        }
        validateCommon(request.getMaterialType(), request.getSourceType(), request.getSourceResourceId());
    }

    private void validateCommon(String materialType, String sourceType, Long sourceResourceId) {
        if (!TrainingMaterialType.VALUES.contains(materialType)) {
            throw new BusinessException("Unsupported training material type");
        }
        if (!TrainingMaterialSourceType.VALUES.contains(sourceType)) {
            throw new BusinessException("Unsupported training material source type");
        }
        if (TrainingMaterialSourceType.COURSE_RESOURCE.equals(sourceType)) {
            if (sourceResourceId == null) {
                throw new BusinessException("course_resource source requires sourceResourceId");
            }
        } else if (sourceResourceId != null) {
            throw new BusinessException(sourceType + " source must not specify sourceResourceId");
        }
    }

    private FileResourceEntity validateSource(Long fileId, String sourceType, Long sourceResourceId) {
        FileResourceEntity file = requireActiveFile(fileId);
        fileAccessGuard.requireAuthenticatedAccess(file);
        if (TrainingMaterialSourceType.COURSE_RESOURCE.equals(sourceType)) {
            CourseResourceEntity resource = courseResourceMapper.selectById(sourceResourceId);
            if (resource == null
                    || Objects.equals(resource.getIsDeleted(), 1)
                    || !Objects.equals(resource.getStatus(), 1)) {
                throw new ResourceNotFoundException("Source course resource not found or inactive");
            }
            if (!Objects.equals(resource.getFileId(), fileId)) {
                throw new BusinessException("sourceResourceId does not reference the requested fileId");
            }
        }
        return file;
    }

    private FileResourceEntity requireActiveFile(Long id) {
        if (id == null || id <= 0) throw new ResourceNotFoundException("File not found");
        FileResourceEntity file = fileMapper.selectById(id);
        if (file == null || Objects.equals(file.getIsDeleted(), 1) || !Objects.equals(file.getStatus(), 1)) {
            throw new ResourceNotFoundException("File not found, deleted or inactive");
        }
        return file;
    }

    private Long requireCurrentOperator() {
        Long id = CurrentUserUtils.currentUserId();
        UserEntity user = id == null || id <= 0 ? null : userMapper.selectById(id);
        if (user == null || Objects.equals(user.getIsDeleted(), 1) || !Objects.equals(user.getStatus(), 1)) {
            throw new ResourceNotFoundException("Current operator not found or inactive");
        }
        return id;
    }

    private void ensureMaterialNoAvailable(String materialNo) {
        if (materialMapper.selectByMaterialNoIncludingDeleted(materialNo.trim()) != null) {
            throw conflict("Training material number already exists");
        }
    }

    private TrainingMaterialEntity requireActive(Long id) {
        if (id == null || id <= 0) throw new BusinessException("Training material id must be positive");
        TrainingMaterialEntity entity = materialMapper.selectByIdIncludingDeleted(id);
        if (entity == null || Objects.equals(entity.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Training material not found");
        }
        return entity;
    }

    private void validateQuery(TrainingMaterialQuery query) {
        if (query.getPageNo() == null || query.getPageNo() < 1) query.setPageNo(1);
        if (query.getPageSize() == null || query.getPageSize() < 1) query.setPageSize(10);
        if (query.getPageSize() > 100) query.setPageSize(100);
        if (StringUtils.hasText(query.getMaterialType())
                && !TrainingMaterialType.VALUES.contains(query.getMaterialType())) {
            throw new BusinessException("Unsupported training material type");
        }
        if (StringUtils.hasText(query.getSourceType())
                && !TrainingMaterialSourceType.VALUES.contains(query.getSourceType())) {
            throw new BusinessException("Unsupported training material source type");
        }
        if (query.getStatus() != null && query.getStatus() != 0 && query.getStatus() != 1) {
            throw new BusinessException("Training material status must be 0 or 1");
        }
        if (StringUtils.hasText(query.getSortField()) && !SORT_FIELDS.contains(query.getSortField())) {
            throw new BusinessException("Unsupported training material sort field");
        }
        if (StringUtils.hasText(query.getSortOrder())
                && !"asc".equalsIgnoreCase(query.getSortOrder())
                && !"desc".equalsIgnoreCase(query.getSortOrder())) {
            throw new BusinessException("Unsupported sort order");
        }
    }

    private LambdaQueryWrapper<TrainingMaterialEntity> buildWrapper(TrainingMaterialQuery query) {
        LambdaQueryWrapper<TrainingMaterialEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(TrainingMaterialEntity::getMaterialNo, query.getKeyword())
                    .or().like(TrainingMaterialEntity::getMaterialName, query.getKeyword()));
        }
        wrapper.eq(StringUtils.hasText(query.getMaterialType()), TrainingMaterialEntity::getMaterialType, query.getMaterialType());
        wrapper.eq(StringUtils.hasText(query.getSourceType()), TrainingMaterialEntity::getSourceType, query.getSourceType());
        wrapper.eq(query.getUploaderId() != null, TrainingMaterialEntity::getUploaderId, query.getUploaderId());
        wrapper.eq(query.getStatus() != null, TrainingMaterialEntity::getStatus, query.getStatus());
        boolean asc = "asc".equalsIgnoreCase(query.getSortOrder());
        String field = StringUtils.hasText(query.getSortField()) ? query.getSortField() : "uploadedAt";
        switch (field) {
            case "materialName" -> wrapper.orderBy(true, asc, TrainingMaterialEntity::getMaterialName);
            case "materialNo" -> wrapper.orderBy(true, asc, TrainingMaterialEntity::getMaterialNo);
            case "createdAt" -> wrapper.orderBy(true, asc, TrainingMaterialEntity::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, TrainingMaterialEntity::getUpdatedAt);
            case "reuseCount" -> wrapper.orderBy(true, asc, TrainingMaterialEntity::getReuseCount);
            default -> wrapper.orderBy(true, asc, TrainingMaterialEntity::getUploadedAt);
        }
        wrapper.orderByDesc(TrainingMaterialEntity::getId);
        return wrapper;
    }

    private Lookup loadLookup(List<TrainingMaterialEntity> entities) {
        Set<Long> fileIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (TrainingMaterialEntity entity : entities) {
            if (entity.getFileId() != null) fileIds.add(entity.getFileId());
            if (entity.getUploaderId() != null) userIds.add(entity.getUploaderId());
        }
        Map<Long, FileResourceEntity> files = new HashMap<>();
        Map<Long, UserEntity> users = new HashMap<>();
        if (!fileIds.isEmpty()) fileMapper.selectBatchIds(fileIds).forEach(value -> files.put(value.getId(), value));
        if (!userIds.isEmpty()) userMapper.selectBatchIds(userIds).forEach(value -> users.put(value.getId(), value));
        return new Lookup(files, users);
    }

    private TrainingMaterialListVO toListVO(TrainingMaterialEntity entity, Lookup lookup) {
        TrainingMaterialListVO vo = new TrainingMaterialListVO();
        copyListFields(entity, vo, lookup.files().get(entity.getFileId()), lookup.users().get(entity.getUploaderId()));
        return vo;
    }

    private void copyListFields(
            TrainingMaterialEntity entity,
            TrainingMaterialListVO vo,
            FileResourceEntity file,
            UserEntity uploader) {
        vo.setId(entity.getId());
        vo.setMaterialNo(entity.getMaterialNo());
        vo.setMaterialName(entity.getMaterialName());
        vo.setMaterialType(entity.getMaterialType());
        vo.setDescription(entity.getDescription());
        vo.setFileId(entity.getFileId());
        vo.setFileName(file == null ? null : file.getFileName());
        vo.setFileType(file == null ? null : file.getFileType());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceResourceId(entity.getSourceResourceId());
        vo.setUploaderId(entity.getUploaderId());
        vo.setUploaderName(uploader == null ? null : uploader.getRealName());
        vo.setUploadedAt(entity.getUploadedAt());
        vo.setReuseCount(entity.getReuseCount());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
    }

    private void recordAudit(String operationType, Long id) {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(AUDIT_MODULE);
        dto.setOperationType(operationType);
        dto.setBizType(BIZ_TYPE);
        dto.setBizId(id);
        auditLogService.record(dto);
    }

    private static long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BusinessException conflict(String message) {
        return new BusinessException(ResultCodeEnum.CONFLICT, message);
    }

    private record Lookup(Map<Long, FileResourceEntity> files, Map<Long, UserEntity> users) {}
}
