package com.bdis.modules.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.course.entity.CourseEnrollmentEntity;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseEnrollmentMapper;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.service.impl.CourseEnrollmentServiceImpl;
import com.bdis.modules.course.vo.CourseEnrollmentVO;
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
class CourseEnrollmentServiceTest {

    @Mock private CourseEnrollmentMapper enrollmentMapper;
    @Mock private CourseMapper courseMapper;

    private CourseEnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new CourseEnrollmentServiceImpl(enrollmentMapper, courseMapper);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentCanEnrollInPublishedCourse() {
        setUser(8L, "STUDENT");
        CourseEntity course = course("published");
        when(courseMapper.selectById(11L)).thenReturn(course);
        when(enrollmentMapper.selectActiveByCourseAndUser(11L, 8L)).thenReturn(null);
        when(enrollmentMapper.insert(any(CourseEnrollmentEntity.class)))
                .thenAnswer(
                        invocation -> {
                            ((com.bdis.modules.course.entity.CourseEnrollmentEntity)
                                            invocation.getArgument(0))
                                    .setId(41L);
                            return 1;
                        });

        CourseEnrollmentVO result = service.enroll(11L);

        assertThat(result.getId()).isEqualTo(41L);
        assertThat(result.getCourseId()).isEqualTo(11L);
        assertThat(result.getUserId()).isEqualTo(8L);
    }

    @Test
    void studentCannotEnrollInDraftCourse() {
        setUser(8L, "STUDENT");
        when(courseMapper.selectById(11L)).thenReturn(course("draft"));

        assertThatThrownBy(() -> service.enroll(11L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void duplicateEnrollmentIsRejected() {
        setUser(8L, "STUDENT");
        when(courseMapper.selectById(11L)).thenReturn(course("published"));
        when(enrollmentMapper.selectActiveByCourseAndUser(11L, 8L))
                .thenReturn(new CourseEnrollmentEntity());

        assertThatThrownBy(() -> service.enroll(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already enrolled");
    }

    private void setUser(Long id, String role) {
        CurrentUser user =
                new CurrentUser(
                        id,
                        "user-" + id,
                        "User",
                        null,
                        null,
                        Set.of(role),
                        Set.of(),
                        Set.of("edu:course:enroll"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    private CourseEntity course(String status) {
        CourseEntity course = new CourseEntity();
        course.setId(11L);
        course.setPublishStatus(status);
        course.setStatus(1);
        course.setIsDeleted(0);
        return course;
    }
}
