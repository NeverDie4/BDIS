package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.entity.TrainingReportVersionEntity;
import com.bdis.modules.training.mapper.TrainingEvaluationMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.mapper.TrainingReportVersionMapper;
import com.bdis.modules.training.request.TrainingRecordReviewRequest;
import com.bdis.modules.training.request.TrainingReportSubmitRequest;
import com.bdis.modules.training.service.impl.TrainingWorkflowServiceImpl;
import java.util.Set;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingWorkflowServiceTest {
    @Mock private TrainingRecordMapper recordMapper;
    @Mock private TrainingPlanMapper planMapper;
    @Mock private TrainingReportVersionMapper reportMapper;
    @Mock private TrainingEvaluationMapper evaluationMapper;
    @Mock private FileResourceService fileResourceService;
    @Mock private FileBusinessService fileBusinessService;
    private TrainingWorkflowService service;

    @BeforeEach
    void setUp() {
        service =
                new TrainingWorkflowServiceImpl(
                        recordMapper,
                        planMapper,
                        reportMapper,
                        evaluationMapper,
                        fileResourceService,
                        fileBusinessService);
        CurrentUser user =
                new CurrentUser(
                        8L,
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
    void learnerCannotReadAnotherLearnersWorkflowData() {
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(1L);
        record.setPlanId(2L);
        record.setUserId(7L);
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(2L);
        plan.setOwnerId(6L);
        plan.setTrainerId(5L);
        when(planMapper.selectById(2L)).thenReturn(plan);

        assertThrows(ForbiddenException.class, () -> service.evaluations(1L));
        assertThrows(ForbiddenException.class, () -> service.reports(1L));
        assertThrows(ForbiddenException.class, () -> service.proof(1L));
    }

    @Test
    void generatedCompletionProofIsBoundToTrainingRecord() {
        CurrentUser owner =
                new CurrentUser(
                        6L,
                        "trainer",
                        "Trainer",
                        null,
                        null,
                        Set.of("TRAINER"),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(owner, "n/a"));
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(1L);
        record.setPlanId(2L);
        record.setUserId(7L);
        record.setTrainingStatus("learning");
        record.setProgress(new BigDecimal("100.00"));
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        when(recordMapper.updateById(record)).thenReturn(1);
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(2L);
        plan.setOwnerId(6L);
        plan.setPlanName("Safety training");
        when(planMapper.selectById(2L)).thenReturn(plan);
        TrainingReportVersionEntity report = new TrainingReportVersionEntity();
        report.setId(12L);
        report.setTrainingRecordId(1L);
        report.setReportStatus("submitted");
        report.setVersion(0);
        when(reportMapper.selectByRecordId(1L)).thenReturn(List.of(report));
        when(reportMapper.updateById(report)).thenReturn(1);
        FileResourceVO file = new FileResourceVO();
        file.setId(99L);
        when(fileResourceService.importPrivate(any(), any(), any())).thenReturn(file);
        TrainingRecordReviewRequest request = new TrainingRecordReviewRequest();
        request.setAction("complete");

        service.review(1L, request);

        org.mockito.ArgumentCaptor<FileBusinessBindDTO> captor =
                org.mockito.ArgumentCaptor.forClass(FileBusinessBindDTO.class);
        verify(fileBusinessService).bindSystem(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getBizType())
                .isEqualTo("edu_training_record");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getBizId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getFileId()).isEqualTo(99L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getFileUsage())
                .isEqualTo("completion_proof");
        assertThat(report.getReportStatus()).isEqualTo("approved");
        assertThat(record.getTrainingStatus()).isEqualTo("completed");
    }

    @Test
    void completedTrainingCannotSubmitAnotherReport() {
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(1L);
        record.setUserId(8L);
        record.setTrainingStatus("completed");
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        TrainingReportSubmitRequest request = new TrainingReportSubmitRequest();
        request.setContent("late report");

        assertThrows(BusinessException.class, () -> service.submit(1L, request));
        verify(reportMapper, never()).insert(any(TrainingReportVersionEntity.class));
        assertThat(record.getTrainingStatus()).isEqualTo("completed");
    }

    @Test
    void resubmittingReturnedTrainingReportDoesNotResetMainStatusToLearning() {
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(1L);
        record.setUserId(8L);
        record.setTrainingStatus("makeup");
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        when(reportMapper.selectByRecordId(1L)).thenReturn(List.of());
        when(reportMapper.insert(any(TrainingReportVersionEntity.class))).thenReturn(1);
        when(recordMapper.updateById(record)).thenReturn(1);
        TrainingReportSubmitRequest request = new TrainingReportSubmitRequest();
        request.setContent("revised report");
        request.setFileId(99L);

        service.submit(1L, request);

        assertThat(record.getTrainingStatus()).isEqualTo("makeup");
        assertThat(record.getReportFileId()).isEqualTo(99L);
    }

    @Test
    void trainingCannotBeCompletedWithoutSubmittedReport() {
        CurrentUser owner =
                new CurrentUser(
                        6L, "trainer", "Trainer", null, null, Set.of("TRAINER"), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(owner, "n/a"));
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(1L);
        record.setPlanId(2L);
        record.setUserId(7L);
        record.setTrainingStatus("learning");
        record.setProgress(new BigDecimal("100.00"));
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(2L);
        plan.setOwnerId(6L);
        when(planMapper.selectById(2L)).thenReturn(plan);
        when(reportMapper.selectByRecordId(1L)).thenReturn(List.of());
        TrainingRecordReviewRequest request = new TrainingRecordReviewRequest();
        request.setAction("complete");

        assertThrows(BusinessException.class, () -> service.review(1L, request));
        verify(fileResourceService, never()).importPrivate(any(), any(), any());
    }
}
