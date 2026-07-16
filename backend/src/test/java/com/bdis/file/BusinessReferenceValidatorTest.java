package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.BusinessReferenceAccessService;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BusinessReferenceValidatorTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private AuthorizationService authorizationService;
    @Mock private DataScopeService dataScopeService;

    @Test
    void eduCourseReferenceRequiresAnActiveCourse() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(0L);
        BusinessReferenceAccessService validator = validator();

        assertThatThrownBy(() -> validator.validate("edu_course", 11L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void eduCourseReferenceAcceptsAnExistingCourse() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(1L);
        BusinessReferenceAccessService validator = validator();

        validator.validate("edu_course", 11L);
    }

    @Test
    void researchProjectReferenceRequiresAnExistingProject() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(0L);
        BusinessReferenceAccessService validator = validator();

        assertThatThrownBy(() -> validator.validate("research_project", 12L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Research project not found");
    }

    @Test
    void researchProjectReferenceAcceptsAnExistingProject() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(1L);
        BusinessReferenceAccessService validator = validator();

        validator.validate("research_project", 12L);
    }

    @Test
    void researchProjectReferenceRejectsAUserOutsideTheProjectMembershipScope() {
        CurrentUser student =
                new CurrentUser(
                        5L,
                        "student",
                        "Student",
                        1L,
                        10L,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of("research:project:detail"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(student, null, List.of()));
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(1L);
        when(jdbcTemplate.queryForList(contains("select leader_id"), eq(12L)))
                .thenReturn(List.of(Map.of("leader_id", 9L)));
        when(jdbcTemplate.queryForObject(
                        contains("rel_project_member"), eq(Long.class), eq(12L), eq(5L)))
                .thenReturn(0L);

        assertThatThrownBy(() -> validator().validate("research_project", 12L))
                .isInstanceOf(com.bdis.common.exception.ForbiddenException.class)
                .hasMessage("业务对象超出当前数据范围");
    }

    @Test
    void performanceSourceAcceptsStudentProjectMembershipWithoutProjectDetailPermission() {
        CurrentUser student =
                new CurrentUser(
                        5L,
                        "student",
                        "Student",
                        1L,
                        10L,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(student, null, List.of()));
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(1L);
        when(jdbcTemplate.queryForList(contains("select leader_id"), eq(12L)))
                .thenReturn(List.of(Map.of("leader_id", 5L)));

        assertDoesNotThrow(() -> validator().validatePerformanceSource("research_project", 12L));
        verifyNoInteractions(authorizationService);
    }

    @Test
    void experimentRecordReferenceAcceptsExistingRecord() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(1L);

        assertDoesNotThrow(() -> validator().validate("edu_experiment_record", 13L));
    }

    @Test
    void experimentRecordReferenceRejectsMissingOrLogicallyDeletedRecord() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Long.class)))
                .thenReturn(0L);

        assertThatThrownBy(() -> validator().validate("edu_experiment_record", 13L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experiment record not found");
    }

    @Test
    void experimentRecordReferenceRejectsNullZeroAndNegativeIds() {
        assertThatThrownBy(() -> validator().validate("edu_experiment_record", null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator().validate("edu_experiment_record", 0L))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator().validate("edu_experiment_record", -1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownBusinessTypesAreRejected() {
        assertThatThrownBy(() -> validator().validate("unknown_business", 13L))
                .isInstanceOf(BusinessException.class);
    }

    private BusinessReferenceAccessService validator() {
        lenient().when(authorizationService.hasPermission(any(String.class))).thenReturn(true);
        return new BusinessReferenceAccessService(
                jdbcTemplate, authorizationService, dataScopeService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
