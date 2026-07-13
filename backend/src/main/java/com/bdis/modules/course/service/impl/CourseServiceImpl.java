package com.bdis.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.course.constant.CoursePublishStatus;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.ExperimentStepEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.ExperimentStepMapper;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.request.CourseCreateRequest;
import com.bdis.modules.course.request.CourseUpdateRequest;
import com.bdis.modules.course.service.CourseService;
import com.bdis.modules.course.service.CourseResourceService;
import com.bdis.modules.course.service.ExperimentStepService;
import com.bdis.modules.course.vo.CourseDetailVO;
import com.bdis.modules.course.vo.CourseListVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseServiceImpl.class);
    private static final String BIZ_TYPE = "edu_course";
    private static final String AUDIT_MODULE = "M12_COURSE";

    private final CourseMapper courseMapper;
    private final ExperimentStepMapper experimentStepMapper;
    private final ExperimentStepService experimentStepService;
    private final CourseResourceService courseResourceService;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public CourseServiceImpl(
            CourseMapper courseMapper,
            ExperimentStepMapper experimentStepMapper,
            ExperimentStepService experimentStepService,
            CourseResourceService courseResourceService,
            UserMapper userMapper,
            AuditLogService auditLogService) {
        this.courseMapper = courseMapper;
        this.experimentStepMapper = experimentStepMapper;
        this.experimentStepService = experimentStepService;
        this.courseResourceService = courseResourceService;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResult<CourseListVO> page(CourseQuery query) {
        CourseQuery safeQuery = query == null ? new CourseQuery() : query;
        normalizePage(safeQuery);
        Page<CourseEntity> page =
                courseMapper.selectPage(
                        Page.of(safeQuery.getPageNo(), safeQuery.getPageSize()),
                        buildQueryWrapper(safeQuery));
        List<CourseListVO> records = page.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(records, page);
    }

    @Override
    public CourseDetailVO getDetail(Long id) {
        CourseDetailVO vo = toDetailVO(requireActive(id));
        vo.setSteps(experimentStepService.listByCourseId(id));
        vo.setResources(courseResourceService.listByCourseId(id, null));
        return vo;
    }

    @Override
    @Transactional
    public CourseDetailVO create(CourseCreateRequest request) {
        validateCreate(request);
        validateTimeRange(request.getStartedAt(), request.getEndedAt());
        ensureCourseNoAvailable(request.getCourseNo(), null);
        validateTeacher(request.getTeacherId());

        LocalDateTime now = LocalDateTime.now();
        CourseEntity entity = new CourseEntity();
        entity.setCourseNo(request.getCourseNo().trim());
        entity.setCourseName(request.getCourseName().trim());
        entity.setCourseType(request.getCourseType().trim());
        entity.setTeacherId(request.getTeacherId());
        entity.setDescription(request.getDescription());
        entity.setVideoUrl(request.getVideoUrl());
        entity.setPublishStatus(CoursePublishStatus.DRAFT);
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setRemark(request.getRemark());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        entity.setVersion(0);
        courseMapper.insert(entity);
        recordAudit("CREATE", entity.getId());
        return toDetailVO(entity);
    }

    @Override
    @Transactional
    public CourseDetailVO update(Long id, CourseUpdateRequest request) {
        CourseEntity existing = requireActive(id);
        CoursePublishStatus.requireEditable(existing.getPublishStatus());
        validateUpdate(request);
        validateTimeRange(request.getStartedAt(), request.getEndedAt());
        ensureCourseNoAvailable(request.getCourseNo(), id);
        validateTeacher(request.getTeacherId());

        existing.setCourseNo(request.getCourseNo().trim());
        existing.setCourseName(request.getCourseName().trim());
        existing.setCourseType(request.getCourseType().trim());
        existing.setTeacherId(request.getTeacherId());
        existing.setDescription(request.getDescription());
        existing.setVideoUrl(request.getVideoUrl());
        existing.setStartedAt(request.getStartedAt());
        existing.setEndedAt(request.getEndedAt());
        existing.setRemark(request.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (courseMapper.updateById(existing) == 0) {
            throw new BusinessException("Course not found or already deleted");
        }
        recordAudit("UPDATE", id);
        return toDetailVO(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CourseEntity existing = requireActive(id);
        CoursePublishStatus.requireDeletable(existing.getPublishStatus());
        Long recordCount = courseMapper.countActiveExperimentRecords(id);
        if (recordCount != null && recordCount > 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Course with experiment records cannot be deleted");
        }
        if (courseMapper.deleteById(id) == 0) {
            throw new BusinessException("Course not found or already deleted");
        }
        recordAudit("DELETE", id);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        CourseEntity existing = requireActive(id);
        CoursePublishStatus.requirePublishable(existing.getPublishStatus());
        Long stepCount =
                experimentStepMapper.selectCount(
                        new LambdaQueryWrapper<ExperimentStepEntity>()
                                .eq(ExperimentStepEntity::getCourseId, id)
                                .eq(ExperimentStepEntity::getStatus, 1));
        if (stepCount == null || stepCount == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Course must have at least one active experiment step before publishing");
        }
        LocalDateTime now = LocalDateTime.now();
        existing.setPublishStatus(CoursePublishStatus.PUBLISHED);
        existing.setPublishedAt(now);
        existing.setPublishedBy(CurrentUserUtils.currentUserId());
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (courseMapper.updateById(existing) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Course publish failed");
        }
        recordAudit("PUBLISH", id);
    }

    @Override
    @Transactional
    public void offline(Long id) {
        CourseEntity existing = requireActive(id);
        CoursePublishStatus.requireOfflineable(existing.getPublishStatus());
        existing.setPublishStatus(CoursePublishStatus.OFFLINE);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (courseMapper.updateById(existing) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Course offline failed");
        }
        recordAudit("OFFLINE", id);
    }

    private CourseEntity requireActive(Long id) {
        if (id == null) {
            throw new BusinessException("Course id is required");
        }
        CourseEntity entity = courseMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Course not found");
        }
        return entity;
    }

    private void validateCreate(CourseCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getCourseNo())
                || !StringUtils.hasText(request.getCourseName())
                || !StringUtils.hasText(request.getCourseType())
                || request.getTeacherId() == null) {
            throw new BusinessException("Course number, name, type and teacher are required");
        }
    }

    private void validateUpdate(CourseUpdateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getCourseNo())
                || !StringUtils.hasText(request.getCourseName())
                || !StringUtils.hasText(request.getCourseType())
                || request.getTeacherId() == null) {
            throw new BusinessException("Course number, name, type and teacher are required");
        }
    }

    private void ensureCourseNoAvailable(String courseNo, Long excludedId) {
        CourseEntity existed = courseMapper.selectByCourseNoIncludingDeleted(courseNo.trim());
        if (existed != null && !Objects.equals(existed.getId(), excludedId)) {
            throw new BusinessException("Course number already exists");
        }
    }

    private void validateTeacher(Long teacherId) {
        UserEntity teacher = userMapper.selectById(teacherId);
        if (teacher == null || !Objects.equals(teacher.getStatus(), 1)) {
            throw new ResourceNotFoundException("Teacher not found or inactive");
        }
    }

    private void validateTimeRange(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt != null && endedAt != null && startedAt.isAfter(endedAt)) {
            throw new BusinessException("Start time must be before end time");
        }
    }

    private LambdaQueryWrapper<CourseEntity> buildQueryWrapper(CourseQuery query) {
        LambdaQueryWrapper<CourseEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    condition ->
                            condition
                                    .like(CourseEntity::getCourseNo, query.getKeyword())
                                    .or()
                                    .like(CourseEntity::getCourseName, query.getKeyword()));
        }
        wrapper.eq(
                StringUtils.hasText(query.getCourseType()),
                CourseEntity::getCourseType,
                query.getCourseType());
        wrapper.eq(query.getTeacherId() != null, CourseEntity::getTeacherId, query.getTeacherId());
        wrapper.eq(
                StringUtils.hasText(query.getPublishStatus()),
                CourseEntity::getPublishStatus,
                query.getPublishStatus());
        wrapper.eq(query.getStatus() != null, CourseEntity::getStatus, query.getStatus());
        wrapper.ge(
                query.getStartedFrom() != null,
                CourseEntity::getStartedAt,
                query.getStartedFrom());
        wrapper.le(query.getStartedTo() != null, CourseEntity::getStartedAt, query.getStartedTo());
        wrapper.orderByDesc(CourseEntity::getCreatedAt).orderByDesc(CourseEntity::getId);
        return wrapper;
    }

    private void normalizePage(CourseQuery query) {
        if (query.getPageNo() == null || query.getPageNo() < 1) {
            query.setPageNo(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }
    }

    private CourseListVO toListVO(CourseEntity entity) {
        CourseListVO vo = new CourseListVO();
        vo.setId(entity.getId());
        vo.setCourseNo(entity.getCourseNo());
        vo.setCourseName(entity.getCourseName());
        vo.setCourseType(entity.getCourseType());
        vo.setTeacherId(entity.getTeacherId());
        vo.setPublishStatus(entity.getPublishStatus());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private CourseDetailVO toDetailVO(CourseEntity entity) {
        CourseDetailVO vo = new CourseDetailVO();
        vo.setId(entity.getId());
        vo.setCourseNo(entity.getCourseNo());
        vo.setCourseName(entity.getCourseName());
        vo.setCourseType(entity.getCourseType());
        vo.setTeacherId(entity.getTeacherId());
        if (entity.getTeacherId() != null) {
            UserEntity teacher = userMapper.selectById(entity.getTeacherId());
            if (teacher != null && Objects.equals(teacher.getStatus(), 1)) {
                vo.setTeacherName(teacher.getRealName());
            }
        }
        vo.setDescription(entity.getDescription());
        vo.setVideoUrl(entity.getVideoUrl());
        vo.setPublishStatus(entity.getPublishStatus());
        vo.setPublishedAt(entity.getPublishedAt());
        vo.setPublishedBy(entity.getPublishedBy());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setUpdatedBy(entity.getUpdatedBy());
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
            LOGGER.warn("Failed to persist course audit log", exception);
        }
    }
}
