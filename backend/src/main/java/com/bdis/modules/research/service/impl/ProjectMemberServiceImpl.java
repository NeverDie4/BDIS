package com.bdis.modules.research.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.research.constant.ResearchProjectStatus;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ProjectMemberAddRequest;
import com.bdis.modules.research.request.ProjectMemberUpdateRequest;
import com.bdis.modules.research.service.ProjectMemberService;
import com.bdis.modules.research.vo.ProjectMemberVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private static final String BIZ_TYPE = "research_project";
    private static final String AUDIT_MODULE = "M13_PROJECT_MEMBER";
    private static final Set<String> ROLES = Set.of("leader", "researcher", "assistant", "student");

    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public ProjectMemberServiceImpl(
            ResearchProjectMapper projectMapper,
            ProjectMemberMapper memberMapper,
            UserMapper userMapper,
            AuditLogService auditLogService) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ProjectMemberVO> list(Long projectId, String memberStatus) {
        requireProject(projectId);
        LambdaQueryWrapper<ProjectMemberEntity> wrapper =
                new LambdaQueryWrapper<ProjectMemberEntity>()
                        .eq(ProjectMemberEntity::getProjectId, projectId);
        if (StringUtils.hasText(memberStatus)) {
            wrapper.eq(ProjectMemberEntity::getMemberStatus, memberStatus);
        }
        List<ProjectMemberEntity> members = memberMapper.selectList(wrapper);
        Map<Long, UserEntity> users = new HashMap<>();
        List<Long> ids =
                members.stream()
                        .map(ProjectMemberEntity::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (!ids.isEmpty()) {
            userMapper.selectBatchIds(ids).forEach(user -> users.put(user.getId(), user));
        }
        return members.stream().map(member -> toVO(member, users.get(member.getUserId()))).toList();
    }

    @Override
    public ProjectMemberVO get(Long projectId, Long userId) {
        requireProject(projectId);
        ProjectMemberEntity member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null) {
            throw new ResourceNotFoundException("Project member not found");
        }
        return toVO(member, userMapper.selectById(userId));
    }

    @Override
    @Transactional
    public Long add(Long projectId, ProjectMemberAddRequest request) {
        ResearchProjectEntity project = requireProject(projectId);
        requireProjectAccess(project, true);
        ResearchProjectStatus.assertMutable(project);
        validateRole(request == null ? null : request.getMemberRole());
        if ("leader".equals(request.getMemberRole())) {
            throw new BusinessException("A second leader cannot be added");
        }
        UserEntity user = requireUser(request.getUserId());
        ProjectMemberEntity existing =
                memberMapper.selectByProjectIdAndUserId(projectId, user.getId());
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if ("active".equals(existing.getMemberStatus())) {
                throw new BusinessException("Member is already active");
            }
            existing.setMemberRole(request.getMemberRole());
            existing.setMemberStatus("active");
            existing.setJoinedAt(now);
            existing.setLeftAt(null);
            existing.setRemark(request.getRemark());
            if (memberMapper.updateById(existing) == 0) {
                throw new BusinessException("Member restore failed");
            }
            recordAudit("RESTORE", existing.getId());
            return existing.getId();
        }
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(projectId);
        member.setUserId(user.getId());
        member.setMemberRole(request.getMemberRole());
        member.setMemberStatus("active");
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setCreatedBy(CurrentUserUtils.currentUserId());
        member.setRemark(request.getRemark());
        if (memberMapper.insert(member) == 0) {
            throw new BusinessException("Member add failed");
        }
        recordAudit("ADD", member.getId());
        return member.getId();
    }

    @Override
    @Transactional
    public void updateRole(Long projectId, Long userId, ProjectMemberUpdateRequest request) {
        ResearchProjectEntity project = requireProject(projectId);
        requireProjectAccess(project, true);
        ResearchProjectStatus.assertMutable(project);
        validateRole(request == null ? null : request.getMemberRole());
        if ("leader".equals(request.getMemberRole())) {
            throw new BusinessException("Ordinary member cannot become leader");
        }
        ProjectMemberEntity member = requireMember(projectId, userId);
        if ("leader".equals(member.getMemberRole())) {
            throw new BusinessException("Project leader role cannot be changed");
        }
        if (!"active".equals(member.getMemberStatus())) {
            throw new BusinessException("Member is not active");
        }
        member.setMemberRole(request.getMemberRole());
        member.setRemark(request.getRemark());
        if (memberMapper.updateById(member) == 0) {
            throw new BusinessException("Member role update failed");
        }
        recordAudit("UPDATE_ROLE", member.getId());
    }

    @Override
    @Transactional
    public void remove(Long projectId, Long userId) {
        ResearchProjectEntity project = requireProject(projectId);
        requireProjectAccess(project, true);
        ResearchProjectStatus.assertMutable(project);
        ProjectMemberEntity member = requireMember(projectId, userId);
        if ("leader".equals(member.getMemberRole())) {
            throw new BusinessException("Project leader cannot leave");
        }
        if (!"active".equals(member.getMemberStatus())) {
            throw new BusinessException("Member has already left");
        }
        member.setMemberStatus("left");
        member.setLeftAt(LocalDateTime.now());
        if (memberMapper.updateById(member) == 0) {
            throw new BusinessException("Member leave failed");
        }
        recordAudit("REMOVE", member.getId());
    }

    @Override
    @Transactional
    public Long invite(Long projectId, ProjectMemberAddRequest request) {
        ResearchProjectEntity project = requireProject(projectId);
        requireProjectAccess(project, true);
        ResearchProjectStatus.assertMutable(project);
        validateRole(request == null ? null : request.getMemberRole());
        UserEntity user = requireUser(request.getUserId());
        ProjectMemberEntity member =
                memberMapper.selectByProjectIdAndUserId(projectId, user.getId());
        if (member != null && "active".equals(member.getMemberStatus())) {
            throw new BusinessException("Member is already active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (member == null) {
            member = new ProjectMemberEntity();
            member.setProjectId(projectId);
            member.setUserId(user.getId());
            member.setCreatedAt(now);
            member.setCreatedBy(CurrentUserUtils.currentUserId());
        }
        member.setMemberRole(request.getMemberRole());
        member.setMemberStatus("invited");
        member.setInvitationStatus("pending");
        member.setInvitedBy(CurrentUserUtils.currentUserId());
        member.setInvitedAt(now);
        member.setAcceptedAt(null);
        member.setRejectedAt(null);
        member.setRemark(request.getRemark());
        if (member.getId() == null) {
            memberMapper.insert(member);
        } else {
            memberMapper.updateById(member);
        }
        return member.getId();
    }

    @Override
    @Transactional
    public void respond(Long projectId, String response) {
        Long userId = CurrentUserUtils.currentUserId();
        if (userId == null) {
            throw new ForbiddenException("Authentication is required");
        }
        if (!"accept".equals(response) && !"reject".equals(response)) {
            throw new BusinessException("Invalid invitation response");
        }
        ProjectMemberEntity member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null || !"pending".equals(member.getInvitationStatus())) {
            throw new ResourceNotFoundException("Pending invitation not found");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean accepted = "accept".equals(response);
        member.setInvitationStatus(accepted ? "accepted" : "rejected");
        member.setMemberStatus(accepted ? "active" : "left");
        member.setJoinedAt(accepted ? now : null);
        member.setAcceptedAt(accepted ? now : null);
        member.setRejectedAt(accepted ? null : now);
        if (memberMapper.updateById(member) == 0) {
            throw new BusinessException("Invitation response failed");
        }
    }

    private ResearchProjectEntity requireProject(Long projectId) {
        ResearchProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("Research project not found");
        }
        requireProjectAccess(project, false);
        return project;
    }

    private void requireProjectAccess(ResearchProjectEntity project, boolean manage) {
        if (CurrentUserUtils.currentRoleCodes().isEmpty()
                || CurrentUserUtils.currentRoleCodes().stream()
                        .anyMatch(
                                role -> SecurityConstants.ADMIN_ROLE_CODE.equalsIgnoreCase(role))) {
            return;
        }
        Long userId = CurrentUserUtils.currentUserId();
        if (Objects.equals(userId, project.getLeaderId())) {
            return;
        }
        if (!manage && projectMapper.existsActiveMember(project.getId(), userId)) {
            return;
        }
        throw new ForbiddenException(
                manage
                        ? "Only the project leader can manage project members"
                        : "User is not an active project member");
    }

    private ProjectMemberEntity requireMember(Long projectId, Long userId) {
        ProjectMemberEntity member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null) {
            throw new ResourceNotFoundException("Project member not found");
        }
        return member;
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null
                || !Objects.equals(user.getStatus(), 1)
                || !Objects.equals(user.getIsDeleted(), 0)) {
            throw new ResourceNotFoundException("User not found or inactive");
        }
        return user;
    }

    private void validateRole(String role) {
        if (!StringUtils.hasText(role) || !ROLES.contains(role)) {
            throw new BusinessException("Invalid member role");
        }
    }

    private ProjectMemberVO toVO(ProjectMemberEntity member, UserEntity user) {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(member.getId());
        vo.setProjectId(member.getProjectId());
        vo.setUserId(member.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
        }
        vo.setMemberRole(member.getMemberRole());
        vo.setMemberStatus(member.getMemberStatus());
        vo.setJoinedAt(member.getJoinedAt());
        vo.setLeftAt(member.getLeftAt());
        vo.setRemark(member.getRemark());
        vo.setCreatedAt(member.getCreatedAt());
        return vo;
    }

    private void recordAudit(String operation, Long id) {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(AUDIT_MODULE);
        dto.setOperationType(operation);
        dto.setBizType(BIZ_TYPE);
        dto.setBizId(id);
        auditLogService.record(dto);
    }
}
