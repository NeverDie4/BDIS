package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ProjectMemberAddRequest;
import com.bdis.modules.research.request.ProjectMemberUpdateRequest;
import com.bdis.modules.research.service.impl.ProjectMemberServiceImpl;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private UserMapper userMapper;
    @Mock private AuditLogService auditLogService;

    private ProjectMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ProjectMemberServiceImpl(
                        projectMapper, memberMapper, userMapper, auditLogService);
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(10L);
        project.setStatus(1);
        project.setIsDeleted(0);
        when(projectMapper.selectById(10L)).thenReturn(project);
    }

    @Test
    void addingActiveMemberAgainFails() {
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        ProjectMemberEntity existing = member(1L, 10L, 7L, "active", "student");
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(existing);

        assertThatThrownBy(() -> service.add(10L, addRequest(7L, "student")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void addingLeftMemberRestoresTheSameRelation() {
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        ProjectMemberEntity existing = member(1L, 10L, 7L, "left", "student");
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(existing);
        when(memberMapper.updateById(existing)).thenReturn(1);

        service.add(10L, addRequest(7L, "researcher"));

        verify(memberMapper).updateById(existing);
        verify(auditLogService).record(any());
    }

    @Test
    void addingNewMemberCreatesActiveRelation() {
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(null);
        when(memberMapper.insert(any(ProjectMemberEntity.class)))
                .thenAnswer(
                        invocation -> {
                            invocation.getArgument(0, ProjectMemberEntity.class).setId(2L);
                            return 1;
                        });

        service.add(10L, addRequest(7L, "student"));

        verify(memberMapper).insert(any(ProjectMemberEntity.class));
        verify(auditLogService).record(any());
    }

    @Test
    void inactiveUserCannotBeAdded() {
        UserEntity user = activeUser(7L);
        user.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(user);

        assertThatThrownBy(() -> service.add(10L, addRequest(7L, "student")))
                .isInstanceOf(com.bdis.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void ordinaryMemberCannotBeMadeLeader() {
        ProjectMemberUpdateRequest request = new ProjectMemberUpdateRequest();
        request.setMemberRole("leader");

        assertThatThrownBy(() -> service.updateRole(10L, 7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("leader");
    }

    @Test
    void leaderCannotLeaveProject() {
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L))
                .thenReturn(member(1L, 10L, 7L, "active", "leader"));

        assertThatThrownBy(() -> service.remove(10L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("leader");
    }

    @Test
    void leavingMemberChangesStateInsteadOfDeleting() {
        ProjectMemberEntity existing = member(1L, 10L, 7L, "active", "student");
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(existing);
        when(memberMapper.updateById(existing)).thenReturn(1);

        service.remove(10L, 7L);

        verify(memberMapper).updateById(existing);
        verify(auditLogService).record(any());
    }

    @Test
    void ordinaryMemberRoleCanBeUpdated() {
        ProjectMemberEntity existing = member(1L, 10L, 7L, "active", "student");
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L)).thenReturn(existing);
        when(memberMapper.updateById(existing)).thenReturn(1);
        ProjectMemberUpdateRequest request = new ProjectMemberUpdateRequest();
        request.setMemberRole("assistant");

        service.updateRole(10L, 7L, request);

        verify(memberMapper).updateById(existing);
        verify(auditLogService).record(any());
    }

    @Test
    void alreadyLeftMemberCannotLeaveAgain() {
        when(memberMapper.selectByProjectIdAndUserId(10L, 7L))
                .thenReturn(member(1L, 10L, 7L, "left", "student"));

        assertThatThrownBy(() -> service.remove(10L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already left");
    }

    private ProjectMemberAddRequest addRequest(Long userId, String role) {
        ProjectMemberAddRequest request = new ProjectMemberAddRequest();
        request.setUserId(userId);
        request.setMemberRole(role);
        return request;
    }

    private UserEntity activeUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setUserType("student");
        return user;
    }

    private ProjectMemberEntity member(
            Long id, Long projectId, Long userId, String status, String role) {
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setId(id);
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setMemberStatus(status);
        member.setMemberRole(role);
        return member;
    }
}
