package com.bdis.modules.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.training.constant.TrainingStatus;
import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.entity.TrainingRecordItemEntity;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @BeforeEach
    void setUp() {
        service =
                new TrainingRecordItemProgressServiceImpl(recordMapper, itemMapper, progressMapper);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void participantCanCompleteAssignedTrainingItem() {
        authenticateParticipant();
        TrainingRecordEntity record = trainingRecord(TrainingStatus.NOT_STARTED);
        when(recordMapper.selectById(10L)).thenReturn(record);
        when(itemMapper.selectActiveById(30L)).thenReturn(planItem());
        when(progressMapper.selectActive(10L, 30L)).thenReturn(null);
        when(progressMapper.insert(any(TrainingRecordItemEntity.class)))
                .thenAnswer(
                        invocation -> {
                            invocation.getArgument(0, TrainingRecordItemEntity.class).setId(40L);
                            return 1;
                        });
        when(progressMapper.countRequiredItems(20L)).thenReturn(1);
        when(progressMapper.countCompletedRequiredItems(10L)).thenReturn(1);
        when(recordMapper.updateById(record)).thenReturn(1);
        assertThat(service.save(10L, 30L, progressRequest(true)).getId()).isEqualTo(40L);
        assertThat(record.getTrainingStatus()).isEqualTo(TrainingStatus.LEARNING);
        assertThat(record.getStartedAt()).isNotNull();
        verify(recordMapper).updateById(record);
    }

    @ParameterizedTest
    @ValueSource(strings = {TrainingStatus.COMPLETED, TrainingStatus.FAILED})
    void completedAndFailedTrainingRecordsAreReadOnly(String trainingStatus) {
        authenticateParticipant();
        TrainingRecordEntity record = trainingRecord(trainingStatus);
        when(recordMapper.selectById(10L)).thenReturn(record);

        assertThatThrownBy(() -> service.save(10L, 30L, progressRequest(true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(trainingStatus)
                .satisfies(
                        error ->
                                assertThat(((BusinessException) error).getResultCode())
                                        .isEqualTo(ResultCodeEnum.CONFLICT));

        verifyNoInteractions(itemMapper, progressMapper);
        verify(recordMapper, never()).updateById(any(TrainingRecordEntity.class));
    }

    @Test
    void failedProgressInsertIsReportedAsConflict() {
        authenticateParticipant();
        TrainingRecordEntity record = trainingRecord(TrainingStatus.LEARNING);
        when(recordMapper.selectById(10L)).thenReturn(record);
        when(itemMapper.selectActiveById(30L)).thenReturn(planItem());
        when(progressMapper.selectActive(10L, 30L)).thenReturn(null);
        when(progressMapper.insert(any(TrainingRecordItemEntity.class))).thenReturn(0);

        assertThatThrownBy(() -> service.save(10L, 30L, progressRequest(true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conflict")
                .satisfies(
                        error ->
                                assertThat(((BusinessException) error).getResultCode())
                                        .isEqualTo(ResultCodeEnum.CONFLICT));

        verify(progressMapper, never()).countRequiredItems(any());
        verify(recordMapper, never()).updateById(any(TrainingRecordEntity.class));
    }

    @Test
    void optimisticLockConflictUpdatingProgressIsReported() {
        authenticateParticipant();
        TrainingRecordEntity record = trainingRecord(TrainingStatus.MAKEUP);
        TrainingRecordItemEntity progress = new TrainingRecordItemEntity();
        progress.setId(40L);
        progress.setVersion(3);
        when(recordMapper.selectById(10L)).thenReturn(record);
        when(itemMapper.selectActiveById(30L)).thenReturn(planItem());
        when(progressMapper.selectActive(10L, 30L)).thenReturn(progress);
        when(progressMapper.updateById(progress)).thenReturn(0);

        assertThatThrownBy(() -> service.save(10L, 30L, progressRequest(false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conflict")
                .satisfies(
                        error ->
                                assertThat(((BusinessException) error).getResultCode())
                                        .isEqualTo(ResultCodeEnum.CONFLICT));

        verify(progressMapper, never()).countRequiredItems(any());
        verify(recordMapper, never()).updateById(any(TrainingRecordEntity.class));
    }

    private void authenticateParticipant() {
        CurrentUser user =
                new CurrentUser(
                        8L,
                        "student",
                        "Student",
                        null,
                        null,
                        Set.of("STUDENT"),
                        Set.of(),
                        Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    private TrainingRecordEntity trainingRecord(String trainingStatus) {
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(10L);
        record.setUserId(8L);
        record.setPlanId(20L);
        record.setProgress(BigDecimal.ZERO);
        record.setTrainingStatus(trainingStatus);
        return record;
    }

    private TrainingPlanItemEntity planItem() {
        TrainingPlanItemEntity item = new TrainingPlanItemEntity();
        item.setId(30L);
        item.setPlanId(20L);
        item.setIsDeleted(0);
        item.setStatus(1);
        return item;
    }

    private TrainingRecordItemProgressRequest progressRequest(boolean completed) {
        TrainingRecordItemProgressRequest request = new TrainingRecordItemProgressRequest();
        request.setProgress(completed ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
        request.setCompleted(completed);
        return request;
    }
}
