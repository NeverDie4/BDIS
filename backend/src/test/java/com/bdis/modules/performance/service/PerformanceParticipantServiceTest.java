package com.bdis.modules.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceParticipantMapper;
import com.bdis.modules.performance.service.impl.PerformanceParticipantServiceImpl;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerformanceParticipantServiceTest {

    @Mock private PerformanceMapper performanceMapper;
    @Mock private PerformanceParticipantMapper participantMapper;
    @Mock private UserMapper userMapper;
    @Mock private BusinessAccessService accessService;

    @Test
    void candidateUsersUsePerformanceDataScope() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        UserEntity user = activeUser(9L);
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        assertThat(service().listParticipantUsers(8L))
                .extracting(candidate -> candidate.getId())
                .containsExactly(9L);

        verify(accessService).applyUserScope(any(), org.mockito.ArgumentMatchers.eq("perf_record"));
    }

    @Test
    void addParticipantRejectsUserOutsidePerformanceDataScope() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        doThrow(new ForbiddenException("参与人超出当前数据范围"))
                .when(accessService)
                .requireUserInScope("perf_record", 9L);
        PerformanceParticipantRequest request = new PerformanceParticipantRequest();
        request.setUserId(9L);
        request.setParticipantRole("participant");

        assertThatThrownBy(() -> service().addParticipant(8L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("参与人超出当前数据范围");
        verify(participantMapper, never()).insert(any(PerformanceParticipantEntity.class));
    }

    @Test
    void updateParticipantRejectsUserOutsidePerformanceDataScope() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        when(participantMapper.selectById(3L)).thenReturn(participant(3L, 7L));
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        doThrow(new ForbiddenException("参与人超出当前数据范围"))
                .when(accessService)
                .requireUserInScope("perf_record", 9L);
        PerformanceParticipantRequest request = new PerformanceParticipantRequest();
        request.setUserId(9L);
        request.setParticipantRole("participant");

        assertThatThrownBy(() -> service().updateParticipant(8L, 3L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("参与人超出当前数据范围");
        verify(participantMapper, never()).updateById(any(PerformanceParticipantEntity.class));
    }

    private PerformanceParticipantServiceImpl service() {
        return new PerformanceParticipantServiceImpl(
                performanceMapper, participantMapper, userMapper, accessService);
    }

    private PerformanceEntity editablePerformance() {
        PerformanceEntity performance = new PerformanceEntity();
        performance.setId(8L);
        performance.setUserId(5L);
        performance.setIdentifyStatus("draft");
        return performance;
    }

    private UserEntity activeUser(Long userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("participant-" + userId);
        user.setStatus(1);
        return user;
    }

    private PerformanceParticipantEntity participant(Long participantId, Long userId) {
        PerformanceParticipantEntity participant = new PerformanceParticipantEntity();
        participant.setId(participantId);
        participant.setPerformanceId(8L);
        participant.setUserId(userId);
        participant.setParticipantRole("participant");
        participant.setIsPrimary(0);
        return participant;
    }
}
