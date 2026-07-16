package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ResearchProjectCreateRequest;
import com.bdis.modules.research.request.ResearchProjectStatusChangeRequest;
import com.bdis.modules.research.request.ResearchProjectUpdateRequest;
import com.bdis.modules.research.service.impl.ResearchProjectServiceImpl;
import com.bdis.modules.research.vo.ResearchProjectListVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ResearchProjectServiceTest {

    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private UserMapper userMapper;
    @Mock private HerbSpeciesMapper herbSpeciesMapper;
    @Mock private ProjectMemberService memberService;
    @Mock private ProjectMaterialService materialService;
    @Mock private AuditLogService auditLogService;

    private ResearchProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ResearchProjectServiceImpl(
                        projectMapper,
                        memberMapper,
                        userMapper,
                        herbSpeciesMapper,
                        memberService,
                        materialService,
                        auditLogService);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonMemberCannotReadProjectButActiveMemberCan() {
        ResearchProjectEntity existing = project(100L, "P-001");
        existing.setLeaderId(7L);
        when(projectMapper.selectById(100L)).thenReturn(existing);
        setUser(8L, "TEACHER");
        when(memberMapper.selectByProjectIdAndUserId(100L, 8L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail(100L)).isInstanceOf(ForbiddenException.class);

        ProjectMemberEntity active = new ProjectMemberEntity();
        active.setProjectId(100L);
        active.setUserId(8L);
        active.setMemberStatus("active");
        when(memberMapper.selectByProjectIdAndUserId(100L, 8L)).thenReturn(active);
        assertThat(service.getDetail(100L)).isNotNull();

        ResearchProjectUpdateRequest update = new ResearchProjectUpdateRequest();
        update.setProjectName("Changed");
        update.setProjectType("research");
        update.setVersion(0);
        assertThatThrownBy(() -> service.update(100L, update))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void nonMemberCannotReadProjectReviewHistory() {
        ResearchProjectEntity existing = project(100L, "P-001");
        existing.setLeaderId(7L);
        when(projectMapper.selectById(100L)).thenReturn(existing);
        setUser(8L, "STUDENT");
        when(memberMapper.selectByProjectIdAndUserId(100L, 8L)).thenReturn(null);

        assertThatThrownBy(() -> service.reviewHistory(100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void projectListBuildsLeaderOrActiveMemberScope() {
        setUser(8L, "TEACHER");
        when(projectMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.page(new com.bdis.modules.research.query.ResearchProjectQuery());

        ArgumentCaptor<LambdaQueryWrapper<ResearchProjectEntity>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(projectMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    private void setUser(Long id, String role) {
        CurrentUser user =
                new CurrentUser(
                        id,
                        "user-" + id,
                        "User",
                        null,
                        null,
                        Set.of(role),
                        Set.of(),
                        Set.of("research:project:detail"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    @Test
    void createProjectForcesPlanningAndAddsLeader() {
        UserEntity leader = user(7L, "teacher");
        when(projectMapper.selectByProjectNoIncludingDeleted("P-001")).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(leader);
        when(projectMapper.insert(any(ResearchProjectEntity.class)))
                .thenAnswer(
                        invocation -> {
                            invocation.getArgument(0, ResearchProjectEntity.class).setId(100L);
                            return 1;
                        });
        when(memberMapper.insert(any(ProjectMemberEntity.class))).thenReturn(1);

        Long id = service.create(createRequest("P-001", 7L));

        assertThat(id).isEqualTo(100L);
        verify(projectMapper).insert(any(ResearchProjectEntity.class));
        verify(memberMapper).insert(any(ProjectMemberEntity.class));
        verify(auditLogService).record(any());
    }

    @Test
    void createProjectPersistsSelectedHerbSpecies() {
        UserEntity leader = user(7L, "teacher");
        HerbEntity herb = new HerbEntity();
        herb.setId(3L);
        herb.setStatus(1);
        herb.setIsDeleted(0);
        when(projectMapper.selectByProjectNoIncludingDeleted("P-002")).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(leader);
        when(herbSpeciesMapper.selectActiveById(3L)).thenReturn(herb);
        when(projectMapper.insert(any(ResearchProjectEntity.class)))
                .thenAnswer(
                        invocation -> {
                            invocation.getArgument(0, ResearchProjectEntity.class).setId(101L);
                            return 1;
                        });
        when(memberMapper.insert(any(ProjectMemberEntity.class))).thenReturn(1);

        ResearchProjectCreateRequest request = createRequest("P-002", 7L);
        request.setSpeciesId(3L);
        service.create(request);

        ArgumentCaptor<ResearchProjectEntity> captor =
                ArgumentCaptor.forClass(ResearchProjectEntity.class);
        verify(projectMapper).insert(captor.capture());
        assertThat(captor.getValue().getSpeciesId()).isEqualTo(3L);
    }

    @Test
    void logicallyDeletedProjectNumberCannotBeReused() {
        ResearchProjectEntity deleted = new ResearchProjectEntity();
        deleted.setId(99L);
        deleted.setIsDeleted(1);
        when(projectMapper.selectByProjectNoIncludingDeleted("P-001")).thenReturn(deleted);

        assertThatThrownBy(() -> service.create(createRequest("P-001", 7L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(projectMapper, never()).insert(any(ResearchProjectEntity.class));
    }

    @Test
    void inactiveOrUnsupportedLeaderIsRejected() {
        UserEntity leader = user(7L, "student");
        when(projectMapper.selectByProjectNoIncludingDeleted("P-001")).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(leader);

        assertThatThrownBy(() -> service.create(createRequest("P-001", 7L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Leader");
    }

    @Test
    void invalidProjectTimeRangeIsRejected() {
        ResearchProjectCreateRequest request = createRequest("P-001", 7L);
        request.setStartedAt(LocalDateTime.of(2026, 2, 1, 0, 0));
        request.setEndedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("before");
    }

    @Test
    void unsupportedProjectTypeIsRejected() {
        ResearchProjectCreateRequest request = createRequest("P-001", 7L);
        request.setProjectType("unknown");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid project type");
    }

    @Test
    void missingSpeciesIsRejected() {
        when(projectMapper.selectByProjectNoIncludingDeleted("P-001")).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(user(7L, "teacher"));
        when(herbSpeciesMapper.selectActiveById(3L)).thenReturn(null);
        ResearchProjectCreateRequest request = createRequest("P-001", 7L);
        request.setSpeciesId(3L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Herb species");
    }

    @Test
    void leaderMembershipFailureStopsCreate() {
        when(projectMapper.selectByProjectNoIncludingDeleted("P-001")).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(user(7L, "teacher"));
        when(projectMapper.insert(any(ResearchProjectEntity.class)))
                .thenAnswer(
                        invocation -> {
                            invocation.getArgument(0, ResearchProjectEntity.class).setId(100L);
                            return 1;
                        });
        when(memberMapper.insert(any(ProjectMemberEntity.class))).thenReturn(0);

        assertThatThrownBy(() -> service.create(createRequest("P-001", 7L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("leader membership");
        verify(auditLogService, never()).record(any());
    }

    @Test
    void pageMapsProjectRowsWithoutReturningEntities() {
        ResearchProjectEntity entity = project(100L, "P-001");
        entity.setLeaderId(7L);
        Page<ResearchProjectEntity> page = Page.of(1, 10);
        page.setRecords(List.of(entity));
        page.setTotal(1);
        when(projectMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(user(7L, "teacher")));

        PageResult<ResearchProjectListVO> result =
                service.page(new com.bdis.modules.research.query.ResearchProjectQuery());

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getProjectNo()).isEqualTo("P-001");
        assertThat(result.getRecords().get(0).getLeaderName()).isEqualTo("Researcher");
    }

    @Test
    void updateProjectBasicFieldsRecordsAudit() {
        ResearchProjectEntity existing = project(100L, "P-001");
        when(projectMapper.selectById(100L)).thenReturn(existing);
        when(projectMapper.updateById(existing)).thenReturn(1);

        ResearchProjectUpdateRequest request = new ResearchProjectUpdateRequest();
        request.setProjectName("Updated");
        request.setProjectType("research");
        request.setVersion(0);
        service.update(100L, request);

        assertThat(existing.getProjectName()).isEqualTo("Updated");
        verify(projectMapper).updateById(existing);
        verify(auditLogService).record(any());
    }

    @Test
    void projectLeaderCannotStartPlanningProjectBeforeReviewApproval() {
        ResearchProjectEntity existing = project(100L, "P-001");
        existing.setLeaderId(7L);
        existing.setReviewStatus("draft");
        when(projectMapper.selectById(100L)).thenReturn(existing);
        setUser(7L, "RESEARCHER");

        ResearchProjectStatusChangeRequest request = new ResearchProjectStatusChangeRequest();
        request.setTargetStatus("ongoing");
        request.setVersion(0);

        assertThatThrownBy(() -> service.changeStatus(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("review approval");
        verify(projectMapper, never()).updateById(any(ResearchProjectEntity.class));
    }

    @Test
    void lifecycleStatusChangeDoesNotRewriteReviewDecision() {
        ResearchProjectEntity existing = project(100L, "P-001");
        existing.setLeaderId(7L);
        existing.setProjectStatus("ongoing");
        existing.setReviewStatus("approved");
        existing.setReviewComment("approved by committee");
        existing.setReviewedBy(9L);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 7, 15, 10, 0);
        existing.setReviewedAt(reviewedAt);
        when(projectMapper.selectById(100L)).thenReturn(existing);
        when(projectMapper.updateById(existing)).thenReturn(1);
        setUser(7L, "RESEARCHER");

        ResearchProjectStatusChangeRequest request = new ResearchProjectStatusChangeRequest();
        request.setTargetStatus("suspended");
        request.setReason("pause experiments");
        request.setVersion(0);

        service.changeStatus(100L, request);

        assertThat(existing.getReviewStatus()).isEqualTo("approved");
        assertThat(existing.getReviewComment()).isEqualTo("approved by committee");
        assertThat(existing.getReviewedBy()).isEqualTo(9L);
        assertThat(existing.getReviewedAt()).isEqualTo(reviewedAt);
    }

    @Test
    void missingProjectDetailFails() {
        when(projectMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail(100L))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class);
    }

    private ResearchProjectCreateRequest createRequest(String no, Long leaderId) {
        ResearchProjectCreateRequest request = new ResearchProjectCreateRequest();
        request.setProjectNo(no);
        request.setProjectName("Research project");
        request.setProjectType("research");
        request.setLeaderId(leaderId);
        return request;
    }

    private UserEntity user(Long id, String type) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setUserType(type);
        user.setRealName("Researcher");
        user.setUsername("researcher");
        return user;
    }

    private ResearchProjectEntity project(Long id, String no) {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(id);
        project.setProjectNo(no);
        project.setProjectName("Original");
        project.setProjectType("research");
        project.setProjectStatus("planning");
        project.setStatus(1);
        project.setIsDeleted(0);
        project.setVersion(0);
        return project;
    }
}
