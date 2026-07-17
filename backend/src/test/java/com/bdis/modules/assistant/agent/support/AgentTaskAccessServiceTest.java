package com.bdis.modules.assistant.agent.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.support.CollectionAccessService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AgentTaskAccessServiceTest {

    @Mock private CollectionAccessService collectionAccessService;

    private AgentTaskAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new AgentTaskAccessService(collectionAccessService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCreationReusesCollectionTaskManageScope() {
        authenticate(1001L, "TEACHER");
        HerbCollectionTaskEntity collectionTask = collectionTask(12L, 1001L, 2001L);

        accessService.requireCreateAccess(collectionTask);

        verify(collectionAccessService).requireTaskManage(collectionTask);
    }

    @Test
    void collectorCannotCreateHighPrivilegeAgentGoal() {
        authenticate(2001L, "COLLECTOR");

        assertThatThrownBy(
                        () -> accessService.requireCreateAccess(collectionTask(12L, 1001L, 2001L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("管理员或教师");
    }

    @Test
    void teacherCanCreateAgentForAnyTaskInManagementScope() {
        authenticate(1001L, "TEACHER");
        HerbCollectionTaskEntity collectionTask = collectionTask(12L, 3001L, 2001L);

        accessService.requireCreateAccess(collectionTask);

        verify(collectionAccessService).requireTaskManage(collectionTask);
    }

    @Test
    void differentUnrelatedUserCannotViewAgentTask() {
        authenticate(3001L, "TEACHER");
        AgentTaskEntity agentTask = new AgentTaskEntity();
        agentTask.setUserId(1001L);

        assertThatThrownBy(
                        () ->
                                accessService.requireViewAccess(
                                        agentTask, collectionTask(12L, 1001L, 2001L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void administratorCanViewAllWithoutCollectionScopeLookup() {
        authenticate(9001L, "ADMIN");
        AgentTaskEntity agentTask = new AgentTaskEntity();
        agentTask.setUserId(1001L);

        accessService.requireViewAccess(agentTask, collectionTask(12L, 1001L, 2001L));

        verifyNoInteractions(collectionAccessService);
    }

    private void authenticate(Long userId, String role) {
        CurrentUser current =
                new CurrentUser(
                        userId,
                        "user-" + userId,
                        "测试用户",
                        1L,
                        1L,
                        Set.of(role),
                        Set.of(1L),
                        Set.of("growth:record:view", "growth:record:create"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(current, null));
    }

    private HerbCollectionTaskEntity collectionTask(Long id, Long createdBy, Long collectorId) {
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(id);
        task.setCreatedBy(createdBy);
        task.setCollectorId(collectorId);
        return task;
    }
}
