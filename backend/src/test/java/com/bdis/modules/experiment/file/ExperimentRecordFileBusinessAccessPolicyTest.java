package com.bdis.modules.experiment.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
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
class ExperimentRecordFileBusinessAccessPolicyTest {

    @Mock private ExperimentRecordMapper recordMapper;
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private AuthorizationService authorizationService;

    private ExperimentRecordFileBusinessAccessPolicy policy;
    private ExperimentRecordEntity record;

    @BeforeEach
    void setUp() {
        policy =
                new ExperimentRecordFileBusinessAccessPolicy(
                        recordMapper, projectMapper, authorizationService);
        record = new ExperimentRecordEntity();
        record.setId(1L);
        record.setRecorderId(10L);
        Mockito.lenient().when(authorizationService.hasPermission(anyString())).thenReturn(true);
        Mockito.lenient().when(recordMapper.selectById(1L)).thenReturn(record);
        Mockito.lenient().when(recordMapper.existsActiveReferenceById(1L)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recorderCanViewAttachDetachAndPublish() {
        authenticate(10L, "TEACHER");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isTrue();
        assertThat(policy.canDetach(1L)).isTrue();
        assertThat(policy.canPublish(1L)).isTrue();
    }

    @Test
    void unrelatedUserCannotViewOrManageRecord() {
        authenticate(20L, "TEACHER");

        assertThat(policy.canView(1L)).isFalse();
        assertThat(policy.canAttach(1L)).isFalse();
        assertThat(policy.canDetach(1L)).isFalse();
        assertThat(policy.canPublish(1L)).isFalse();
    }

    @Test
    void administratorCanViewAndManageRecord() {
        authenticate(30L, "ADMIN");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isTrue();
        assertThat(policy.canDetach(1L)).isTrue();
        assertThat(policy.canPublish(1L)).isTrue();
    }

    @Test
    void courseTeacherCanViewButCannotManageRecord() {
        record.setCourseId(7L);
        CourseEntity course = new CourseEntity();
        course.setId(7L);
        course.setTeacherId(20L);
        when(recordMapper.selectCourseByIdIncludingDeleted(7L)).thenReturn(course);
        authenticate(20L, "TEACHER");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isFalse();
        assertThat(policy.canDetach(1L)).isFalse();
        assertThat(policy.canPublish(1L)).isFalse();
    }

    @Test
    void missingOrDeletedRecordIsRejected() {
        authenticate(10L, "TEACHER");
        when(recordMapper.existsActiveReferenceById(2L)).thenReturn(false);
        when(recordMapper.selectById(2L)).thenReturn(null);

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
