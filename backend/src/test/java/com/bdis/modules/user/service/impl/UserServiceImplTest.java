package com.bdis.modules.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.core.PageResult;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.entity.UserRoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import com.bdis.modules.user.query.UserQuery;
import com.bdis.modules.user.vo.UserVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserSessionService userSessionService;

    @Test
    void roleFilterUsesDatabasePaginationAndKeepsFilteredTotal() {
        UserServiceImpl service =
                new UserServiceImpl(
                        userMapper,
                        roleMapper,
                        userRoleMapper,
                        passwordEncoder,
                        userSessionService);
        UserQuery query = new UserQuery();
        query.setRoleId(7L);
        query.setPage(2);
        query.setSize(1);

        UserEntity targetUser = new UserEntity();
        targetUser.setId(30L);
        targetUser.setUsername("reviewer02");
        Page<UserEntity> databasePage = new Page<>(2, 1, 2);
        databasePage.setRecords(List.of(targetUser));
        when(userMapper.selectUserPage(any(Page.class), org.mockito.Mockito.same(query)))
                .thenReturn(databasePage);

        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(30L);
        relation.setRoleId(7L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(relation));
        RoleEntity role = new RoleEntity();
        role.setId(7L);
        role.setRoleCode("REVIEWER");
        role.setRoleName("审核员");
        when(roleMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(role));

        PageResult<UserVO> result = service.page(query);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords())
                .extracting(UserVO::getUsername)
                .containsExactly("reviewer02");
        assertThat(result.getRecords().getFirst().getRoles())
                .extracting(com.bdis.modules.user.vo.RoleVO::getId)
                .containsExactly(7L);
        verify(userMapper).selectUserPage(any(Page.class), org.mockito.Mockito.same(query));
    }
}
