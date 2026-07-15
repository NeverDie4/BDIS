package com.bdis.modules.course.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.course.entity.CourseEnrollmentEntity;
import com.bdis.modules.course.entity.CourseLearningProgressEntity;
import com.bdis.modules.course.mapper.CourseEnrollmentMapper;
import com.bdis.modules.course.mapper.CourseLearningProgressMapper;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.request.CourseLearningProgressRequest;
import com.bdis.modules.course.service.CourseLearningProgressService;
import com.bdis.modules.course.vo.CourseLearningProgressVO;
import com.bdis.modules.course.vo.CourseLearningSummaryVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseLearningProgressServiceImpl implements CourseLearningProgressService {
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseLearningProgressMapper progressMapper;

    public CourseLearningProgressServiceImpl(CourseMapper courseMapper, CourseEnrollmentMapper enrollmentMapper,
            CourseLearningProgressMapper progressMapper) {
        this.courseMapper = courseMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.progressMapper = progressMapper;
    }

    @Override @Transactional(readOnly = true)
    public List<CourseLearningProgressVO> list(Long courseId) {
        return progressMapper.selectVOByEnrollment(requireEnrollment(courseId).getId());
    }

    @Override @Transactional
    public CourseLearningProgressVO save(Long courseId, CourseLearningProgressRequest request) {
        CourseEnrollmentEntity enrollment = requireEnrollment(courseId);
        validateItem(courseId, request, enrollment.getId());
        LocalDateTime now = LocalDateTime.now();
        CourseLearningProgressEntity entity = progressMapper.selectActive(enrollment.getId(), request.getItemType(), request.getItemId());
        if (entity == null) {
            entity = new CourseLearningProgressEntity();
            entity.setEnrollmentId(enrollment.getId()); entity.setCourseId(courseId); entity.setUserId(enrollment.getUserId());
            entity.setItemType(request.getItemType()); entity.setItemId(request.getItemId()); entity.setFirstAccessedAt(now);
            entity.setStatus(1); entity.setIsDeleted(0); entity.setCreatedAt(now); entity.setCreatedBy(enrollment.getUserId()); entity.setVersion(0);
        }
        entity.setProgressValue(request.getProgressValue() == null ? (request.getCompleted() ? BigDecimal.valueOf(100) : BigDecimal.ZERO) : request.getProgressValue());
        entity.setProgressSeconds(request.getProgressSeconds()); entity.setTotalSeconds(request.getTotalSeconds());
        entity.setCompleted(request.getCompleted() ? 1 : 0); entity.setLastAccessedAt(now);
        entity.setCompletedAt(request.getCompleted() ? now : null); entity.setUpdatedAt(now); entity.setUpdatedBy(enrollment.getUserId());
        if (entity.getId() == null) progressMapper.insert(entity); else progressMapper.updateById(entity);
        refreshEnrollment(enrollment, now);
        return progressMapper.selectVOByEnrollment(enrollment.getId()).stream()
                .filter(item -> item.getItemType().equals(request.getItemType()) && item.getItemId().equals(request.getItemId()))
                .findFirst().orElseThrow(() -> new BusinessException("Learning progress was not saved"));
    }

    @Override @Transactional(readOnly = true)
    public CourseLearningSummaryVO summary(Long courseId) {
        if (courseMapper.selectById(courseId) == null) throw new ResourceNotFoundException("Course not found");
        CourseLearningSummaryVO vo = new CourseLearningSummaryVO(); vo.setCourseId(courseId);
        vo.setEnrollmentCount(progressMapper.countEnrollments(courseId));
        vo.setCompletedStepCount(progressMapper.countCompletedSteps(courseId));
        vo.setSubmittedReportCount(progressMapper.countSubmittedReports(courseId));
        vo.setUngradedReportCount(progressMapper.countUngradedReports(courseId));
        List<BigDecimal> scores = progressMapper.selectScores(courseId);
        if (scores == null) scores = Collections.emptyList();
        vo.setAverageScore(scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(Math.max(1, scores.size())), 2, RoundingMode.HALF_UP));
        vo.setExcellentCount((int) scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(90)) >= 0).count());
        vo.setPassCount((int) scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(60)) >= 0 && s.compareTo(BigDecimal.valueOf(90)) < 0).count());
        vo.setFailCount((int) scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(60)) < 0).count());
        return vo;
    }

    private CourseEnrollmentEntity requireEnrollment(Long courseId) {
        Long userId = CurrentUserUtils.currentUserId();
        if (userId == null) throw new ForbiddenException("Authentication is required");
        if (courseMapper.selectById(courseId) == null) throw new ResourceNotFoundException("Course not found");
        CourseEnrollmentEntity enrollment = enrollmentMapper.selectActiveByCourseAndUser(courseId, userId);
        if (enrollment == null) throw new ForbiddenException("Please enroll in the course first");
        return enrollment;
    }

    private void validateItem(Long courseId, CourseLearningProgressRequest request, Long enrollmentId) {
        if (!"step".equals(request.getItemType()) && !"resource".equals(request.getItemType()) && !"video".equals(request.getItemType())) {
            throw new BusinessException("Unsupported learning item type");
        }
        if ("step".equals(request.getItemType()) && progressMapper.existsActiveStep(courseId, request.getItemId()) == 0) {
            throw new BusinessException("Experiment step does not belong to this course");
        }
        if ("step".equals(request.getItemType()) && progressMapper.countIncompletePreviousSteps(courseId, request.getItemId(), enrollmentId) > 0) {
            throw new BusinessException("Experiment steps must be completed in order");
        }
        if ("resource".equals(request.getItemType()) && progressMapper.existsActiveResource(courseId, request.getItemId()) == 0) {
            throw new BusinessException("Course resource does not belong to this course");
        }
    }

    private void refreshEnrollment(CourseEnrollmentEntity enrollment, LocalDateTime now) {
        int total = progressMapper.countActiveSteps(enrollment.getCourseId()) + progressMapper.countActiveResources(enrollment.getCourseId());
        int completed = progressMapper.countCompleted(enrollment.getId());
        BigDecimal progress = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        enrollment.setProgress(progress); enrollment.setEnrollmentStatus(progress.compareTo(BigDecimal.valueOf(100)) >= 0 ? "completed" : "enrolled");
        enrollment.setCompletedAt(progress.compareTo(BigDecimal.valueOf(100)) >= 0 ? now : null); enrollment.setLastAccessedAt(now);
        enrollment.setUpdatedAt(now); enrollment.setUpdatedBy(enrollment.getUserId()); enrollmentMapper.updateById(enrollment);
    }
}
