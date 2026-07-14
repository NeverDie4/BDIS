package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.BusinessReferenceAccessService;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
