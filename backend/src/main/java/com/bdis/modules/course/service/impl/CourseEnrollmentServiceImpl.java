package com.bdis.modules.course.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.modules.course.constant.CoursePublishStatus;
import com.bdis.modules.course.entity.CourseEnrollmentEntity;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseEnrollmentMapper;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.service.CourseEnrollmentService;
import com.bdis.modules.course.vo.CourseEnrollmentVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseEnrollmentServiceImpl implements CourseEnrollmentService {
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;

    public CourseEnrollmentServiceImpl(CourseEnrollmentMapper enrollmentMapper, CourseMapper courseMapper) {
        this.enrollmentMapper = enrollmentMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    @Transactional
    public CourseEnrollmentVO enroll(Long courseId) {
        CourseEntity course = requirePublishedCourse(courseId);
        Long userId = requireUserId();
        if (!isStudent() && !isAdmin()) {
            throw new ForbiddenException("Only students can enroll in a course");
        }
        if (enrollmentMapper.selectActiveByCourseAndUser(course.getId(), userId) != null) {
            throw new BusinessException("Student is already enrolled in this course");
        }
        validatePrerequisites(course, userId);
        LocalDateTime now = LocalDateTime.now();
        CourseEnrollmentEntity entity = new CourseEnrollmentEntity();
        entity.setCourseId(course.getId());
        entity.setUserId(userId);
        entity.setEnrollmentStatus("enrolled");
        entity.setProgress(BigDecimal.ZERO);
        entity.setEnrolledAt(now);
        entity.setLastAccessedAt(now);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setVersion(0);
        if (enrollmentMapper.insert(entity) == 0) {
            throw new BusinessException("Course enrollment failed");
        }
        CourseEnrollmentVO vo = new CourseEnrollmentVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setUserId(entity.getUserId());
        vo.setEnrollmentStatus(entity.getEnrollmentStatus());
        vo.setProgress(entity.getProgress());
        vo.setEnrolledAt(entity.getEnrolledAt());
        return vo;
    }

    private void validatePrerequisites(CourseEntity course, Long userId) {
        if (course.getPrerequisites() == null || course.getPrerequisites().isBlank()) return;
        for (String token : course.getPrerequisites().split("[,，\\s]+")) {
            if (token.isBlank()) continue;
            try {
                Long prerequisiteId = Long.valueOf(token.trim());
                if (enrollmentMapper.countCompleted(prerequisiteId, userId) == 0) {
                    throw new BusinessException("Prerequisite course is not completed: " + prerequisiteId);
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEnrollmentVO> listMine() {
        return enrollmentMapper.selectVOByUserId(requireUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEnrollmentVO> listByCourse(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        Long userId = requireUserId();
        if (!isAdmin() && !Objects.equals(course.getTeacherId(), userId)
                && !Objects.equals(course.getCreatedBy(), userId)) {
            throw new ForbiddenException("Only the course owner can view enrollments");
        }
        return enrollmentMapper.selectVOByCourseId(courseId);
    }

    private CourseEntity requirePublishedCourse(Long courseId) {
        CourseEntity course = requireCourse(courseId);
        if (!CoursePublishStatus.PUBLISHED.equals(course.getPublishStatus())) {
            throw new ForbiddenException("Only published courses can be enrolled in");
        }
        return course;
    }

    private CourseEntity requireCourse(Long courseId) {
        if (courseId == null || courseId <= 0) throw new BusinessException("Course id is required");
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null || Objects.equals(course.getIsDeleted(), 1)) throw new ResourceNotFoundException("Course not found");
        if (!Objects.equals(course.getStatus(), 1)) throw new BusinessException("Course is disabled");
        return course;
    }

    private Long requireUserId() {
        Long userId = CurrentUserUtils.currentUserId();
        if (userId == null || userId <= 0) throw new ForbiddenException("Authentication is required");
        return userId;
    }

    private boolean isStudent() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch(role -> "STUDENT".equalsIgnoreCase(role));
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream().anyMatch(SecurityConstants.ADMIN_ROLE_CODE::equalsIgnoreCase);
    }
}
