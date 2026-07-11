package com.bdis.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.course.dto.CourseRequest;
import com.bdis.modules.course.dto.CourseResourceRequest;
import com.bdis.modules.course.dto.ExperimentStepRequest;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.CourseResourceEntity;
import com.bdis.modules.course.entity.ExperimentStepEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.CourseResourceMapper;
import com.bdis.modules.course.mapper.ExperimentStepMapper;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.service.CourseService;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.course.vo.CourseVO;
import com.bdis.modules.course.vo.ExperimentStepVO;
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CourseServiceImpl implements CourseService {

    private static final String COURSE_BIZ_TYPE = "edu_course";
    private static final Set<String> PUBLISH_STATUSES = Set.of("draft", "published", "archived");

    private final CourseMapper courseMapper;
    private final ExperimentStepMapper experimentStepMapper;
    private final CourseResourceMapper courseResourceMapper;
    private final UserMapper userMapper;
    private final FileResourceService fileResourceService;
    private final FileBusinessService fileBusinessService;
    private final DictionaryReferenceValidator dictionaryReferenceValidator;

    public CourseServiceImpl(
            CourseMapper courseMapper,
            ExperimentStepMapper experimentStepMapper,
            CourseResourceMapper courseResourceMapper,
            UserMapper userMapper,
            FileResourceService fileResourceService,
            FileBusinessService fileBusinessService,
            DictionaryReferenceValidator dictionaryReferenceValidator) {
        this.courseMapper = courseMapper;
        this.experimentStepMapper = experimentStepMapper;
        this.courseResourceMapper = courseResourceMapper;
        this.userMapper = userMapper;
        this.fileResourceService = fileResourceService;
        this.fileBusinessService = fileBusinessService;
        this.dictionaryReferenceValidator = dictionaryReferenceValidator;
    }

    @Override
    public PageResult<CourseVO> page(CourseQuery query) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        LambdaQueryWrapper<CourseEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    item ->
                            item.like(CourseEntity::getCourseNo, query.getKeyword())
                                    .or()
                                    .like(CourseEntity::getCourseName, query.getKeyword()));
        }
        wrapper.eq(query.getStatus() != null, CourseEntity::getStatus, query.getStatus())
                .eq(
                        StringUtils.hasText(query.getCourseType()),
                        CourseEntity::getCourseType,
                        query.getCourseType())
                .eq(
                        StringUtils.hasText(query.getPublishStatus()),
                        CourseEntity::getPublishStatus,
                        query.getPublishStatus())
                .eq(query.getTeacherId() != null, CourseEntity::getTeacherId, query.getTeacherId());
        if (!isAdmin(currentUser)) {
            if (canManage(currentUser)) {
                wrapper.and(
                        item ->
                                item.eq(CourseEntity::getPublishStatus, "published")
                                        .or()
                                        .eq(CourseEntity::getTeacherId, currentUser.getUserId()));
            } else {
                wrapper.eq(CourseEntity::getPublishStatus, "published");
            }
        }
        wrapper.orderByDesc(CourseEntity::getCreatedAt).orderByDesc(CourseEntity::getId);
        Page<CourseEntity> result =
                courseMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toSummaryVO).toList(), result);
    }

    @Override
    public CourseVO detail(Long id) {
        CourseEntity course = requireVisibleCourse(id);
        CourseVO vo = toSummaryVO(course);
        vo.setSteps(
                experimentStepMapper
                        .selectList(
                                new LambdaQueryWrapper<ExperimentStepEntity>()
                                        .eq(ExperimentStepEntity::getCourseId, id)
                                        .orderByAsc(ExperimentStepEntity::getSortOrder)
                                        .orderByAsc(ExperimentStepEntity::getId))
                        .stream()
                        .map(this::toStepVO)
                        .toList());
        vo.setResources(
                courseResourceMapper
                        .selectList(
                                new LambdaQueryWrapper<CourseResourceEntity>()
                                        .eq(CourseResourceEntity::getCourseId, id)
                                        .orderByDesc(CourseResourceEntity::getUploadedAt))
                        .stream()
                        .map(this::toResourceVO)
                        .toList());
        return vo;
    }

    @Override
    @Transactional
    public Long create(CourseRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        ensureCourseNoAvailable(request.getCourseNo(), null);
        validateCourseDictionaries(request);
        Long teacherId = resolveTeacherId(request.getTeacherId(), currentUser);
        requireUser(teacherId);
        CourseEntity entity = new CourseEntity();
        applyCourse(entity, request);
        entity.setTeacherId(teacherId);
        entity.setPublishStatus("draft");
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(currentUser.getUserId());
        courseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CourseRequest request) {
        CourseEntity entity = requireOwnedCourse(id);
        ensureCourseNoAvailable(request.getCourseNo(), id);
        validateCourseDictionaries(request);
        CurrentUser currentUser = SecurityUtils.currentUser();
        Long teacherId = resolveTeacherId(request.getTeacherId(), currentUser);
        requireUser(teacherId);
        applyCourse(entity, request);
        entity.setTeacherId(teacherId);
        entity.setUpdatedBy(currentUser.getUserId());
        courseMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CourseEntity entity = requireOwnedCourse(id);
        experimentStepMapper.delete(
                new LambdaQueryWrapper<ExperimentStepEntity>()
                        .eq(ExperimentStepEntity::getCourseId, id));
        courseResourceMapper.delete(
                new LambdaQueryWrapper<CourseResourceEntity>()
                        .eq(CourseResourceEntity::getCourseId, id));
        fileBusinessService.deleteByBusiness(COURSE_BIZ_TYPE, id);
        courseMapper.deleteById(entity.getId());
    }

    @Override
    @Transactional
    public CourseVO publish(Long id, String status) {
        if (!StringUtils.hasText(status) || !PUBLISH_STATUSES.contains(status)) {
            throw new IllegalArgumentException("课程发布状态仅支持 draft、published、archived");
        }
        CourseEntity entity = requireOwnedCourse(id);
        entity.setPublishStatus(status);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        courseMapper.updateById(entity);
        return detail(id);
    }

    @Override
    @Transactional
    public ExperimentStepVO addStep(Long courseId, ExperimentStepRequest request) {
        requireOwnedCourse(courseId);
        ensureStepNoAvailable(courseId, request.getStepNo(), null);
        ExperimentStepEntity entity = new ExperimentStepEntity();
        applyStep(entity, request);
        entity.setCourseId(courseId);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        experimentStepMapper.insert(entity);
        return toStepVO(entity);
    }

    @Override
    @Transactional
    public ExperimentStepVO updateStep(Long courseId, Long stepId, ExperimentStepRequest request) {
        requireOwnedCourse(courseId);
        ExperimentStepEntity entity = requireStep(courseId, stepId);
        ensureStepNoAvailable(courseId, request.getStepNo(), stepId);
        applyStep(entity, request);
        entity.setUpdatedBy(SecurityUtils.currentUser().getUserId());
        experimentStepMapper.updateById(entity);
        return toStepVO(entity);
    }

    @Override
    @Transactional
    public void deleteStep(Long courseId, Long stepId) {
        requireOwnedCourse(courseId);
        experimentStepMapper.deleteById(requireStep(courseId, stepId).getId());
    }

    @Override
    @Transactional
    public CourseResourceVO addResource(Long courseId, CourseResourceRequest request) {
        requireOwnedCourse(courseId);
        dictionaryReferenceValidator.validateIfConfigured(
                "course_resource_type", request.getResourceType(), "课程资源类型");
        FileResourceVO file = fileResourceService.detail(request.getFileId());
        CourseResourceEntity entity = new CourseResourceEntity();
        entity.setCourseId(courseId);
        entity.setResourceName(request.getResourceName());
        entity.setResourceType(request.getResourceType());
        entity.setFileId(file.getId());
        entity.setFileUrl(file.getFileUrl());
        entity.setFileSize(file.getFileSize());
        entity.setFileFormat(file.getFileFormat());
        entity.setUploaderId(file.getUploaderId());
        entity.setUploadedAt(file.getUploadedAt());
        entity.setDownloadCount(0);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setCreatedBy(SecurityUtils.currentUser().getUserId());
        courseResourceMapper.insert(entity);

        FileBusinessBindDTO bind = new FileBusinessBindDTO();
        bind.setFileId(file.getId());
        bind.setBizType(COURSE_BIZ_TYPE);
        bind.setBizId(courseId);
        bind.setFileUsage("course_resource");
        bind.setRemark(request.getRemark());
        fileBusinessService.bind(bind);
        return toResourceVO(entity);
    }

    @Override
    @Transactional
    public void deleteResource(Long courseId, Long resourceId) {
        requireOwnedCourse(courseId);
        CourseResourceEntity entity = requireResource(courseId, resourceId);
        courseResourceMapper.deleteById(entity.getId());
        fileBusinessService.deleteByBusinessAndFile(COURSE_BIZ_TYPE, courseId, entity.getFileId());
    }

    private CourseEntity requireVisibleCourse(Long id) {
        CourseEntity entity = requireCourse(id);
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (isAdmin(currentUser)
                || "published".equals(entity.getPublishStatus())
                || (canManage(currentUser)
                        && currentUser.getUserId().equals(entity.getTeacherId()))) {
            return entity;
        }
        throw new ForbiddenException("无权查看该课程");
    }

    private CourseEntity requireOwnedCourse(Long id) {
        CourseEntity entity = requireCourse(id);
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (isAdmin(currentUser) || currentUser.getUserId().equals(entity.getTeacherId())) {
            return entity;
        }
        throw new ForbiddenException("只能管理本人负责的课程");
    }

    private CourseEntity requireCourse(Long id) {
        CourseEntity entity = courseMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("课程不存在");
        }
        return entity;
    }

    private ExperimentStepEntity requireStep(Long courseId, Long stepId) {
        ExperimentStepEntity entity = experimentStepMapper.selectById(stepId);
        if (entity == null || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException("实验步骤不存在");
        }
        return entity;
    }

    private CourseResourceEntity requireResource(Long courseId, Long resourceId) {
        CourseResourceEntity entity = courseResourceMapper.selectById(resourceId);
        if (entity == null || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException("课程资源不存在");
        }
        return entity;
    }

    private UserEntity requireUser(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("授课教师不存在");
        }
        return entity;
    }

    private Long resolveTeacherId(Long requestedTeacherId, CurrentUser currentUser) {
        if (isAdmin(currentUser)) {
            return requestedTeacherId == null ? currentUser.getUserId() : requestedTeacherId;
        }
        if (requestedTeacherId != null && !currentUser.getUserId().equals(requestedTeacherId)) {
            throw new ForbiddenException("不能将课程转交给其他教师");
        }
        return currentUser.getUserId();
    }

    private void ensureCourseNoAvailable(String courseNo, Long excludeId) {
        CourseEntity existing =
                courseMapper.selectOne(
                        new LambdaQueryWrapper<CourseEntity>()
                                .eq(CourseEntity::getCourseNo, courseNo));
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new DuplicateResourceException("课程编号已存在");
        }
    }

    private void ensureStepNoAvailable(Long courseId, String stepNo, Long excludeId) {
        ExperimentStepEntity existing =
                experimentStepMapper.selectOne(
                        new LambdaQueryWrapper<ExperimentStepEntity>()
                                .eq(ExperimentStepEntity::getCourseId, courseId)
                                .eq(ExperimentStepEntity::getStepNo, stepNo));
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw new DuplicateResourceException("课程步骤编号已存在");
        }
    }

    private void applyCourse(CourseEntity entity, CourseRequest request) {
        entity.setCourseNo(request.getCourseNo());
        entity.setCourseName(request.getCourseName());
        entity.setCourseType(request.getCourseType());
        entity.setDescription(request.getDescription());
        entity.setVideoUrl(request.getVideoUrl());
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private void validateCourseDictionaries(CourseRequest request) {
        dictionaryReferenceValidator.validateIfConfigured(
                "course_type", request.getCourseType(), "课程类型");
    }

    private void applyStep(ExperimentStepEntity entity, ExperimentStepRequest request) {
        entity.setStepNo(request.getStepNo());
        entity.setStepTitle(request.getStepTitle());
        entity.setStepContent(request.getStepContent());
        entity.setExpectedResult(request.getExpectedResult());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private CourseVO toSummaryVO(CourseEntity entity) {
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getTeacherId() != null) {
            UserEntity teacher = userMapper.selectById(entity.getTeacherId());
            vo.setTeacherName(teacher == null ? null : teacher.getRealName());
        }
        return vo;
    }

    private ExperimentStepVO toStepVO(ExperimentStepEntity entity) {
        ExperimentStepVO vo = new ExperimentStepVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private CourseResourceVO toResourceVO(CourseResourceEntity entity) {
        CourseResourceVO vo = new CourseResourceVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getFileId() != null) {
            vo.setFileUrl("/api/files/" + entity.getFileId() + "/content");
        }
        return vo;
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser.getRoleCodes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }

    private boolean canManage(CurrentUser currentUser) {
        return currentUser.getPermissions().contains("course:manage");
    }
}
