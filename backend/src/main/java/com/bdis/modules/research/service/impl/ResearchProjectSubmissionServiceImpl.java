package com.bdis.modules.research.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.notification.service.NotificationService;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import com.bdis.modules.research.entity.ResearchProjectSubmissionReviewEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionMapper;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionReviewMapper;
import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.request.ResearchProjectSubmissionReviewRequest;
import com.bdis.modules.research.service.ResearchProjectSubmissionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchProjectSubmissionServiceImpl implements ResearchProjectSubmissionService {
    private static final Set<String> TYPES =
            Set.of(
                    "stage_report",
                    "data",
                    "sample",
                    "identification",
                    "statistics",
                    "paper",
                    "patent",
                    "final_result");
    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ResearchProjectSubmissionMapper submissionMapper;
    private final ResearchProjectSubmissionReviewMapper reviewMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationService notificationService;

    public ResearchProjectSubmissionServiceImpl(
            ResearchProjectMapper projectMapper,
            ProjectMemberMapper memberMapper,
            ResearchProjectSubmissionMapper submissionMapper) {
        this(projectMapper, memberMapper, submissionMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ResearchProjectSubmissionServiceImpl(
            ResearchProjectMapper projectMapper,
            ProjectMemberMapper memberMapper,
            ResearchProjectSubmissionMapper submissionMapper,
            ResearchProjectSubmissionReviewMapper reviewMapper) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.submissionMapper = submissionMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional
    public Long submit(Long projectId, ResearchProjectSubmissionCreateRequest request) {
        Long userId = requireUser();
        ResearchProjectEntity project = requireProject(projectId);
        if (!"ongoing".equals(project.getProjectStatus())
                && !"completed".equals(project.getProjectStatus())) {
            throw new BusinessException("Project is not accepting submissions");
        }
        if (!userId.equals(project.getLeaderId())
                && !projectMapper.existsActiveMember(projectId, userId)) {
            throw new ForbiddenException("Only project members can submit");
        }
        if (request == null || !TYPES.contains(request.getSubmissionType())) {
            throw new BusinessException("Invalid submission type");
        }
        if (request.getFileId() == null
                && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BusinessException("Submission content or file is required");
        }
        LocalDateTime now = LocalDateTime.now();
        ResearchProjectSubmissionEntity entity = new ResearchProjectSubmissionEntity();
        entity.setProjectId(projectId);
        entity.setTaskId(request.getTaskId());
        entity.setRecordId(request.getRecordId());
        entity.setSubmitterId(userId);
        entity.setSubmissionType(request.getSubmissionType());
        entity.setSubmissionTitle(request.getSubmissionTitle().trim());
        entity.setContent(request.getContent());
        entity.setFileId(request.getFileId());
        entity.setSubmissionVersion(1);
        entity.setSubmissionStatus("submitted");
        entity.setSubmittedAt(now);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setVersion(0);
        if (submissionMapper.insert(entity) == 0) {
            throw new BusinessException("Submission failed");
        }
        if (notificationService != null) {
            notificationService.create(
                    project.getLeaderId(),
                    "RESEARCH_SUBMISSION_SUBMITTED",
                    "research_project_submission",
                    entity.getId(),
                    "课题成果已提交",
                    entity.getSubmissionTitle());
        }
        return entity.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchProjectSubmissionEntity> list(Long projectId) {
        Long userId = requireUser();
        ResearchProjectEntity project = requireProject(projectId);
        requireReadAccess(project, userId);
        return submissionMapper.selectByProjectId(projectId);
    }

    @Override
    @Transactional
    public void review(Long submissionId, ResearchProjectSubmissionReviewRequest request) {
        Long userId = requireUser();
        ResearchProjectSubmissionEntity submission =
                submissionMapper.selectActiveById(submissionId);
        if (submission == null) {
            throw new ResourceNotFoundException("Submission not found");
        }
        ResearchProjectEntity project = requireProject(submission.getProjectId());
        if (!userId.equals(project.getLeaderId()) && !isReviewer()) {
            throw new ForbiddenException("Only project leader or reviewer can review");
        }
        if (request == null || request.getVersion() == null) {
            throw new BusinessException("Review action and version are required");
        }
        String target =
                "return".equals(request.getAction())
                        ? "returned"
                        : "approve".equals(request.getAction())
                                ? "approved"
                                : "archive".equals(request.getAction()) ? "archived" : null;
        if (target == null) {
            throw new BusinessException("Invalid review action");
        }
        String current = submission.getSubmissionStatus();
        if (("return".equals(request.getAction()) || "approve".equals(request.getAction()))
                && !Set.of("submitted", "reviewing").contains(current)) {
            throw new BusinessException(
                    "Submission cannot be "
                            + ("return".equals(request.getAction()) ? "returned" : "approved")
                            + " from status "
                            + current);
        }
        if ("archive".equals(request.getAction()) && !"approved".equals(current)) {
            throw new BusinessException("Submission cannot be archived from status " + current);
        }
        LocalDateTime reviewedAt = LocalDateTime.now();
        if (submissionMapper.reviewByIdAndVersion(
                        submissionId, request.getVersion(), target, userId, reviewedAt)
                == 0) {
            throw new BusinessException("Submission review conflict");
        }
        if (reviewMapper == null) {
            throw new BusinessException("Submission review history service is unavailable");
        }
        ResearchProjectSubmissionReviewEntity history =
                new ResearchProjectSubmissionReviewEntity();
        history.setSubmissionId(submissionId);
        history.setReviewAction(request.getAction());
        history.setReviewComment(request.getComment());
        history.setScore(request.getScore());
        history.setReviewerId(userId);
        history.setReviewedAt(reviewedAt);
        history.setVersion(0);
        if (reviewMapper.insert(history) == 0) {
            throw new BusinessException("Submission review history creation failed");
        }
        if (notificationService != null) {
            notificationService.create(
                    submission.getSubmitterId(),
                    "RESEARCH_SUBMISSION_REVIEWED",
                    "research_project_submission",
                    submissionId,
                    "课题成果审核结果",
                    request.getComment());
        }
    }

    private ResearchProjectEntity requireProject(Long id) {
        ResearchProjectEntity p = projectMapper.selectById(id);
        if (p == null || p.getIsDeleted() != null && p.getIsDeleted() == 1) {
            throw new ResourceNotFoundException("Research project not found");
        }
        return p;
    }

    private Long requireUser() {
        Long id = CurrentUserUtils.currentUserId();
        if (id == null) {
            throw new ForbiddenException("Authentication is required");
        }
        return id;
    }

    private void requireReadAccess(ResearchProjectEntity project, Long userId) {
        if (isReviewer()
                || userId.equals(project.getLeaderId())
                || projectMapper.existsActiveMember(project.getId(), userId)) {
            return;
        }
        throw new ForbiddenException("User is not an active project member");
    }

    private boolean isReviewer() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "REVIEWER".equalsIgnoreCase(r));
    }
}
