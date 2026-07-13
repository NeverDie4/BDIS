package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ProjectMemberMapper;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ResearchProjectCreateRequest;
import com.bdis.modules.research.request.ResearchProjectUpdateRequest;
import com.bdis.modules.research.service.impl.ResearchProjectServiceImpl;
import com.bdis.modules.research.vo.ResearchProjectDetailVO;
import com.bdis.modules.research.vo.ResearchProjectListVO;
import com.bdis.common.core.PageResult;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        service = new ResearchProjectServiceImpl(
                projectMapper, memberMapper, userMapper, herbSpeciesMapper, memberService, materialService, auditLogService);
    }

    @Test
    void createProjectForcesPlanningAndAddsLeader() {
        UserEntity leader = user(7L, "teacher");
        when(projectMapper.selectByProjectNoIncludingDeleted("P-001")).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(leader);
        when(projectMapper.insert(any(ResearchProjectEntity.class))).thenAnswer(invocation -> {
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
        when(projectMapper.insert(any(ResearchProjectEntity.class))).thenAnswer(invocation -> {
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

        PageResult<ResearchProjectListVO> result = service.page(new com.bdis.modules.research.query.ResearchProjectQuery());

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
        service.update(100L, request);

        assertThat(existing.getProjectName()).isEqualTo("Updated");
        verify(projectMapper).updateById(existing);
        verify(auditLogService).record(any());
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
        return project;
    }
}
