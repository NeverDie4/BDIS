package com.bdis.modules.research.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.entity.ResearchProjectTaskEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.mapper.ResearchProjectTaskMapper;
import com.bdis.modules.research.mapper.ResearchProjectTaskMemberMapper;
import com.bdis.modules.research.request.ResearchProjectTaskCreateRequest;
import com.bdis.modules.research.service.impl.ResearchProjectTaskServiceImpl;
import java.util.Arrays;
import java.util.List;
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
class ResearchProjectTaskServiceTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ResearchProjectTaskMapper taskMapper;
    @Mock private ResearchProjectTaskMemberMapper memberMapper;
    private ResearchProjectTaskService service;

    @BeforeEach
    void setUp() {
        service = new ResearchProjectTaskServiceImpl(projectMapper, taskMapper, memberMapper);
        CurrentUser user =
                new CurrentUser(
                        8L,
                        "student",
                        "Student",
                        null,
                        null,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonMemberCannotReadTasksOrTaskMembers() {
        ResearchProjectEntity project = activeProject();
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(projectMapper.existsActiveMember(11L, 8L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(11L)).isInstanceOf(ForbiddenException.class);

        ResearchProjectTaskEntity task = new ResearchProjectTaskEntity();
        task.setId(21L);
        task.setProjectId(11L);
        task.setIsDeleted(0);
        when(taskMapper.selectById(21L)).thenReturn(task);
        assertThatThrownBy(() -> service.members(21L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void taskMapperProvidesCourseAndSpeciesRelationWrites() {
        assertThat(
                        Arrays.stream(ResearchProjectTaskMapper.class.getMethods())
                                .map(java.lang.reflect.Method::getName))
                .contains("insertCourseRelation", "insertSpeciesRelation");
    }

    @Test
    void createPersistsDeduplicatedCourseAndSpeciesRelations() {
        ResearchProjectEntity project = activeProject();
        project.setLeaderId(8L);
        when(projectMapper.selectById(11L)).thenReturn(project);
        doAnswer(
                        invocation -> {
                            ResearchProjectTaskEntity task = invocation.getArgument(0);
                            task.setId(21L);
                            return 1;
                        })
                .when(taskMapper)
                .insert(any(ResearchProjectTaskEntity.class));
        when(taskMapper.insertCourseRelation(any(), any(), any(Integer.class), any()))
                .thenReturn(1);
        when(taskMapper.insertSpeciesRelation(any(), any(), any(Integer.class), any()))
                .thenReturn(1);

        ResearchProjectTaskCreateRequest request = new ResearchProjectTaskCreateRequest();
        request.setTaskNo("T-001");
        request.setTaskName("Task");
        request.setCourseIds(List.of(2L, 2L, 3L));
        request.setSpeciesIds(List.of(4L, 4L));

        assertThat(service.create(11L, request)).isEqualTo(21L);
        verify(taskMapper).insertCourseRelation(21L, 2L, 0, 8L);
        verify(taskMapper).insertCourseRelation(21L, 3L, 1, 8L);
        verify(taskMapper).insertSpeciesRelation(21L, 4L, 0, 8L);
    }

    private ResearchProjectEntity activeProject() {
        ResearchProjectEntity project = new ResearchProjectEntity();
        project.setId(11L);
        project.setLeaderId(3L);
        project.setStatus(1);
        project.setIsDeleted(0);
        return project;
    }
}
