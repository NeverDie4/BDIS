package com.bdis.modules.course.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.course.entity.CourseEnrollmentEntity;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.CourseLearningProgressEntity;
import com.bdis.modules.course.mapper.CourseEnrollmentMapper;
import com.bdis.modules.course.mapper.CourseLearningProgressMapper;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.request.CourseLearningProgressRequest;
import com.bdis.modules.course.service.impl.CourseLearningProgressServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CourseLearningProgressServiceTest {
    @Mock private CourseMapper courseMapper;
    @Mock private CourseEnrollmentMapper enrollmentMapper;
    @Mock private CourseLearningProgressMapper progressMapper;
    private CourseLearningProgressService service;

    @BeforeEach
    void setUp() {
        service =
                new CourseLearningProgressServiceImpl(
                        courseMapper, enrollmentMapper, progressMapper);
        CurrentUser user =
                new CurrentUser(
                        7L,
                        "student",
                        "Student",
                        null,
                        null,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void arbitraryVideoIdCannotCreateProgress() {
        CourseEntity course = new CourseEntity();
        course.setId(2L);
        CourseEnrollmentEntity enrollment = new CourseEnrollmentEntity();
        enrollment.setId(3L);
        enrollment.setCourseId(2L);
        enrollment.setUserId(7L);
        when(courseMapper.selectById(2L)).thenReturn(course);
        when(enrollmentMapper.selectActiveByCourseAndUser(2L, 7L)).thenReturn(enrollment);

        CourseLearningProgressRequest request = new CourseLearningProgressRequest();
        request.setItemType("video");
        request.setItemId(999L);
        request.setCompleted(true);

        assertThatThrownBy(() -> service.save(2L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Video resource does not belong to this course");
        verify(progressMapper, never())
                .insert(org.mockito.ArgumentMatchers.any(CourseLearningProgressEntity.class));
    }
}
