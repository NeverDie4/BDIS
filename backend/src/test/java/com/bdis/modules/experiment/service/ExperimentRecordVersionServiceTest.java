package com.bdis.modules.experiment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.constant.ExperimentArchiveStatus;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.entity.ExperimentRecordVersionEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.experiment.mapper.ExperimentRecordVersionMapper;
import com.bdis.modules.experiment.request.ExperimentRecordVersionRequest;
import com.bdis.modules.experiment.service.impl.ExperimentRecordVersionServiceImpl;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ExperimentRecordVersionServiceTest {
    @Mock private ExperimentRecordMapper recordMapper;
    @Mock private ExperimentRecordVersionMapper versionMapper;
    @Mock private FileBusinessService fileBusinessService;
    private ExperimentRecordVersionService service;

    @BeforeEach
    void setUp() {
        service =
                new ExperimentRecordVersionServiceImpl(
                        recordMapper, versionMapper, fileBusinessService);
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
    void creatingVersionForReturnedRecordResubmitsMainRecord() {
        ExperimentRecordEntity record = new ExperimentRecordEntity();
        record.setId(1L);
        record.setRecorderId(7L);
        record.setCourseId(2L);
        record.setRecordedAt(LocalDateTime.now().minusHours(1));
        record.setArchiveStatus(ExperimentArchiveStatus.RETURNED);
        record.setArchiveComment("Please revise the result");
        record.setVersion(4);
        when(recordMapper.selectById(1L)).thenReturn(record);
        CourseEntity course = new CourseEntity();
        course.setId(2L);
        course.setStatus(1);
        course.setIsDeleted(0);
        course.setPublishStatus("published");
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(course);
        FileBusinessVO reportBinding = new FileBusinessVO();
        reportBinding.setFileId(99L);
        reportBinding.setFileUsage("report");
        when(fileBusinessService.listBindingsByBusiness("edu_experiment_record", 1L))
                .thenReturn(List.of(reportBinding));
        when(versionMapper.selectByRecordId(1L)).thenReturn(List.of());
        when(versionMapper.insert(any(ExperimentRecordVersionEntity.class))).thenReturn(1);
        when(recordMapper.resubmitVersionByIdAndVersion(
                        eq(1L),
                        eq(4),
                        eq(7L),
                        any(LocalDateTime.class),
                        eq("Revised report"),
                        eq("Revised process"),
                        eq("Revised result"),
                        eq(99L)))
                .thenReturn(1);
        ExperimentRecordVersionRequest request = new ExperimentRecordVersionRequest();
        request.setExperimentTitle("Revised report");
        request.setExperimentProcess("Revised process");
        request.setExperimentResult("Revised result");
        request.setReportFileId(99L);

        service.create(1L, request);

        assertThat(record.getExperimentTitle()).isEqualTo("Revised report");
        assertThat(record.getExperimentProcess()).isEqualTo("Revised process");
        assertThat(record.getExperimentResult()).isEqualTo("Revised result");
        assertThat(record.getReportFileId()).isEqualTo(99L);
        assertThat(record.getArchiveComment()).isNull();
        verify(recordMapper)
                .resubmitVersionByIdAndVersion(
                        eq(1L),
                        eq(4),
                        eq(7L),
                        any(LocalDateTime.class),
                        eq("Revised report"),
                        eq("Revised process"),
                        eq("Revised result"),
                        eq(99L));
    }

    @Test
    void versionCannotSubmitWithoutProcessAndResult() {
        ExperimentRecordEntity record = returnedCourseRecord();
        when(recordMapper.selectById(1L)).thenReturn(record);
        ExperimentRecordVersionRequest request = new ExperimentRecordVersionRequest();
        request.setExperimentTitle("Revised report");
        request.setExperimentProcess(" ");
        request.setExperimentResult(null);

        assertThatThrownBy(() -> service.create(1L, request))
                .isInstanceOf(com.bdis.common.exception.BusinessException.class)
                .hasMessageContaining("process");
    }

    @Test
    void versionCannotSubmitAgainstMissingSource() {
        ExperimentRecordEntity record = returnedCourseRecord();
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(1L, completeRequest(null)))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Course");
    }

    @Test
    void versionCannotReferenceFileNotBoundAsReport() {
        ExperimentRecordEntity record = returnedCourseRecord();
        when(recordMapper.selectById(1L)).thenReturn(record);
        CourseEntity course = new CourseEntity();
        course.setId(2L);
        course.setStatus(1);
        course.setIsDeleted(0);
        course.setPublishStatus("published");
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(course);
        when(fileBusinessService.listBindingsByBusiness("edu_experiment_record", 1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(1L, completeRequest(99L)))
                .isInstanceOf(com.bdis.common.exception.BusinessException.class)
                .hasMessageContaining("bound");
    }

    @Test
    void resubmissionSqlClearsPreviousArchiveComment() {
        Update update =
                Arrays.stream(ExperimentRecordMapper.class.getDeclaredMethods())
                        .filter(method -> "resubmitVersionByIdAndVersion".equals(method.getName()))
                        .findFirst()
                        .orElseThrow()
                        .getAnnotation(Update.class);

        assertThat(String.join(" ", update.value())).contains("archive_comment = NULL");
    }

    @Test
    void otherLearnerCannotReadExperimentReportVersions() {
        CurrentUser other =
                new CurrentUser(
                        8L,
                        "other",
                        "Other",
                        null,
                        null,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of("edu:experiment-record:detail"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(other, "n/a"));
        ExperimentRecordEntity record = new ExperimentRecordEntity();
        record.setId(1L);
        record.setRecorderId(7L);
        record.setCourseId(2L);
        record.setIsDeleted(0);
        when(recordMapper.selectById(1L)).thenReturn(record);
        CourseEntity course = new CourseEntity();
        course.setId(2L);
        course.setTeacherId(9L);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(course);

        assertThatThrownBy(() -> service.list(1L)).isInstanceOf(ForbiddenException.class);
    }

    private ExperimentRecordEntity returnedCourseRecord() {
        ExperimentRecordEntity record = new ExperimentRecordEntity();
        record.setId(1L);
        record.setRecorderId(7L);
        record.setCourseId(2L);
        record.setRecordedAt(LocalDateTime.now().minusHours(1));
        record.setArchiveStatus(ExperimentArchiveStatus.RETURNED);
        record.setVersion(4);
        return record;
    }

    private ExperimentRecordVersionRequest completeRequest(Long reportFileId) {
        ExperimentRecordVersionRequest request = new ExperimentRecordVersionRequest();
        request.setExperimentTitle("Revised report");
        request.setExperimentProcess("Revised process");
        request.setExperimentResult("Revised result");
        request.setReportFileId(reportFileId);
        return request;
    }
}
