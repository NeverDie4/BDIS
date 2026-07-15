package com.bdis.modules.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.performance.dto.PerformanceParticipantRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceParticipantMapper;
import com.bdis.modules.performance.service.impl.PerformanceParticipantServiceImpl;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.AuthorizationDecisionVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PerformanceParticipantServiceTest {

    @Mock private PerformanceMapper performanceMapper;
    @Mock private PerformanceParticipantMapper participantMapper;
    @Mock private UserMapper userMapper;
    @Mock private AuthorizationService authorizationService;
    @Mock private DataScopeService dataScopeService;

    private BusinessAccessService accessService;

    @BeforeEach
    void setUpStudentSession() {
        CurrentUser student =
                new CurrentUser(
                        5L,
                        "student-owner",
                        "Student Owner",
                        1L,
                        10L,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of("performance:record:update"));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(student, null, List.of()));
        initializeUserTableMetadata();
        accessService =
                new BusinessAccessService(authorizationService, dataScopeService, userMapper);
        AuthorizationDecisionVO allowed = new AuthorizationDecisionVO();
        allowed.setAllowed(true);
        when(authorizationService.decide(any())).thenReturn(allowed);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentOwnerCanListAndAddSameDepartmentParticipant() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        UserEntity owner = student(5L, 1L, 10L);
        UserEntity peer = student(9L, 1L, 10L);
        when(userMapper.selectById(5L)).thenReturn(owner);
        when(userMapper.selectById(9L)).thenReturn(peer);
        Page<UserEntity> candidates = new Page<>(1L, 50L, 1L);
        candidates.setRecords(List.of(peer));
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(candidates);

        assertThat(service().listParticipantUsers(8L, "participant", 0L, 500L).getRecords())
                .extracting(candidate -> candidate.getId())
                .containsExactly(9L);
        ArgumentCaptor<Page<UserEntity>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<UserEntity>> scopeCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(userMapper).selectPage(pageCaptor.capture(), scopeCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(50L);
        assertThat(scopeCaptor.getValue().getSqlSegment())
                .contains("organization_id")
                .contains("department_id")
                .contains("real_name")
                .contains("username");
        assertThat(scopeCaptor.getValue().getParamNameValuePairs())
                .containsValue(1L)
                .containsValue(10L);

        PerformanceParticipantRequest request = requestFor(9L);
        service().addParticipant(8L, request);

        ArgumentCaptor<PerformanceParticipantEntity> participantCaptor =
                ArgumentCaptor.forClass(PerformanceParticipantEntity.class);
        verify(participantMapper).insert(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getUserId()).isEqualTo(9L);
        verifyNoInteractions(dataScopeService);
    }

    @Test
    void studentOwnerCannotAddParticipantFromAnotherDepartment() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        when(userMapper.selectById(5L)).thenReturn(student(5L, 1L, 10L));
        when(userMapper.selectById(9L)).thenReturn(student(9L, 1L, 11L));

        assertThatThrownBy(() -> service().addParticipant(8L, requestFor(9L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("参与人必须与业绩负责人属于同一部门或机构");
        verify(participantMapper, never()).insert(any(PerformanceParticipantEntity.class));
    }

    @Test
    void studentOwnerCannotUpdateParticipantToAnotherOrganization() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        when(participantMapper.selectById(3L)).thenReturn(participant(3L, 7L));
        when(userMapper.selectById(5L)).thenReturn(student(5L, 1L, 10L));
        when(userMapper.selectById(9L)).thenReturn(student(9L, 2L, 10L));

        assertThatThrownBy(() -> service().updateParticipant(8L, 3L, requestFor(9L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("参与人必须与业绩负责人属于同一部门或机构");
        verify(participantMapper, never()).updateById(any(PerformanceParticipantEntity.class));
    }

    @Test
    void ordinaryParticipantCannotUseReservedOwnerRole() {
        when(performanceMapper.selectById(8L)).thenReturn(editablePerformance());
        when(userMapper.selectById(5L)).thenReturn(student(5L, 1L, 10L));
        when(userMapper.selectById(9L)).thenReturn(student(9L, 1L, 10L));
        PerformanceParticipantRequest request = requestFor(9L);
        request.setParticipantRole("owner");

        assertThatThrownBy(() -> service().addParticipant(8L, request))
                .hasMessage("owner 是负责人保留角色，普通参与人不能使用");
        verify(participantMapper, never()).insert(any(PerformanceParticipantEntity.class));
    }

    private PerformanceParticipantServiceImpl service() {
        return new PerformanceParticipantServiceImpl(
                performanceMapper, participantMapper, userMapper, accessService);
    }

    private void initializeUserTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, UserEntity.class.getName());
        assistant.setCurrentNamespace(UserEntity.class.getName());
        TableInfoHelper.initTableInfo(assistant, UserEntity.class);
    }

    private PerformanceEntity editablePerformance() {
        PerformanceEntity performance = new PerformanceEntity();
        performance.setId(8L);
        performance.setUserId(5L);
        performance.setIdentifyStatus("draft");
        return performance;
    }

    private PerformanceParticipantRequest requestFor(Long userId) {
        PerformanceParticipantRequest request = new PerformanceParticipantRequest();
        request.setUserId(userId);
        request.setParticipantRole("participant");
        return request;
    }

    private UserEntity student(Long userId, Long organizationId, Long departmentId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("participant-" + userId);
        user.setRealName("参与人" + userId);
        user.setStatus(1);
        user.setUserType("student");
        user.setOrganizationId(organizationId);
        user.setDepartmentId(departmentId);
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
