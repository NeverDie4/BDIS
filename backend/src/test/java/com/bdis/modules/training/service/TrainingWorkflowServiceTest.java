package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingEvaluationMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.mapper.TrainingReportVersionMapper;
import com.bdis.modules.training.request.TrainingRecordReviewRequest;
import com.bdis.modules.training.service.impl.TrainingWorkflowServiceImpl;
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
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(2L);
        plan.setOwnerId(6L);
        plan.setPlanName("Safety training");
        when(planMapper.selectById(2L)).thenReturn(plan);
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
    }
}
