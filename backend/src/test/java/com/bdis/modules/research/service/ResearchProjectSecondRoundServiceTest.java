package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ResearchProjectLeaderChangeRequest;
import com.bdis.modules.research.request.ResearchProjectStatusChangeRequest;
import com.bdis.modules.research.service.impl.ResearchProjectServiceImpl;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearchProjectSecondRoundServiceTest {

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
        service = new ResearchProjectServiceImpl(projectMapper, memberMapper, userMapper, herbSpeciesMapper,
                memberService, materialService, auditLogService);
    }

    @Test
    void changeLeaderDemotesOldLeaderAndPromotesExistingMember() {
        ResearchProjectEntity project = project("planning");
        ProjectMemberEntity oldLeader = member(1L, 7L, "leader", "active");
        ProjectMemberEntity newLeader = member(2L, 8L, "researcher", "active");
        when(projectMapper.selectById(10L)).thenReturn(project);
        when(userMapper.selectById(8L)).thenReturn(leader(8L));
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(oldLeader);
        when(memberMapper.selectByProjectIdAndUserId(10L, 8L)).thenReturn(newLeader);
        when(projectMapper.updateById(project)).thenReturn(1);
        when(memberMapper.updateById(any(ProjectMemberEntity.class))).thenReturn(1);

        service.changeLeader(10L, leaderRequest(8L, "assistant"));

        assertThat(project.getLeaderId()).isEqualTo(8L);
        assertThat(oldLeader.getMemberRole()).isEqualTo("assistant");
        assertThat(newLeader.getMemberRole()).isEqualTo("leader");
        assertThat(newLeader.getMemberStatus()).isEqualTo("active");
        verify(auditLogService).record(any());
    }

    @Test
    void completedProjectCannotChangeLeader() {
        when(projectMapper.selectById(10L)).thenReturn(project("completed"));

        assertThatThrownBy(() -> service.changeLeader(10L, leaderRequest(8L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void leftNewLeaderIsRestoredAndPromoted() {
        ResearchProjectEntity project = project("ongoing");
        ProjectMemberEntity oldLeader = member(1L, 7L, "leader", "active");
        ProjectMemberEntity formerMember = member(2L, 8L, "student", "left");
        when(projectMapper.selectById(10L)).thenReturn(project);
        when(userMapper.selectById(8L)).thenReturn(leader(8L));
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(oldLeader);
        when(memberMapper.selectByProjectIdAndUserId(10L, 8L)).thenReturn(formerMember);
        when(projectMapper.updateById(project)).thenReturn(1);
        when(memberMapper.updateById(any(ProjectMemberEntity.class))).thenReturn(1);

        service.changeLeader(10L, leaderRequest(8L, null));

        assertThat(formerMember.getMemberRole()).isEqualTo("leader");
        assertThat(formerMember.getMemberStatus()).isEqualTo("active");
        assertThat(formerMember.getLeftAt()).isNull();
    }

    @Test
    void planningCanBecomeOngoingWhenLeaderAndMemberAreValid() {
        ResearchProjectEntity project = project("planning");
        when(projectMapper.selectById(10L)).thenReturn(project);
        when(userMapper.selectById(7L)).thenReturn(leader(7L));
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(member(1L, 7L, "leader", "active"));
        when(memberMapper.selectCount(any())).thenReturn(1L);
        when(projectMapper.updateById(project)).thenReturn(1);

        service.changeStatus(10L, statusRequest("ongoing", "start"));

        assertThat(project.getProjectStatus()).isEqualTo("ongoing");
        verify(auditLogService).record(any());
    }

    @Test
    void invalidStatusTransitionIsRejected() {
        when(projectMapper.selectById(10L)).thenReturn(project("planning"));

        assertThatThrownBy(() -> service.changeStatus(10L, statusRequest("completed", "skip")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("transition");
    }

    @Test
    void suspensionAndResumeRequireReason() {
        when(projectMapper.selectById(10L)).thenReturn(project("ongoing"));
        ResearchProjectStatusChangeRequest request = statusRequest("suspended", null);

        assertThatThrownBy(() -> service.changeStatus(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void completionRequiresReason() {
        when(projectMapper.selectById(10L)).thenReturn(project("ongoing"));

        assertThatThrownBy(() -> service.changeStatus(10L, statusRequest("completed", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reason");
    }

    private ResearchProjectLeaderChangeRequest leaderRequest(Long newLeaderId, String oldRole) {
        ResearchProjectLeaderChangeRequest request = new ResearchProjectLeaderChangeRequest();
        request.setNewLeaderId(newLeaderId);
        request.setOldLeaderRole(oldRole);
        request.setReason("handover");
        request.setVersion(0);
        return request;
    }

    private ResearchProjectStatusChangeRequest statusRequest(String target, String reason) {
        ResearchProjectStatusChangeRequest request = new ResearchProjectStatusChangeRequest();
        request.setTargetStatus(target);
        request.setReason(reason);
        request.setVersion(0);
        return request;
    }

    private ResearchProjectEntity project(String status) {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(10L); project.setProjectNo("P-001"); project.setProjectName("Research");
        project.setProjectStatus(status); project.setLeaderId(7L); project.setStatus(1); project.setIsDeleted(0); project.setVersion(0);
        return project;
    }

    private ProjectMemberEntity member(Long id, Long userId, String role, String status) {
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setId(id); member.setProjectId(10L); member.setUserId(userId); member.setMemberRole(role); member.setMemberStatus(status);
        return member;
    }

    private UserEntity leader(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id); user.setStatus(1); user.setIsDeleted(0); user.setUserType("teacher");
        return user;
    }
}
