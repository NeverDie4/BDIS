package com.bdis.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.AuthorizationDecisionVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessAccessServiceTest {

    @Mock private AuthorizationService authorizationService;
    @Mock private DataScopeService dataScopeService;
    @Mock private UserMapper userMapper;
    @InjectMocks private BusinessAccessService accessService;

    @Test
    void rejectsResourceOutsideCurrentUsersDataScope() {
        AuthorizationDecisionVO decision = new AuthorizationDecisionVO();
        decision.setAllowed(false);
        decision.setReason("资源不在当前用户数据范围内");
        when(authorizationService.decide(any())).thenReturn(decision);

        assertThatThrownBy(
                        () ->
                                accessService.requireResourceAccess(
                                        "perf_record", 9L, "performance:record:update", 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("资源不在当前用户数据范围内");
    }

    @Test
    void rejectsUserOutsidePerformanceDataScope() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> accessService.requireUserInScope("perf_record", 9L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("参与人超出当前数据范围");

        verify(dataScopeService)
                .applyToQuery(
                        any(LambdaQueryWrapper.class),
                        eq("perf_record"),
                        any(),
                        any(),
                        any());
    }
}
