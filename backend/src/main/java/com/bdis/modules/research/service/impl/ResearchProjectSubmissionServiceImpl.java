package com.bdis.modules.research.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionMapper;
import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.request.ResearchProjectSubmissionReviewRequest;
import com.bdis.modules.research.service.ResearchProjectSubmissionService;
import com.bdis.modules.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchProjectSubmissionServiceImpl implements ResearchProjectSubmissionService {
    private static final Set<String> TYPES = Set.of("stage_report", "data", "sample", "identification", "statistics", "paper", "patent", "final_result");
    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ResearchProjectSubmissionMapper submissionMapper;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private NotificationService notificationService;

    public ResearchProjectSubmissionServiceImpl(ResearchProjectMapper projectMapper, ProjectMemberMapper memberMapper, ResearchProjectSubmissionMapper submissionMapper) {
        this.projectMapper = projectMapper; this.memberMapper = memberMapper; this.submissionMapper = submissionMapper;
    }

    @Override @Transactional
    public Long submit(Long projectId, ResearchProjectSubmissionCreateRequest request) {
        Long userId = requireUser(); ResearchProjectEntity project = requireProject(projectId);
        if (!"ongoing".equals(project.getProjectStatus()) && !"completed".equals(project.getProjectStatus())) throw new BusinessException("Project is not accepting submissions");
        if (!userId.equals(project.getLeaderId()) && !projectMapper.existsActiveMember(projectId, userId)) throw new ForbiddenException("Only project members can submit");
        if (request == null || !TYPES.contains(request.getSubmissionType())) throw new BusinessException("Invalid submission type");
        if (request.getFileId() == null && (request.getContent() == null || request.getContent().isBlank())) throw new BusinessException("Submission content or file is required");
        LocalDateTime now = LocalDateTime.now(); ResearchProjectSubmissionEntity entity = new ResearchProjectSubmissionEntity();
        entity.setProjectId(projectId); entity.setTaskId(request.getTaskId()); entity.setRecordId(request.getRecordId()); entity.setSubmitterId(userId);
        entity.setSubmissionType(request.getSubmissionType()); entity.setSubmissionTitle(request.getSubmissionTitle().trim()); entity.setContent(request.getContent()); entity.setFileId(request.getFileId());
        entity.setSubmissionVersion(1); entity.setSubmissionStatus("submitted"); entity.setSubmittedAt(now); entity.setStatus(1); entity.setIsDeleted(0); entity.setCreatedAt(now); entity.setUpdatedAt(now); entity.setCreatedBy(userId); entity.setUpdatedBy(userId); entity.setVersion(0);
        if (submissionMapper.insert(entity) == 0) throw new BusinessException("Submission failed"); if(notificationService!=null) notificationService.create(project.getLeaderId(),"RESEARCH_SUBMISSION_SUBMITTED","research_project_submission",entity.getId(),"课题成果已提交",entity.getSubmissionTitle()); return entity.getId();
    }

    @Override @Transactional
    public void review(Long submissionId, ResearchProjectSubmissionReviewRequest request) {
        Long userId = requireUser(); ResearchProjectSubmissionEntity submission = submissionMapper.selectActiveById(submissionId); if (submission == null) throw new ResourceNotFoundException("Submission not found");
        ResearchProjectEntity project = requireProject(submission.getProjectId());
        if (!userId.equals(project.getLeaderId()) && !isReviewer()) throw new ForbiddenException("Only project leader or reviewer can review");
        String target = "return".equals(request.getAction()) ? "returned" : "approve".equals(request.getAction()) ? "approved" : "archive".equals(request.getAction()) ? "archived" : null;
        if (target == null) throw new BusinessException("Invalid review action");
        if ("return".equals(request.getAction()) && !"submitted".equals(submission.getSubmissionStatus()) && !"reviewing".equals(submission.getSubmissionStatus())) throw new BusinessException("Submission cannot be returned");
        submission.setSubmissionStatus(target); submission.setReviewedAt(LocalDateTime.now()); submission.setUpdatedAt(LocalDateTime.now()); submission.setUpdatedBy(userId);
        if (submissionMapper.updateById(submission) == 0) throw new BusinessException("Submission review conflict"); if(notificationService!=null) notificationService.create(submission.getSubmitterId(),"RESEARCH_SUBMISSION_REVIEWED","research_project_submission",submissionId,"课题成果审核结果",request.getComment());
    }

    private ResearchProjectEntity requireProject(Long id) { ResearchProjectEntity p = projectMapper.selectById(id); if (p == null || p.getIsDeleted() != null && p.getIsDeleted() == 1) throw new ResourceNotFoundException("Research project not found"); return p; }
    private Long requireUser() { Long id = CurrentUserUtils.currentUserId(); if (id == null) throw new ForbiddenException("Authentication is required"); return id; }
    private boolean isReviewer() { return CurrentUserUtils.currentRoleCodes().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "REVIEWER".equalsIgnoreCase(r)); }
}
