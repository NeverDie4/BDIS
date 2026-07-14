package com.bdis.modules.research.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ResearchProjectFileBusinessAccessPolicyTest {

    @Mock private ResearchProjectMapper projectMapper;
    @Mock private AuthorizationService authorizationService;

    private ResearchProjectFileBusinessAccessPolicy policy;
    private ResearchProjectEntity project;

    @BeforeEach
    void setUp() {
        policy = new ResearchProjectFileBusinessAccessPolicy(projectMapper, authorizationService);
        project = new ResearchProjectEntity();
        project.setId(1L);
        project.setLeaderId(10L);
        Mockito.lenient().when(authorizationService.hasPermission(anyString())).thenReturn(true);
        Mockito.lenient().when(projectMapper.selectById(1L)).thenReturn(project);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leaderCanViewAttachDetachAndPublish() {
        authenticate(10L, "TEACHER");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isTrue();
        assertThat(policy.canDetach(1L)).isTrue();
        assertThat(policy.canPublish(1L)).isTrue();
    }

    @Test
    void activeMemberCanViewButCannotManageProject() {
        authenticate(20L, "TEACHER");
        when(projectMapper.existsActiveMember(1L, 20L)).thenReturn(true);

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isFalse();
        assertThat(policy.canDetach(1L)).isFalse();
        assertThat(policy.canPublish(1L)).isFalse();
    }

    @Test
    void unrelatedUserCannotViewOrManageProject() {
        authenticate(30L, "TEACHER");

        assertThat(policy.canView(1L)).isFalse();
        assertThat(policy.canAttach(1L)).isFalse();
        assertThat(policy.canDetach(1L)).isFalse();
        assertThat(policy.canPublish(1L)).isFalse();
    }

    @Test
    void administratorCanViewAndManageProject() {
        authenticate(40L, "ADMIN");

        assertThat(policy.canView(1L)).isTrue();
        assertThat(policy.canAttach(1L)).isTrue();
        assertThat(policy.canDetach(1L)).isTrue();
        assertThat(policy.canPublish(1L)).isTrue();
    }

    @Test
    void missingOrDeletedProjectIsRejected() {
        authenticate(10L, "TEACHER");
        when(projectMapper.selectById(2L)).thenReturn(null);

        assertThat(policy.exists(2L)).isFalse();
        assertThat(policy.canView(2L)).isFalse();
        assertThat(policy.canAttach(2L)).isFalse();
        assertThat(policy.canDetach(2L)).isFalse();
        assertThat(policy.canPublish(2L)).isFalse();
    }

    private void authenticate(Long userId, String role) {
        CurrentUser user =
                new CurrentUser(
                        userId,
                        "user-" + userId,
                        "User " + userId,
                        null,
                        null,
                        Set.of(role),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
}
