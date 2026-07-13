package com.bdis.modules.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.ExperimentStepEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.ExperimentStepMapper;
import com.bdis.modules.course.request.ExperimentStepCreateRequest;
import com.bdis.modules.course.request.ExperimentStepUpdateRequest;
import com.bdis.modules.course.service.impl.ExperimentStepServiceImpl;
import com.bdis.modules.course.vo.ExperimentStepVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentStepServiceTest {

    @Mock private ExperimentStepMapper stepMapper;

    @Mock private CourseMapper courseMapper;

    @Mock private AuditLogService auditLogService;

    private ExperimentStepService stepService;

    @BeforeEach
    void setUp() {
        stepService = new ExperimentStepServiceImpl(stepMapper, courseMapper, auditLogService);
    }

    @Test
    void createStepSetsDefaultSortOrderAndRecordsAudit() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(stepMapper.selectByCourseIdAndStepNoIncludingDeleted(11L, "S-01"))
                .thenReturn(null);
        when(stepMapper.insert(any(ExperimentStepEntity.class))).thenAnswer(invocation -> {
            ExperimentStepEntity entity = invocation.getArgument(0);
            entity.setId(21L);
            return 1;
        });

        ExperimentStepVO result = stepService.create(11L, createRequest());

        ArgumentCaptor<ExperimentStepEntity> captor =
                ArgumentCaptor.forClass(ExperimentStepEntity.class);
        verify(stepMapper).insert(captor.capture());
        assertThat(captor.getValue().getCourseId()).isEqualTo(11L);
        assertThat(captor.getValue().getSortOrder()).isZero();
        assertThat(result.getId()).isEqualTo(21L);
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getBizType()).isEqualTo("edu_experiment_step");
        assertThat(auditCaptor.getValue().getOperationType()).isEqualTo("CREATE");
    }

    @Test
    void createStepRejectsDuplicateStepNo() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(stepMapper.selectByCourseIdAndStepNoIncludingDeleted(11L, "S-01"))
                .thenReturn(activeStep());

        assertThatThrownBy(() -> stepService.create(11L, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Step number already exists");
    }

    @Test
    void updateStepRequiresMatchingCourseAndVersion() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        ExperimentStepEntity step = activeStep();
        when(stepMapper.selectById(21L)).thenReturn(step);
        when(stepMapper.selectByCourseIdAndStepNoIncludingDeleted(11L, "S-01"))
                .thenReturn(step);
        when(stepMapper.updateById(any(ExperimentStepEntity.class))).thenReturn(1);

        ExperimentStepUpdateRequest request = new ExperimentStepUpdateRequest();
        request.setStepNo("S-01");
        request.setStepTitle("Updated step");
        request.setVersion(0);

        ExperimentStepVO result = stepService.update(11L, 21L, request);

        assertThat(result.getStepTitle()).isEqualTo("Updated step");
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void publishedCourseCannotChangeSteps() {
        CourseEntity course = activeCourse();
        course.setPublishStatus("published");
        when(courseMapper.selectById(11L)).thenReturn(course);

        assertThatThrownBy(() -> stepService.create(11L, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Published course cannot be edited");
    }

    @Test
    void listStepsMapsActiveSteps() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(stepMapper.selectList(any())).thenReturn(List.of(activeStep()));

        List<ExperimentStepVO> result = stepService.listByCourseId(11L);

        assertThat(result).singleElement().satisfies(vo -> {
            assertThat(vo.getCourseId()).isEqualTo(11L);
            assertThat(vo.getStepNo()).isEqualTo("S-01");
        });
    }

    private ExperimentStepCreateRequest createRequest() {
        ExperimentStepCreateRequest request = new ExperimentStepCreateRequest();
        request.setStepNo("S-01");
        request.setStepTitle("Prepare sample");
        return request;
    }

    private CourseEntity activeCourse() {
        CourseEntity entity = new CourseEntity();
        entity.setId(11L);
        entity.setPublishStatus("draft");
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }

    private ExperimentStepEntity activeStep() {
        ExperimentStepEntity entity = new ExperimentStepEntity();
        entity.setId(21L);
        entity.setCourseId(11L);
        entity.setStepNo("S-01");
        entity.setStepTitle("Prepare sample");
        entity.setVersion(0);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }
}
