package com.bdis.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessReferenceValidatorTest {

    @Mock private CourseMapper courseMapper;
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ExperimentRecordMapper experimentRecordMapper;

    @Test
    void eduCourseReferenceRequiresAnActiveCourse() {
        when(courseMapper.selectById(11L)).thenReturn(null);
        BusinessReferenceValidator validator = validator();

        assertThatThrownBy(() -> validator.validate("edu_course", 11L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void eduCourseReferenceAcceptsAnExistingCourse() {
        CourseEntity course = new CourseEntity();
        course.setId(11L);
        when(courseMapper.selectById(11L)).thenReturn(course);
        BusinessReferenceValidator validator = validator();

        validator.validate("edu_course", 11L);
    }

    @Test
    void researchProjectReferenceRequiresAnExistingProject() {
        when(projectMapper.selectById(12L)).thenReturn(null);
        BusinessReferenceValidator validator = validator();

        assertThatThrownBy(() -> validator.validate("research_project", 12L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Research project not found");
    }

    @Test
    void researchProjectReferenceAcceptsAnExistingProject() {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(12L);
        when(projectMapper.selectById(12L)).thenReturn(project);
        BusinessReferenceValidator validator = validator();

        validator.validate("research_project", 12L);
    }

    @Test
    void experimentRecordReferenceAcceptsExistingRecordInEveryWorkflowState() {
        when(experimentRecordMapper.existsActiveReferenceById(13L)).thenReturn(true);

        assertDoesNotThrow(() -> validator().validate("edu_experiment_record", 13L));
        assertDoesNotThrow(() -> validator().validate("edu_experiment_record", 13L));
        assertDoesNotThrow(() -> validator().validate("edu_experiment_record", 13L));

        verify(experimentRecordMapper, org.mockito.Mockito.times(3))
                .existsActiveReferenceById(13L);
    }

    @Test
    void experimentRecordReferenceRejectsMissingOrLogicallyDeletedRecord() {
        when(experimentRecordMapper.existsActiveReferenceById(13L)).thenReturn(false);

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
        verifyNoInteractions(experimentRecordMapper);
    }

    @Test
    void otherBusinessTypesDoNotAccidentallyUseExperimentRecordValidator() {
        validator().validate("unknown_business", 13L);
        verifyNoInteractions(experimentRecordMapper);
    }

    private BusinessReferenceValidator validator() {
        return new BusinessReferenceValidator(
                courseMapper, projectMapper, experimentRecordMapper);
    }
}
