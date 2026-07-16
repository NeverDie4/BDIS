package com.bdis.modules.training.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingRecordFileBusinessAccessPolicyTest {
    @Mock private TrainingRecordMapper recordMapper;
    @Mock private TrainingPlanMapper planMapper;
    @Mock private AuthorizationService authorizationService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void participantCanViewOwnCompletionProofButAnotherLearnerCannot() {
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(1L);
        record.setPlanId(2L);
        record.setUserId(7L);
        when(recordMapper.selectActiveById(1L)).thenReturn(record);
        when(authorizationService.hasPermission("edu:training-record:detail")).thenReturn(true);
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(2L);
        plan.setOwnerId(6L);
        plan.setTrainerId(5L);
        when(planMapper.selectById(2L)).thenReturn(plan);
        TrainingRecordFileBusinessAccessPolicy policy =
                new TrainingRecordFileBusinessAccessPolicy(
                        recordMapper, planMapper, authorizationService);

        setUser(7L);
        assertThat(policy.canView(1L)).isTrue();
        setUser(8L);
        assertThat(policy.canView(1L)).isFalse();
    }

    private void setUser(Long id) {
        CurrentUser user =
                new CurrentUser(
                        id,
                        "user-" + id,
                        "User",
                        null,
                        null,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of("edu:training-record:detail"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }
}
