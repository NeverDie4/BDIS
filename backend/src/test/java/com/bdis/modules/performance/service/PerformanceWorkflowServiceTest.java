package com.bdis.modules.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.performance.dto.PerformanceRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.mapper.PerformanceAuditMapper;
import com.bdis.modules.performance.mapper.PerformanceMapper;
import com.bdis.modules.performance.mapper.PerformanceStandardMapper;
import com.bdis.modules.performance.service.impl.PerformanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerformanceWorkflowServiceTest {

    @Mock private PerformanceMapper performanceMapper;
    @Mock private PerformanceStandardMapper standardMapper;
    @Mock private PerformanceAuditMapper auditMapper;
    @Mock private PerformanceMaterialService materialService;
    @Mock private BusinessAccessService accessService;

    @Test
    void creationUsesAuthenticatedUserAndAlwaysStartsAsDraft() {
        when(accessService.currentUserId()).thenReturn(5L);
        when(performanceMapper.selectById(8L)).thenAnswer(invocation -> inserted);
        PerformanceServiceImpl service =
                new PerformanceServiceImpl(
                        performanceMapper,
                        standardMapper,
                        auditMapper,
                        materialService,
                        accessService);
        PerformanceRequest request = new PerformanceRequest();
        request.setUserId(999L);
        request.setPerformanceTitle("测试业绩");
        request.setIdentifyStatus("approved");

        doAnswer(
                        invocation -> {
                            inserted = invocation.getArgument(0);
                            inserted.setId(8L);
                            return 1;
                        })
                .when(performanceMapper)
                .insert(any(PerformanceEntity.class));
        PerformanceEntity result = service.createPerformance(request);

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getIdentifyStatus()).isEqualTo("draft");
    }

    private PerformanceEntity inserted;
}
