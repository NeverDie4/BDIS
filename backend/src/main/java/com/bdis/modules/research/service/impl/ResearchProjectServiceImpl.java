package com.bdis.modules.research.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.research.constant.ResearchProjectStatus;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.query.ResearchProjectQuery;
import com.bdis.modules.research.request.ResearchProjectCreateRequest;
import com.bdis.modules.research.request.ResearchProjectLeaderChangeRequest;
import com.bdis.modules.research.request.ResearchProjectStatusChangeRequest;
import com.bdis.modules.research.request.ResearchProjectUpdateRequest;
import com.bdis.modules.research.service.ProjectMaterialService;
import com.bdis.modules.research.service.ProjectMemberService;
import com.bdis.modules.research.service.ResearchAchievementService;
import com.bdis.modules.research.service.ResearchProjectService;
import com.bdis.modules.research.vo.ResearchProjectDetailVO;
import com.bdis.modules.research.vo.ResearchProjectListVO;
import com.bdis.modules.research.vo.ResearchUserCandidateVO;
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
public class ResearchProjectServiceImpl implements ResearchProjectService {

    private static final String BIZ_TYPE = "research_project";
    private static final String AUDIT_MODULE = "M13_RESEARCH_PROJECT";
    private static final String PLANNING = "planning";
    private static final Set<String> PROJECT_TYPES = Set.of("teaching", "research", "cooperation");

    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final HerbSpeciesMapper herbSpeciesMapper;
    private final ProjectMemberService memberService;
    private final ProjectMaterialService materialService;
    private final ResearchAchievementService achievementService;
    private final AuditLogService auditLogService;

    public ResearchProjectServiceImpl(
            ResearchProjectMapper projectMapper,
            ProjectMemberMapper memberMapper,
            UserMapper userMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            ProjectMemberService memberService,
            ProjectMaterialService materialService,
            AuditLogService auditLogService) {
        this(
                projectMapper,
                memberMapper,
                userMapper,
                herbSpeciesMapper,
                memberService,
                materialService,
                null,
                auditLogService);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ResearchProjectServiceImpl(
            ResearchProjectMapper projectMapper,
            ProjectMemberMapper memberMapper,
            UserMapper userMapper,
            HerbSpeciesMapper herbSpeciesMapper,
            ProjectMemberService memberService,
            ProjectMaterialService materialService,
            ResearchAchievementService achievementService,
            AuditLogService auditLogService) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.memberService = memberService;
        this.materialService = materialService;
        this.achievementService = achievementService;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResult<ResearchProjectListVO> page(ResearchProjectQuery query) {
        ResearchProjectQuery safe = query == null ? new ResearchProjectQuery() : query;
        if (safe.getPageNo() == null || safe.getPageNo() < 1) {
            safe.setPageNo(1);
        }
        if (safe.getPageSize() == null || safe.getPageSize() < 1) {
            safe.setPageSize(10);
        }
        Page<ResearchProjectEntity> page =
                projectMapper.selectPage(
                        Page.of(safe.getPageNo(), safe.getPageSize()), buildWrapper(safe));

        Map<Long, UserEntity> users = new HashMap<>();
        Map<Long, HerbEntity> herbs = new HashMap<>();
        List<Long> userIds =
                page.getRecords().stream()
                        .map(ResearchProjectEntity::getLeaderId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        List<Long> herbIds =
                page.getRecords().stream()
                        .map(ResearchProjectEntity::getSpeciesId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(user -> users.put(user.getId(), user));
        }
        if (!herbIds.isEmpty()) {
            herbSpeciesMapper
                    .selectBatchIds(herbIds)
                    .forEach(herb -> herbs.put(herb.getId(), herb));
        }
        List<ResearchProjectListVO> records =
                page.getRecords().stream()
                        .map(
                                entity ->
                                        toListVO(
                                                entity,
                                                users.get(entity.getLeaderId()),
                                                herbs.get(entity.getSpeciesId())))
                        .toList();
        return PageResult.of(records, page);
    }

    @Override
    public List<ResearchUserCandidateVO> listUserCandidates() {
        Map<Long, String> leaderRoles =
                userMapper.selectResearchLeaderUserIds().stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        id -> id,
                                        id -> {
                                            String role = userMapper.selectResearchLeaderRole(id);
                                            return role == null ? null : role.toLowerCase();
                                        },
                                        (left, right) -> left));
        return userMapper.selectList(
                        new LambdaQueryWrapper<UserEntity>()
                                .eq(UserEntity::getStatus, 1)
                                .eq(UserEntity::getIsDeleted, 0)
                                .orderByAsc(UserEntity::getRealName)
                                .orderByAsc(UserEntity::getUsername))
                .stream()
                .map(
                        user -> {
                            ResearchUserCandidateVO vo = new ResearchUserCandidateVO();
                            vo.setId(user.getId());
                            vo.setUsername(user.getUsername());
                            vo.setRealName(user.getRealName());
                            String role = leaderRoles.get(user.getId());
                            vo.setUserType(
                                    StringUtils.hasText(user.getUserType())
                                            ? user.getUserType().toLowerCase()
                                            : role);
                            vo.setStatus(user.getStatus());
                            return vo;
                        })
                .toList();
    }

    @Override
    public ResearchProjectDetailVO getDetail(Long id) {
        ResearchProjectEntity entity = requireActive(id);
        requireProjectAccess(entity, false);
        UserEntity leader =
                entity.getLeaderId() == null ? null : userMapper.selectById(entity.getLeaderId());
        HerbEntity species =
                entity.getSpeciesId() == null
                        ? null
                        : herbSpeciesMapper.selectById(entity.getSpeciesId());
        ResearchProjectDetailVO vo = new ResearchProjectDetailVO();
        copy(entity, vo, leader, species);
        vo.setMembers(memberService.list(id, null));
        vo.setMaterials(materialService.list(id, null));
        if (achievementService != null) {
            vo.setAchievements(achievementService.listByProjectId(id));
            vo.setAchievementSummary(achievementService.summarizeByProjectId(id));
        }
        return vo;
    }

    @Override
    @Transactional
    public Long create(ResearchProjectCreateRequest request) {
        validateCreate(request);
        validateProjectType(request.getProjectType());
        validateTimeRange(request.getStartedAt(), request.getEndedAt());
        ensureProjectNoAvailable(request.getProjectNo());
        UserEntity leader = requireLeader(request.getLeaderId());
        validateSpecies(request.getSpeciesId());

        LocalDateTime now = LocalDateTime.now();
        ResearchProjectEntity entity = new ResearchProjectEntity();
        entity.setProjectNo(request.getProjectNo().trim());
        entity.setProjectName(request.getProjectName().trim());
        entity.setProjectType(request.getProjectType().trim());
        entity.setLeaderId(leader.getId());
        entity.setSpeciesId(request.getSpeciesId());
        entity.setDescription(request.getDescription());
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setProjectStatus(PLANNING);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        entity.setRemark(request.getRemark());
        entity.setVersion(0);
        projectMapper.insert(entity);

        ProjectMemberEntity relation = new ProjectMemberEntity();
        relation.setProjectId(entity.getId());
        relation.setUserId(leader.getId());
        relation.setMemberRole("leader");
        relation.setMemberStatus("active");
        relation.setJoinedAt(now);
        relation.setCreatedAt(now);
        relation.setCreatedBy(CurrentUserUtils.currentUserId());
        relation.setRemark("Project leader");
        if (memberMapper.insert(relation) == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Failed to create leader membership");
        }
        recordAudit("CREATE", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ResearchProjectUpdateRequest request) {
        ResearchProjectEntity entity = requireActive(id);
        requireProjectAccess(entity, true);
        ResearchProjectStatus.assertMutable(entity);
        if (request == null) {
            throw new BusinessException("Project update request is required");
        }
        if (request.getVersion() == null
                || !Objects.equals(request.getVersion(), entity.getVersion())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project version conflict");
        }
        if (!StringUtils.hasText(request.getProjectName())
                || !StringUtils.hasText(request.getProjectType())) {
            throw new BusinessException("Project name and type are required");
        }
        validateProjectType(request.getProjectType());
        validateTimeRange(request.getStartedAt(), request.getEndedAt());
        validateSpecies(request.getSpeciesId());
        entity.setProjectName(request.getProjectName().trim());
        entity.setProjectType(request.getProjectType().trim());
        entity.setSpeciesId(request.getSpeciesId());
        entity.setDescription(request.getDescription());
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setRemark(request.getRemark());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (projectMapper.updateById(entity) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project update failed");
        }
        recordAudit("UPDATE", id);
    }

    @Override
    @Transactional
    public void changeLeader(Long id, ResearchProjectLeaderChangeRequest request) {
        ResearchProjectEntity project = requireActive(id);
        requireProjectAccess(project, true);
        ResearchProjectStatus.assertMutable(project);
        validateLeaderChangeRequest(request, project);
        UserEntity newLeader = requireLeader(request.getNewLeaderId());
        if (Objects.equals(project.getLeaderId(), newLeader.getId())) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "New leader must differ from current leader");
        }
        ProjectMemberEntity oldLeader =
                memberMapper.selectByProjectIdAndUserId(id, project.getLeaderId());
        if (oldLeader == null
                || !"leader".equals(oldLeader.getMemberRole())
                || !"active".equals(oldLeader.getMemberStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Current project leader membership is invalid");
        }
        ProjectMemberEntity newLeaderRelation =
                memberMapper.selectByProjectIdAndUserId(id, newLeader.getId());
        LocalDateTime now = LocalDateTime.now();
        project.setLeaderId(newLeader.getId());
        project.setUpdatedAt(now);
        project.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (projectMapper.updateById(project) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project leader change conflict");
        }
        oldLeader.setMemberRole(resolveOldLeaderRole(request.getOldLeaderRole()));
        if (memberMapper.updateById(oldLeader) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Previous leader update failed");
        }
        if (newLeaderRelation == null) {
            newLeaderRelation = new ProjectMemberEntity();
            newLeaderRelation.setProjectId(id);
            newLeaderRelation.setUserId(newLeader.getId());
            newLeaderRelation.setMemberRole("leader");
            newLeaderRelation.setMemberStatus("active");
            newLeaderRelation.setJoinedAt(now);
            newLeaderRelation.setCreatedAt(now);
            newLeaderRelation.setCreatedBy(CurrentUserUtils.currentUserId());
            newLeaderRelation.setRemark("Project leader");
            if (memberMapper.insert(newLeaderRelation) == 0) {
                throw new BusinessException(
                        ResultCodeEnum.CONFLICT, "New leader membership create failed");
            }
        } else {
            newLeaderRelation.setMemberRole("leader");
            newLeaderRelation.setMemberStatus("active");
            newLeaderRelation.setJoinedAt(now);
            newLeaderRelation.setLeftAt(null);
            if (memberMapper.updateById(newLeaderRelation) == 0) {
                throw new BusinessException(
                        ResultCodeEnum.CONFLICT, "New leader membership update failed");
            }
        }
        recordAudit("CHANGE_LEADER", id);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, ResearchProjectStatusChangeRequest request) {
        ResearchProjectEntity project = requireActive(id);
        requireProjectAccess(project, true);
        if (request == null
                || !StringUtils.hasText(request.getTargetStatus())
                || request.getVersion() == null) {
            throw new BusinessException("Target status and version are required");
        }
        if (!Objects.equals(request.getVersion(), project.getVersion())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project version conflict");
        }
        String target = request.getTargetStatus().trim();
        ResearchProjectStatus.validateTransition(project.getProjectStatus(), target);
        if ((ResearchProjectStatus.SUSPENDED.equals(target)
                        || ResearchProjectStatus.COMPLETED.equals(target)
                        || ResearchProjectStatus.ONGOING.equals(target)
                                && ResearchProjectStatus.SUSPENDED.equals(
                                        project.getProjectStatus()))
                && !StringUtils.hasText(request.getReason())) {
            throw new BusinessException("Status change reason is required");
        }
        if (ResearchProjectStatus.PLANNING.equals(project.getProjectStatus())) {
            validateBeforeStart(project);
        } else if (ResearchProjectStatus.SUSPENDED.equals(project.getProjectStatus())) {
            validateLeaderRelation(project);
        } else if (ResearchProjectStatus.COMPLETED.equals(target)) {
            validateBeforeComplete(project);
        }
        project.setProjectStatus(target);
        project.setUpdatedAt(LocalDateTime.now());
        project.setUpdatedBy(CurrentUserUtils.currentUserId());
        if (projectMapper.updateById(project) == 0) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project status change conflict");
        }
        recordAudit("CHANGE_STATUS", id);
    }

    private void validateLeaderChangeRequest(
            ResearchProjectLeaderChangeRequest request, ResearchProjectEntity project) {
        if (request == null
                || request.getNewLeaderId() == null
                || !StringUtils.hasText(request.getReason())
                || request.getVersion() == null) {
            throw new BusinessException("New leader, reason and version are required");
        }
        if (!Objects.equals(request.getVersion(), project.getVersion())) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project version conflict");
        }
        resolveOldLeaderRole(request.getOldLeaderRole());
    }

    private String resolveOldLeaderRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "researcher";
        }
        if (!Set.of("researcher", "assistant").contains(role)) {
            throw new BusinessException("Old leader role must be researcher or assistant");
        }
        return role;
    }

    private void validateBeforeStart(ResearchProjectEntity project) {
        if (!StringUtils.hasText(project.getProjectName())) {
            throw new BusinessException("Project name is required before start");
        }
        validateTimeRange(project.getStartedAt(), project.getEndedAt());
        requireLeader(project.getLeaderId());
        validateLeaderRelation(project);
        Long activeMemberCount =
                memberMapper.selectCount(
                        new LambdaQueryWrapper<ProjectMemberEntity>()
                                .eq(ProjectMemberEntity::getProjectId, project.getId())
                                .eq(ProjectMemberEntity::getMemberStatus, "active"));
        if (activeMemberCount == null || activeMemberCount == 0) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Project requires at least one active member before start");
        }
        validateSpecies(project.getSpeciesId());
    }

    private void validateLeaderRelation(ResearchProjectEntity project) {
        requireLeader(project.getLeaderId());
        ProjectMemberEntity relation =
                memberMapper.selectByProjectIdAndUserId(project.getId(), project.getLeaderId());
        if (relation == null
                || !"leader".equals(relation.getMemberRole())
                || !"active".equals(relation.getMemberStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Project active leader membership is required");
        }
    }

    private void validateBeforeComplete(ResearchProjectEntity project) {
        validateLeaderRelation(project);
    }

    private ResearchProjectEntity requireActive(Long id) {
        ResearchProjectEntity entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Research project not found");
        }
        return entity;
    }

    private void validateCreate(ResearchProjectCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getProjectNo())
                || !StringUtils.hasText(request.getProjectName())
                || !StringUtils.hasText(request.getProjectType())
                || request.getLeaderId() == null) {
            throw new BusinessException("Project number, name, type and leader are required");
        }
    }

    private void ensureProjectNoAvailable(String projectNo) {
        if (projectMapper.selectByProjectNoIncludingDeleted(projectNo.trim()) != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Project number already exists");
        }
    }

    private UserEntity requireLeader(Long leaderId) {
        UserEntity user = userMapper.selectById(leaderId);
        if (user == null
                || !Objects.equals(user.getStatus(), 1)
                || (!Set.of("teacher", "researcher")
                                .contains(
                                        user.getUserType() == null
                                                ? ""
                                                : user.getUserType().toLowerCase())
                        && userMapper.selectResearchLeaderRole(leaderId) == null)) {
            throw new BusinessException(
                    ResultCodeEnum.VALIDATION_ERROR,
                    "Leader must be an enabled teacher or researcher");
        }
        return user;
    }

    private void validateSpecies(Long speciesId) {
        if (speciesId == null) {
            return;
        }
        HerbEntity herb = herbSpeciesMapper.selectActiveById(speciesId);
        if (herb == null
                || !Objects.equals(herb.getStatus(), 1)
                || !Objects.equals(herb.getIsDeleted(), 0)) {
            throw new ResourceNotFoundException("Herb species not found or inactive");
        }
    }

    private void validateProjectType(String type) {
        if (!PROJECT_TYPES.contains(type.trim())) {
            throw new BusinessException("Invalid project type");
        }
    }

    private void validateTimeRange(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt != null && endedAt != null && startedAt.isAfter(endedAt)) {
            throw new BusinessException("Start time must be before end time");
        }
    }

    private LambdaQueryWrapper<ResearchProjectEntity> buildWrapper(ResearchProjectQuery query) {
        LambdaQueryWrapper<ResearchProjectEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(
                    w ->
                            w.like(ResearchProjectEntity::getProjectNo, query.getKeyword())
                                    .or()
                                    .like(
                                            ResearchProjectEntity::getProjectName,
                                            query.getKeyword()));
        }
        wrapper.eq(
                StringUtils.hasText(query.getProjectType()),
                ResearchProjectEntity::getProjectType,
                query.getProjectType());
        wrapper.eq(
                query.getLeaderId() != null,
                ResearchProjectEntity::getLeaderId,
                query.getLeaderId());
        wrapper.eq(
                query.getSpeciesId() != null,
                ResearchProjectEntity::getSpeciesId,
                query.getSpeciesId());
        wrapper.eq(
                StringUtils.hasText(query.getProjectStatus()),
                ResearchProjectEntity::getProjectStatus,
                query.getProjectStatus());
        wrapper.ge(
                query.getStartedFrom() != null,
                ResearchProjectEntity::getStartedAt,
                query.getStartedFrom());
        wrapper.le(
                query.getStartedTo() != null,
                ResearchProjectEntity::getStartedAt,
                query.getStartedTo());
        applyUserScope(wrapper);
        return wrapper.orderByDesc(ResearchProjectEntity::getCreatedAt)
                .orderByDesc(ResearchProjectEntity::getId);
    }

    private void applyUserScope(LambdaQueryWrapper<ResearchProjectEntity> wrapper) {
        if (!hasScopedIdentity() || isAdmin()) {
            return;
        }
        Long userId = CurrentUserUtils.currentUserId();
        wrapper.apply(
                "(leader_id = {0} OR EXISTS (SELECT 1 FROM rel_project_member rpm "
                        + "WHERE rpm.project_id = research_project.id AND rpm.user_id = {0} "
                        + "AND rpm.member_status = 'active'))",
                userId);
    }

    private void requireProjectAccess(ResearchProjectEntity project, boolean manage) {
        if (!hasScopedIdentity() || isAdmin()) {
            return;
        }
        Long userId = CurrentUserUtils.currentUserId();
        if (Objects.equals(userId, project.getLeaderId())) {
            return;
        }
        ProjectMemberEntity member =
                memberMapper.selectByProjectIdAndUserId(project.getId(), userId);
        if (!manage && member != null && "active".equals(member.getMemberStatus())) {
            return;
        }
        throw new ForbiddenException(
                manage
                        ? "Only the project leader can manage this project"
                        : "User is not an active project member");
    }

    private boolean hasScopedIdentity() {
        return !CurrentUserUtils.currentRoleCodes().isEmpty();
    }

    private boolean isAdmin() {
        return CurrentUserUtils.currentRoleCodes().stream()
                .anyMatch(role -> SecurityConstants.ADMIN_ROLE_CODE.equalsIgnoreCase(role));
    }

    private ResearchProjectListVO toListVO(
            ResearchProjectEntity entity, UserEntity leader, HerbEntity species) {
        ResearchProjectListVO vo = new ResearchProjectListVO();
        vo.setId(entity.getId());
        vo.setProjectNo(entity.getProjectNo());
        vo.setProjectName(entity.getProjectName());
        vo.setProjectType(entity.getProjectType());
        vo.setLeaderId(entity.getLeaderId());
        vo.setLeaderName(leader == null ? null : leader.getRealName());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(species == null ? null : species.getHerbName());
        vo.setProjectStatus(entity.getProjectStatus());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private void copy(
            ResearchProjectEntity entity,
            ResearchProjectDetailVO vo,
            UserEntity leader,
            HerbEntity species) {
        vo.setId(entity.getId());
        vo.setProjectNo(entity.getProjectNo());
        vo.setProjectName(entity.getProjectName());
        vo.setProjectType(entity.getProjectType());
        vo.setLeaderId(entity.getLeaderId());
        vo.setLeaderName(leader == null ? null : leader.getRealName());
        vo.setSpeciesId(entity.getSpeciesId());
        vo.setSpeciesName(species == null ? null : species.getHerbName());
        vo.setDescription(entity.getDescription());
        vo.setProjectStatus(entity.getProjectStatus());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setVersion(entity.getVersion());
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
