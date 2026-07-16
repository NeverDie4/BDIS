package com.bdis.modules.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceAuditRequest;
import com.bdis.modules.performance.entity.PerformanceAuditEntity;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceAuditMapper;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.service.impl.PerformanceAuditServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerformanceAuditServiceTest {

    @Mock private PerformanceMapper performanceMapper;
    @Mock private PerformanceAuditMapper auditMapper;
    @Mock private BusinessAccessService accessService;

    @Test
    void ownerCannotAuditOwnSubmittedPerformance() {
        PerformanceEntity performance = submittedPerformance(5L);
        when(performanceMapper.selectById(8L)).thenReturn(performance);
        when(accessService.currentUserId()).thenReturn(5L);
        PerformanceAuditRequest request = new PerformanceAuditRequest();
        request.setDecision("approved");

        assertThatThrownBy(
                        () ->
                                new PerformanceAuditServiceImpl(
                                                performanceMapper, auditMapper, accessService)
                                        .auditPerformance(8L, request))
                .hasMessage("不能审核本人业绩");
    }

    @Test
    void rejectionRequiresComment() {
        PerformanceEntity performance = submittedPerformance(5L);
        when(performanceMapper.selectById(8L)).thenReturn(performance);
        when(accessService.currentUserId()).thenReturn(6L);
        PerformanceAuditRequest request = new PerformanceAuditRequest();
        request.setDecision("rejected");

        assertThatThrownBy(
                        () ->
                                new PerformanceAuditServiceImpl(
                                                performanceMapper, auditMapper, accessService)
                                        .auditPerformance(8L, request))
                .hasMessage("要求修改时必须填写认定意见");
    }

    @Test
    void reviewerCanApproveAnotherUsersSubmittedPerformance() {
        PerformanceEntity performance = submittedPerformance(5L);
        when(performanceMapper.selectById(8L)).thenReturn(performance);
        when(accessService.currentUserId()).thenReturn(6L);
        when(performanceMapper.updateById(performance)).thenReturn(1);
        PerformanceAuditRequest request = new PerformanceAuditRequest();
        request.setDecision("approved");
        request.setComment("材料齐全，符合认定条件");

        new PerformanceAuditServiceImpl(performanceMapper, auditMapper, accessService)
                .auditPerformance(8L, request);

        assertThat(performance.getIdentifyStatus()).isEqualTo("approved");
        verify(auditMapper).insert(any(PerformanceAuditEntity.class));
    }

    private PerformanceEntity submittedPerformance(Long ownerId) {
        PerformanceEntity entity = new PerformanceEntity();
        entity.setId(8L);
        entity.setUserId(ownerId);
        entity.setIdentifyStatus("submitted");
        return entity;
    }
}
