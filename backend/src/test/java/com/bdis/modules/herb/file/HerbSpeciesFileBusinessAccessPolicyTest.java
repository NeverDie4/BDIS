package com.bdis.modules.herb.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HerbSpeciesFileBusinessAccessPolicyTest {

    @Mock private HerbSpeciesMapper herbSpeciesMapper;
    @Mock private AuthorizationService authorizationService;

    private HerbSpeciesFileBusinessAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new HerbSpeciesFileBusinessAccessPolicy(herbSpeciesMapper, authorizationService);
    }

    @Test
    void existsUsesActiveHerbSpecies() {
        when(herbSpeciesMapper.selectActiveById(21L)).thenReturn(new HerbEntity());

        assertThat(policy.exists(21L)).isTrue();
        assertThat(policy.exists(22L)).isFalse();
    }

    @Test
    void createPermissionCanAttachAndPublishNewCover() {
        allowPermissions("herb:species:create");

        assertThat(policy.canAttach(21L)).isTrue();
        assertThat(policy.canPublish(21L)).isTrue();
        assertThat(policy.canDetach(21L)).isFalse();
    }

    @Test
    void deletePermissionCanDetachAndPublishExistingCoverRemoval() {
        allowPermissions("herb:species:delete");

        assertThat(policy.canAttach(21L)).isFalse();
        assertThat(policy.canDetach(21L)).isTrue();
        assertThat(policy.canPublish(21L)).isTrue();
    }

    @Test
    void updatePermissionCanManageCoverLifecycle() {
        allowPermissions("herb:species:update");

        assertThat(policy.canAttach(21L)).isTrue();
        assertThat(policy.canDetach(21L)).isTrue();
        assertThat(policy.canPublish(21L)).isTrue();
    }

    private void allowPermissions(String... permissions) {
        java.util.Set<String> allowed = java.util.Set.of(permissions);
        when(authorizationService.hasPermission(anyString()))
                .thenAnswer(invocation -> allowed.contains(invocation.getArgument(0)));
    }
}
