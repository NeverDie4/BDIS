package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import com.bdis.modules.research.entity.ResearchProjectSubmissionReviewEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionMapper;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionReviewMapper;
import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.request.ResearchProjectSubmissionReviewRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.bdis.modules.research.service.impl.ResearchProjectSubmissionServiceImpl;
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
class ResearchProjectSubmissionServiceTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private ResearchProjectSubmissionMapper submissionMapper;
    @Mock private ResearchProjectSubmissionReviewMapper reviewMapper;
    private ResearchProjectSubmissionService service;

    @BeforeEach
    void setUp() {
        service =
                new ResearchProjectSubmissionServiceImpl(
                        projectMapper, memberMapper, submissionMapper, reviewMapper);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptedMemberCanSubmitStageReport() {
        setUser(8L, "STUDENT");
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(11L);
        project.setLeaderId(3L);
        project.setProjectStatus("ongoing");
        project.setStatus(1);
        project.setIsDeleted(0);
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(projectMapper.existsActiveMember(11L, 8L)).thenReturn(true);
        when(submissionMapper.insert(any(ResearchProjectSubmissionEntity.class)))
                .thenAnswer(
                        invocation -> {
                            ((ResearchProjectSubmissionEntity) invocation.getArgument(0))
                                    .setId(41L);
                            return 1;
                        });
        ResearchProjectSubmissionCreateRequest request =
                new ResearchProjectSubmissionCreateRequest();
        request.setSubmissionType("stage_report");
        request.setSubmissionTitle("阶段报告");
        request.setContent("result");
        assertThat(service.submit(11L, request)).isEqualTo(41L);
    }

    @Test
    void nonMemberCannotListProjectSubmissions() {
        setUser(8L, "STUDENT");
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(11L);
        project.setLeaderId(3L);
        project.setStatus(1);
        project.setIsDeleted(0);
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(projectMapper.existsActiveMember(11L, 8L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(11L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void reviewPersistsDecisionScoreAndReviewerAfterOptimisticUpdate() {
        setUser(3L, "TEACHER");
        ResearchProjectSubmissionEntity submission = submission("submitted", 2);
        when(submissionMapper.selectActiveById(41L)).thenReturn(submission);
        when(projectMapper.selectById(11L)).thenReturn(project(3L));
        when(submissionMapper.reviewByIdAndVersion(
                        eq(41L), eq(2), eq("approved"), eq(3L), any(LocalDateTime.class)))
                .thenReturn(1);
        when(reviewMapper.insert(any(ResearchProjectSubmissionReviewEntity.class))).thenReturn(1);
        ResearchProjectSubmissionReviewRequest request = new ResearchProjectSubmissionReviewRequest();
        request.setAction("approve");
        request.setComment("meets stage criteria");
        request.setScore(new BigDecimal("92.50"));
        request.setVersion(2);

        service.review(41L, request);

        org.mockito.ArgumentCaptor<ResearchProjectSubmissionReviewEntity> captor =
                org.mockito.ArgumentCaptor.forClass(ResearchProjectSubmissionReviewEntity.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getSubmissionId()).isEqualTo(41L);
        assertThat(captor.getValue().getReviewAction()).isEqualTo("approve");
        assertThat(captor.getValue().getReviewComment()).isEqualTo("meets stage criteria");
        assertThat(captor.getValue().getScore()).isEqualByComparingTo("92.50");
        assertThat(captor.getValue().getReviewerId()).isEqualTo(3L);
    }

    @Test
    void archivedSubmissionCannotBeApprovedAgain() {
        setUser(3L, "TEACHER");
        when(submissionMapper.selectActiveById(41L)).thenReturn(submission("archived", 3));
        when(projectMapper.selectById(11L)).thenReturn(project(3L));
        ResearchProjectSubmissionReviewRequest request = reviewRequest("approve", 3);

        assertThatThrownBy(() -> service.review(41L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be approved");
        verify(submissionMapper, never())
                .reviewByIdAndVersion(any(), any(), any(), any(), any());
    }

    @Test
    void returnedSubmissionCannotBeArchived() {
        setUser(3L, "TEACHER");
        when(submissionMapper.selectActiveById(41L)).thenReturn(submission("returned", 3));
        when(projectMapper.selectById(11L)).thenReturn(project(3L));

        assertThatThrownBy(() -> service.review(41L, reviewRequest("archive", 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be archived");
    }

    private ResearchProjectSubmissionEntity submission(String status, int version) {
        ResearchProjectSubmissionEntity submission = new ResearchProjectSubmissionEntity();
        submission.setId(41L);
        submission.setProjectId(11L);
        submission.setSubmitterId(8L);
        submission.setSubmissionStatus(status);
        submission.setVersion(version);
        submission.setIsDeleted(0);
        return submission;
    }

    private ResearchProjectEntity project(Long leaderId) {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(11L);
        project.setLeaderId(leaderId);
        project.setStatus(1);
        project.setIsDeleted(0);
        return project;
    }

    private ResearchProjectSubmissionReviewRequest reviewRequest(String action, int version) {
        ResearchProjectSubmissionReviewRequest request = new ResearchProjectSubmissionReviewRequest();
        request.setAction(action);
        request.setVersion(version);
        return request;
    }

    private void setUser(Long id, String role) {
        CurrentUser user =
                new CurrentUser(
                        id, "user-" + id, "User", null, null, Set.of(role), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }
}
