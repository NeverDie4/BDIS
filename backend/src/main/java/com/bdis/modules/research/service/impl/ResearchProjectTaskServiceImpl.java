package com.bdis.modules.research.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.notification.service.NotificationService;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.entity.ResearchProjectTaskEntity;
import com.bdis.modules.research.entity.ResearchProjectTaskMemberEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.mapper.ResearchProjectTaskMapper;
import com.bdis.modules.research.mapper.ResearchProjectTaskMemberMapper;
import com.bdis.modules.research.request.ResearchProjectTaskCreateRequest;
import com.bdis.modules.research.request.ResearchProjectTaskMemberRequest;
import com.bdis.modules.research.service.ResearchProjectTaskService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchProjectTaskServiceImpl implements ResearchProjectTaskService {
    private final ResearchProjectMapper projectMapper;
    private final ResearchProjectTaskMapper taskMapper;
    private final ResearchProjectTaskMemberMapper memberMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationService notificationService;

    public ResearchProjectTaskServiceImpl(
            ResearchProjectMapper projectMapper,
            ResearchProjectTaskMapper taskMapper,
            ResearchProjectTaskMemberMapper memberMapper) {
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.memberMapper = memberMapper;
    }

    @Override
    @Transactional
    public Long create(Long projectId, ResearchProjectTaskCreateRequest request) {
        Long user = requiredUser();
        ResearchProjectEntity project = requireProject(projectId);
        if (!Objects.equals(user, project.getLeaderId())) {
            throw new ForbiddenException("Only the project leader can manage tasks");
        }
        if (request == null || request.getCourseIds() == null || request.getCourseIds().isEmpty()) {
            throw new BusinessException("At least one course is required");
        }
        ResearchProjectTaskEntity e = new ResearchProjectTaskEntity();
        e.setProjectId(projectId);
        e.setTaskNo(request.getTaskNo());
        e.setTaskName(request.getTaskName());
        e.setDescription(request.getDescription());
        e.setResponsibleUserId(request.getResponsibleUserId());
        e.setBaseId(request.getBaseId());
        e.setSourceRecordId(request.getSourceRecordId());
        e.setStartedAt(request.getStartedAt());
        e.setEndedAt(request.getEndedAt());
        e.setDeadlineAt(request.getDeadlineAt());
        e.setTaskStatus("pending");
        e.setStatus(1);
        e.setIsDeleted(0);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e.setCreatedBy(user);
        e.setUpdatedBy(user);
        e.setVersion(0);
        if (taskMapper.insert(e) == 0) {
            throw new BusinessException("Task creation failed");
        }
        int sortOrder = 0;
        for (Long courseId : new LinkedHashSet<>(request.getCourseIds())) {
            if (courseId == null || courseId <= 0) {
                throw new BusinessException("Course id must be positive");
            }
            if (taskMapper.insertCourseRelation(e.getId(), courseId, sortOrder++, user) == 0) {
                throw new BusinessException("Task course relation creation failed");
            }
        }
        sortOrder = 0;
        if (request.getSpeciesIds() != null) {
            for (Long speciesId : new LinkedHashSet<>(request.getSpeciesIds())) {
                if (speciesId == null || speciesId <= 0) {
                    throw new BusinessException("Species id must be positive");
                }
                if (taskMapper.insertSpeciesRelation(e.getId(), speciesId, sortOrder++, user)
                        == 0) {
                    throw new BusinessException("Task species relation creation failed");
                }
            }
        }
        return e.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchProjectTaskEntity> list(Long projectId) {
        ResearchProjectEntity project = requireProject(projectId);
        requireReadAccess(project);
        return taskMapper.selectByProjectId(projectId);
    }

    @Override
    @Transactional
    public void accept(Long taskId) {
        Long user = requiredUser();
        ResearchProjectTaskEntity e = taskMapper.selectById(taskId);
        if (e == null) {
            throw new ResourceNotFoundException("Research task not found");
        }
        if (memberMapper.exists(taskId, user) == 0
                && !projectMapper.existsActiveMember(e.getProjectId(), user)) {
            throw new ForbiddenException("Only assigned project members can accept tasks");
        }
        e.setTaskStatus("in_progress");
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy(user);
        taskMapper.updateById(e);
    }

    @Override
    @Transactional
    public void assignMember(Long taskId, ResearchProjectTaskMemberRequest request) {
        Long user = requiredUser();
        ResearchProjectTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Research task not found");
        }
        ResearchProjectEntity project = projectMapper.selectById(task.getProjectId());
        if (project == null || !Objects.equals(project.getLeaderId(), user)) {
            throw new ForbiddenException("Only the project leader can assign task members");
        }
        if (request == null
                || request.getUserId() == null
                || !projectMapper.existsActiveMember(task.getProjectId(), request.getUserId())) {
            throw new BusinessException("User must be an active project member");
        }
        if (memberMapper.exists(taskId, request.getUserId()) > 0) {
            return;
        }
        ResearchProjectTaskMemberEntity m = new ResearchProjectTaskMemberEntity();
        m.setTaskId(taskId);
        m.setUserId(request.getUserId());
        m.setMemberRole(request.getMemberRole() == null ? "executor" : request.getMemberRole());
        m.setAssignedBy(user);
        m.setAssignedAt(LocalDateTime.now());
        m.setStatus(1);
        m.setIsDeleted(0);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        m.setCreatedBy(user);
        m.setUpdatedBy(user);
        m.setVersion(0);
        memberMapper.insert(m);
        if (notificationService != null) {
            notificationService.create(
                    request.getUserId(),
                    "RESEARCH_TASK_ASSIGNED",
                    "research_project_task",
                    taskId,
                    "课题任务已分配",
                    task.getTaskName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchProjectTaskMemberEntity> members(Long taskId) {
        ResearchProjectTaskEntity task = taskMapper.selectById(taskId);
        if (task == null || Objects.equals(task.getIsDeleted(), 1)) {
            throw new ResourceNotFoundException("Research task not found");
        }
        requireReadAccess(requireProject(task.getProjectId()));
        return memberMapper.selectByTaskId(taskId);
    }

    private ResearchProjectEntity requireProject(Long projectId) {
        ResearchProjectEntity project = projectMapper.selectById(projectId);
        if (project == null
                || Objects.equals(project.getIsDeleted(), 1)
                || Objects.equals(project.getStatus(), 0)) {
            throw new ResourceNotFoundException("Research project not found");
        }
        return project;
    }

    private void requireReadAccess(ResearchProjectEntity project) {
        Long user = requiredUser();
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(r -> "ADMIN".equalsIgnoreCase(r))
                || Objects.equals(user, project.getLeaderId())
                || projectMapper.existsActiveMember(project.getId(), user)) {
            return;
        }
        throw new ForbiddenException("User is not an active project member");
    }

    private Long requiredUser() {
        Long id = CurrentUserUtils.currentUserId();
        if (id == null) {
            throw new ForbiddenException("Authentication is required");
        }
        return id;
    }
}
