package com.bdis.modules.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.ExperimentStepMapper;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.request.CourseCreateRequest;
import com.bdis.modules.course.request.CourseUpdateRequest;
import com.bdis.modules.course.service.impl.CourseServiceImpl;
import com.bdis.modules.course.service.CourseResourceService;
import com.bdis.modules.course.service.ExperimentStepService;
import com.bdis.modules.course.vo.CourseDetailVO;
import com.bdis.modules.course.vo.CourseListVO;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.course.vo.ExperimentStepVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseMapper courseMapper;

    @Mock private ExperimentStepMapper experimentStepMapper;

    @Mock private ExperimentStepService experimentStepService;

    @Mock private CourseResourceService courseResourceService;

    @Mock private UserMapper userMapper;

    @Mock private AuditLogService auditLogService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService =
                new CourseServiceImpl(
                        courseMapper,
                        experimentStepMapper,
                        experimentStepService,
                        courseResourceService,
                        userMapper,
                        auditLogService);
    }

    @Test
    void createCourseUsesDraftStatusAndRecordsAudit() {
        CourseCreateRequest request = validCreateRequest();
        UserEntity teacher = new UserEntity();
        teacher.setId(7L);
        teacher.setStatus(1);
        when(courseMapper.selectByCourseNoIncludingDeleted(anyString())).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(teacher);
        when(courseMapper.insert(any(CourseEntity.class))).thenAnswer(invocation -> {
            CourseEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return 1;
        });

        CourseDetailVO result = courseService.create(request);

        ArgumentCaptor<CourseEntity> entityCaptor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseMapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getPublishStatus()).isEqualTo("draft");
        assertThat(entityCaptor.getValue().getCourseNo()).isEqualTo("C-001");
        assertThat(result.getId()).isEqualTo(11L);
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getBizType()).isEqualTo("edu_course");
        assertThat(auditCaptor.getValue().getOperationType()).isEqualTo("CREATE");
    }

    @Test
    void createCourseRejectsDuplicateCourseNo() {
        CourseEntity deleted = activeCourse();
        deleted.setIsDeleted(1);
        when(courseMapper.selectByCourseNoIncludingDeleted("C-001")).thenReturn(deleted);

        assertThatThrownBy(() -> courseService.create(validCreateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Course number already exists");
    }

    @Test
    void createCourseRejectsMissingRequiredField() {
        CourseCreateRequest request = validCreateRequest();
        request.setCourseName(" ");

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("required");
    }

    @Test
    void createCourseRejectsStartAfterEnd() {
        CourseCreateRequest request = validCreateRequest();
        request.setStartedAt(LocalDateTime.of(2026, 7, 12, 10, 0));
        request.setEndedAt(LocalDateTime.of(2026, 7, 12, 9, 0));

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    void updateCourseRejectsDirectPublishStatusChange() {
        CourseEntity published = activeCourse();
        published.setPublishStatus("published");
        when(courseMapper.selectById(11L)).thenReturn(published);
        CourseUpdateRequest request = new CourseUpdateRequest();
        request.setCourseNo("C-001");
        request.setCourseName("Updated course");
        request.setCourseType("experiment");
        request.setTeacherId(7L);

        assertThatThrownBy(() -> courseService.update(11L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Published course cannot be edited");
    }

    @Test
    void updateCourseRejectsDuplicateCourseNo() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        CourseEntity other = activeCourse();
        other.setId(12L);
        other.setCourseNo("C-002");
        when(courseMapper.selectByCourseNoIncludingDeleted("C-002")).thenReturn(other);

        CourseUpdateRequest request = validUpdateRequest();
        request.setCourseNo("C-002");

        assertThatThrownBy(() -> courseService.update(11L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Course number already exists");
    }

    @Test
    void updateCourseRecordsAuditAfterSuccessfulUpdate() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        UserEntity teacher = new UserEntity();
        teacher.setId(7L);
        teacher.setStatus(1);
        when(userMapper.selectById(7L)).thenReturn(teacher);
        when(courseMapper.selectByCourseNoIncludingDeleted(anyString())).thenReturn(null);
        when(courseMapper.updateById(any(CourseEntity.class))).thenReturn(1);

        courseService.update(11L, validUpdateRequest());

        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getOperationType()).isEqualTo("UPDATE");
        assertThat(auditCaptor.getValue().getBizId()).isEqualTo(11L);
    }

    @Test
    void updateCourseAllowsKeepingItsOwnCourseNo() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(courseMapper.selectByCourseNoIncludingDeleted("C-001"))
                .thenReturn(activeCourse());
        UserEntity teacher = new UserEntity();
        teacher.setId(7L);
        teacher.setStatus(1);
        when(userMapper.selectById(7L)).thenReturn(teacher);
        when(courseMapper.updateById(any(CourseEntity.class))).thenReturn(1);

        CourseDetailVO result = courseService.update(11L, validUpdateRequest());

        assertThat(result.getCourseNo()).isEqualTo("C-001");
    }

    @Test
    void getDetailRejectsMissingCourse() {
        when(courseMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> courseService.getDetail(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void getDetailAggregatesStepsAndResources() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        ExperimentStepVO step = new ExperimentStepVO();
        step.setId(21L);
        CourseResourceVO resource = new CourseResourceVO();
        resource.setId(41L);
        when(experimentStepService.listByCourseId(11L)).thenReturn(List.of(step));
        when(courseResourceService.listByCourseId(11L, null)).thenReturn(List.of(resource));

        CourseDetailVO result = courseService.getDetail(11L);

        assertThat(result.getSteps()).containsExactly(step);
        assertThat(result.getResources()).containsExactly(resource);
    }

    @Test
    void deleteRejectsMissingCourse() {
        when(courseMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> courseService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void publishCourseRequiresAnActiveExperimentStep() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(experimentStepMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> courseService.publish(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one active experiment step");
    }

    @Test
    void publishCourseUpdatesStatusAndAudit() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(experimentStepMapper.selectCount(any())).thenReturn(1L);
        when(courseMapper.updateById(any(CourseEntity.class))).thenReturn(1);

        courseService.publish(11L);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPublishStatus()).isEqualTo("published");
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getOperationType()).isEqualTo("PUBLISH");
    }

    @Test
    void offlineCourseRequiresPublishedStatusAndPreservesPublicationMetadata() {
        CourseEntity published = activeCourse();
        published.setPublishStatus("published");
        published.setPublishedAt(LocalDateTime.of(2026, 7, 11, 10, 0));
        published.setPublishedBy(8L);
        when(courseMapper.selectById(11L)).thenReturn(published);
        when(courseMapper.updateById(any(CourseEntity.class))).thenReturn(1);

        courseService.offline(11L);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPublishStatus()).isEqualTo("offline");
        assertThat(captor.getValue().getPublishedBy()).isEqualTo(8L);
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void offlineCourseCanBePublishedAgain() {
        CourseEntity offline = activeCourse();
        offline.setPublishStatus("offline");
        when(courseMapper.selectById(11L)).thenReturn(offline);
        when(experimentStepMapper.selectCount(any())).thenReturn(1L);
        when(courseMapper.updateById(any(CourseEntity.class))).thenReturn(1);

        courseService.publish(11L);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPublishStatus()).isEqualTo("published");
    }

    @Test
    void publishedCourseCannotBePublishedAgain() {
        CourseEntity published = activeCourse();
        published.setPublishStatus("published");
        when(courseMapper.selectById(11L)).thenReturn(published);

        assertThatThrownBy(() -> courseService.publish(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only draft or offline");
    }

    @Test
    void draftCourseCannotBeTakenOffline() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());

        assertThatThrownBy(() -> courseService.offline(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only published course");
    }

    @Test
    void offlineCourseCannotBeTakenOfflineAgain() {
        CourseEntity offline = activeCourse();
        offline.setPublishStatus("offline");
        when(courseMapper.selectById(11L)).thenReturn(offline);

        assertThatThrownBy(() -> courseService.offline(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only published course");
    }

    @Test
    void publishedCourseCannotBeDeleted() {
        CourseEntity published = activeCourse();
        published.setPublishStatus("published");
        when(courseMapper.selectById(11L)).thenReturn(published);

        assertThatThrownBy(() -> courseService.delete(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Published course cannot be deleted");
    }

    @Test
    void pageReturnsMappedCourseList() {
        CourseQuery query = new CourseQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        when(courseMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CourseEntity> page =
                    invocation.getArgument(0);
            page.setRecords(List.of(activeCourse()));
            page.setTotal(1);
            return page;
        });

        PageResult<CourseListVO> result = courseService.page(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).singleElement().satisfies(vo ->
                assertThat(vo.getCourseNo()).isEqualTo("C-001"));
    }

    @Test
    void deleteCourseRequiresActiveCourseAndRecordsAudit() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(courseMapper.deleteById(11L)).thenReturn(1);

        courseService.delete(11L);

        verify(courseMapper).deleteById(11L);
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getOperationType()).isEqualTo("DELETE");
    }

    private CourseCreateRequest validCreateRequest() {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setCourseNo("C-001");
        request.setCourseName("Basic experiment");
        request.setCourseType("experiment");
        request.setTeacherId(7L);
        return request;
    }

    private CourseUpdateRequest validUpdateRequest() {
        CourseUpdateRequest request = new CourseUpdateRequest();
        request.setCourseNo("C-001");
        request.setCourseName("Updated course");
        request.setCourseType("experiment");
        request.setTeacherId(7L);
        return request;
    }

    private CourseEntity activeCourse() {
        CourseEntity entity = new CourseEntity();
        entity.setId(11L);
        entity.setCourseNo("C-001");
        entity.setCourseName("Basic experiment");
        entity.setCourseType("experiment");
        entity.setTeacherId(7L);
        entity.setPublishStatus("draft");
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }
}
