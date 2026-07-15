package com.bdis.modules.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingPlanItemMapper;
import com.bdis.modules.training.mapper.TrainingRecordItemMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.request.TrainingRecordItemProgressRequest;
import com.bdis.modules.training.service.impl.TrainingRecordItemProgressServiceImpl;
import java.math.BigDecimal;
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
class TrainingRecordItemProgressServiceTest {
    @Mock private TrainingRecordMapper recordMapper;
    @Mock private TrainingPlanItemMapper itemMapper;
    @Mock private TrainingRecordItemMapper progressMapper;
    private TrainingRecordItemProgressService service;

    @BeforeEach void setUp() { service = new TrainingRecordItemProgressServiceImpl(recordMapper, itemMapper, progressMapper); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void participantCanCompleteAssignedTrainingItem() {
        CurrentUser user = new CurrentUser(8L, "student", "Student", null, null, Set.of("STUDENT"), Set.of(), Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
        TrainingRecordEntity record = new TrainingRecordEntity(); record.setId(10L); record.setUserId(8L); record.setPlanId(20L); record.setProgress(BigDecimal.ZERO);
        TrainingPlanItemEntity item = new TrainingPlanItemEntity(); item.setId(30L); item.setPlanId(20L); item.setIsDeleted(0); item.setStatus(1);
        when(recordMapper.selectById(10L)).thenReturn(record); when(itemMapper.selectActiveById(30L)).thenReturn(item);
        when(progressMapper.selectActive(10L, 30L)).thenReturn(null); when(progressMapper.insert(any(com.bdis.modules.training.entity.TrainingRecordItemEntity.class))).thenAnswer(invocation -> { ((com.bdis.modules.training.entity.TrainingRecordItemEntity) invocation.getArgument(0)).setId(40L); return 1; });
        TrainingRecordItemProgressRequest request = new TrainingRecordItemProgressRequest(); request.setProgress(BigDecimal.valueOf(100)); request.setCompleted(true);
        assertThat(service.save(10L, 30L, request).getId()).isEqualTo(40L);
    }
}
