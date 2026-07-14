package com.bdis.modules.course.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CourseFileBusinessAccessPolicyTest {

    @Mock private CourseMapper courseMapper;
    @Mock private AuthorizationService authorizationService;

    private CourseFileBusinessAccessPolicy policy;
    private CourseEntity course;

    @BeforeEach
    void setUp() {
        policy = new CourseFileBusinessAccessPolicy(courseMapper, authorizationService);
        course = new CourseEntity();
        course.setId(1L);
        course.setCreatedBy(10L);
        course.setTeacherId(10L);
        course.setPublishStatus("published");
        Mockito.lenient().when(authorizationService.hasPermission(anyString())).thenReturn(true);
        Mockito.lenient().when(courseMapper.selectById(1L)).thenReturn(course);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanViewAttachDetachAndPublish() {
        authenticate(10L, "TEACHER");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isTrue();
        assertThat(policy.canDetach(1L)).isTrue();
        assertThat(policy.canPublish(1L)).isTrue();
    }

    @Test
    void unrelatedUserCannotViewOrManageCourse() {
        authenticate(20L, "TEACHER");

        assertThat(policy.canView(1L)).isFalse();
        assertThat(policy.canAttach(1L)).isFalse();
        assertThat(policy.canDetach(1L)).isFalse();
        assertThat(policy.canPublish(1L)).isFalse();
    }

    @Test
    void administratorCanViewAndManageCourse() {
        authenticate(30L, "ADMIN");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isTrue();
        assertThat(policy.canDetach(1L)).isTrue();
        assertThat(policy.canPublish(1L)).isTrue();
    }

    @Test
    void missingOrDeletedCourseIsRejected() {
        authenticate(10L, "TEACHER");
        when(courseMapper.selectById(2L)).thenReturn(null);

        assertThat(policy.exists(2L)).isFalse();
        assertThat(policy.canView(2L)).isFalse();
        assertThat(policy.canAttach(2L)).isFalse();
        assertThat(policy.canDetach(2L)).isFalse();
        assertThat(policy.canPublish(2L)).isFalse();
    }

    private void authenticate(Long userId, String role) {
        CurrentUser user =
                new CurrentUser(
                        userId,
                        "user-" + userId,
                        "User " + userId,
                        null,
                        null,
                        Set.of(role),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
}
