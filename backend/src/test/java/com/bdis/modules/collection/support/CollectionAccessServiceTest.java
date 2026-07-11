package com.bdis.modules.collection.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.user.mapper.UserMapper;
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
class CollectionAccessServiceTest {

    @Mock private DataScopeService dataScopeService;

    @Mock private UserMapper userMapper;

    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;

    private CollectionAccessService collectionAccessService;

    @BeforeEach
    void setUp() {
        collectionAccessService =
                new CollectionAccessService(dataScopeService, userMapper, herbCollectionTaskMapper);
        CurrentUser current =
                new CurrentUser(
                        1001L,
                        "collector-a",
                        "采集员 A",
                        10L,
                        20L,
                        Set.of("COLLECTOR"),
                        Set.of(3L),
                        Set.of("growth:record:view", "growth:record:update"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(current, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void taskManagerCannotModifyAnotherUsersTaskWithSelfScope() {
        DataScopeResultVO scope = new DataScopeResultVO();
        scope.setSelfIncluded(true);
        when(dataScopeService.resolveForCurrentUser("herb_growth_record")).thenReturn(scope);
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(1L);
        task.setCreatedBy(2002L);
        task.setCollectorId(2002L);

        assertThatThrownBy(() -> collectionAccessService.requireTaskManage(task))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("本人创建");
    }

    @Test
    void batchOwnerCannotMutateAnotherUsersUnassignedBatch() {
        DataScopeResultVO scope = new DataScopeResultVO();
        scope.setSelfIncluded(true);
        when(dataScopeService.resolveForCurrentUser("herb_growth_record")).thenReturn(scope);
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(1L);
        batch.setCreatedBy(2002L);

        assertThatThrownBy(() -> collectionAccessService.requireBatchOwner(batch))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("本人负责");
    }

    @Test
    void administratorDataScopeCanManageOtherUsersTask() {
        DataScopeResultVO scope = new DataScopeResultVO();
        scope.setAllIncluded(true);
        when(dataScopeService.resolveForCurrentUser("herb_growth_record")).thenReturn(scope);
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(1L);
        task.setCreatedBy(2002L);

        collectionAccessService.requireTaskManage(task);
    }
}
