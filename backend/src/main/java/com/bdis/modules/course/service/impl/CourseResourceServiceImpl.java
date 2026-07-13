package com.bdis.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.modules.course.constant.CoursePublishStatus;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.CourseResourceEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.CourseResourceMapper;
import com.bdis.modules.course.query.CourseResourceQuery;
import com.bdis.modules.course.request.CourseResourceBindRequest;
import com.bdis.modules.course.service.CourseResourceService;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CourseResourceServiceImpl implements CourseResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseResourceServiceImpl.class);
    private static final String BIZ_TYPE = "edu_course";
    private static final String AUDIT_MODULE = "M12_COURSE";

    private final CourseResourceMapper resourceMapper;
    private final CourseMapper courseMapper;
    private final FileResourceMapper fileResourceMapper;
    private final FileBusinessMapper fileBusinessMapper;
    private final FileBusinessService fileBusinessService;
    private final AuditLogService auditLogService;

    public CourseResourceServiceImpl(
            CourseResourceMapper resourceMapper,
            CourseMapper courseMapper,
            FileResourceMapper fileResourceMapper,
            FileBusinessMapper fileBusinessMapper,
            FileBusinessService fileBusinessService,
            AuditLogService auditLogService) {
        this.resourceMapper = resourceMapper;
        this.courseMapper = courseMapper;
        this.fileResourceMapper = fileResourceMapper;
        this.fileBusinessMapper = fileBusinessMapper;
        this.fileBusinessService = fileBusinessService;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<CourseResourceVO> listByCourseId(Long courseId, CourseResourceQuery query) {
        requireCourse(courseId);
        LambdaQueryWrapper<CourseResourceEntity> wrapper =
                new LambdaQueryWrapper<CourseResourceEntity>()
                        .eq(CourseResourceEntity::getCourseId, courseId)
                        .eq(CourseResourceEntity::getStatus, 1)
                        .orderByAsc(CourseResourceEntity::getSortOrder)
                        .orderByAsc(CourseResourceEntity::getId);
        if (query != null && StringUtils.hasText(query.getResourceType())) {
            wrapper.eq(CourseResourceEntity::getResourceType, query.getResourceType());
        }
        List<CourseResourceEntity> resources = resourceMapper.selectList(wrapper);
        if (resources.isEmpty()) {
            return List.of();
        }
        List<Long> fileIds =
                resources.stream()
                        .map(CourseResourceEntity::getFileId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (fileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, FileResourceEntity> files = new LinkedHashMap<>();
        for (FileResourceEntity file : fileResourceMapper.selectBatchIds(fileIds)) {
            if (Objects.equals(file.getStatus(), 1)) {
                files.put(file.getId(), file);
            }
        }
        return resources.stream()
                .filter(resource -> files.containsKey(resource.getFileId()))
                .map(resource -> toVO(resource, files.get(resource.getFileId())))
                .toList();
    }

    @Override
    @Transactional
    public CourseResourceVO bind(Long courseId, CourseResourceBindRequest request) {
        CourseEntity course = requireEditableCourse(courseId);
        validateRequest(request);
        FileResourceEntity file = requireActiveFile(request.getFileId());
        CourseResourceEntity duplicate =
                resourceMapper.selectOne(
                        new LambdaQueryWrapper<CourseResourceEntity>()
                                .eq(CourseResourceEntity::getCourseId, courseId)
                                .eq(CourseResourceEntity::getFileId, request.getFileId()));
        if (duplicate != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "File is already bound to course");
        }
        LocalDateTime now = LocalDateTime.now();
        CourseResourceEntity entity = new CourseResourceEntity();
        entity.setCourseId(course.getId());
        entity.setResourceName(request.getResourceName().trim());
        entity.setResourceType(request.getResourceType());
        entity.setFileId(file.getId());
        entity.setUploaderId(CurrentUserUtils.currentUserId());
        entity.setUploadedAt(now);
        entity.setDownloadCount(0);
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        entity.setRemark(request.getRemark());
        entity.setVersion(0);
        resourceMapper.insert(entity);

        FileBusinessBindDTO bindDTO = new FileBusinessBindDTO();
        bindDTO.setFileId(file.getId());
        bindDTO.setBizType(BIZ_TYPE);
        bindDTO.setBizId(courseId);
        bindDTO.setFileUsage(request.getResourceType());
        bindDTO.setRemark(request.getRemark());
        fileBusinessService.bind(bindDTO);
        recordAudit("BIND", courseId);
        return toVO(entity, file);
    }

    @Override
    @Transactional
    public void unbind(Long courseId, Long resourceId) {
        requireEditableCourse(courseId);
        CourseResourceEntity resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException("Course resource not found");
        }
        if (!Objects.equals(resource.getCourseId(), courseId)) {
            throw new ResourceNotFoundException("Course resource does not belong to course");
        }
        FileBusinessEntity relation =
                fileBusinessMapper.selectOne(
                        new LambdaQueryWrapper<FileBusinessEntity>()
                                .eq(FileBusinessEntity::getFileId, resource.getFileId())
                                .eq(FileBusinessEntity::getBizType, BIZ_TYPE)
                                .eq(FileBusinessEntity::getBizId, courseId));
        if (relation != null) {
            fileBusinessService.unbind(relation.getId());
        }
        if (resourceMapper.deleteById(resource.getId()) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Course resource unbind failed");
        }
        recordAudit("UNBIND", courseId);
    }

    private CourseEntity requireCourse(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException("Course id is required");
        }
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found");
        }
        return course;
    }

    private CourseEntity requireEditableCourse(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        CoursePublishStatus.requireEditable(course.getPublishStatus());
        return course;
    }

    private FileResourceEntity requireActiveFile(Long fileId) {
        FileResourceEntity file = fileResourceMapper.selectById(fileId);
        if (file == null || !Objects.equals(file.getStatus(), 1)) {
            throw new ResourceNotFoundException("File not found or inactive");
        }
        return file;
    }

    private void validateRequest(CourseResourceBindRequest request) {
        if (request == null
                || request.getFileId() == null
                || !StringUtils.hasText(request.getResourceName())) {
            throw new BusinessException("File and resource name are required");
        }
    }

    private CourseResourceVO toVO(CourseResourceEntity entity, FileResourceEntity file) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setResourceName(entity.getResourceName());
        vo.setResourceType(entity.getResourceType());
        vo.setFileId(entity.getFileId());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setRemark(entity.getRemark());
        if (file != null) {
            vo.setFileName(file.getFileName());
            vo.setOriginalFilename(file.getOriginalFilename());
            vo.setFileType(file.getFileType());
            vo.setFileFormat(file.getFileFormat());
            vo.setFileSize(file.getFileSize());
            vo.setFileUrl(file.getFileUrl());
            vo.setThumbnailUrl(file.getThumbnailUrl());
            vo.setContentType(file.getContentType());
            vo.setUploaderId(file.getUploaderId());
            vo.setUploadedAt(file.getUploadedAt());
        }
        return vo;
    }

    private void recordAudit(String operationType, Long courseId) {
        AuditRecordDTO audit = new AuditRecordDTO();
        audit.setOperationModule(AUDIT_MODULE);
        audit.setOperationType(operationType);
        audit.setBizType(BIZ_TYPE);
        audit.setBizId(courseId);
        try {
            auditLogService.record(audit);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist course resource audit log", exception);
        }
    }
}
